(ns structuralsteel.governor
  "Structural Fabrication Governor -- the independent compliance layer
  that earns the Structural Fabrication Advisor the right to commit.
  The LLM has no notion of welding-procedure-qualification law,
  whether an assembly's own measured camber deviation actually stays
  within its own recorded spec bounds, whether an NDE-detected defect
  against the assembly has actually stayed unresolved, or when an act
  stops being a draft and becomes a real-world robot assembly dispatch
  or fabrication-certificate issuance, so this MUST be a separate
  system able to *reject* a proposal and fall back to HOLD -- the
  structural-steel-fabricator analog of `cloud-itonami-isic-6512`'s
  CasualtyGovernor.

  Six checks, in priority order, ALL HARD violations: a human approver
  CANNOT override them (you don't get to approve your way past a
  fabricated welding-procedure spec-basis, incomplete evidence, an
  out-of-spec assembly, an unresolved NDE defect, or a double dispatch/
  certificate-issuance). The confidence/actuation gate is SOFT: it asks
  a human to look (low confidence / actuation), and the human may
  approve -- but see `structuralsteel.phase`: for `:stake :actuation/
  dispatch-assembly`/`:actuation/issue-fabrication-certificate` (a
  real safety-critical act) NO phase ever allows auto-commit either.
  Two independent layers agree that actuation is always a human call.

    1. Spec-basis                  -- did the requirements proposal cite
                                       an OFFICIAL source (`structural
                                       steel.facts`), or invent one?
    2. Evidence incomplete         -- for `:actuation/dispatch-
                                       assembly`/`:actuation/issue-
                                       fabrication-certificate`, has the
                                       assembly actually been verified
                                       with a full welding-procedure-
                                       qualification-report/nde-
                                       inspection-report/weld-quality-
                                       chain-of-custody-record/material-
                                       certification-record evidence
                                       checklist on file?
    3. Assembly camber out of
       range                         -- for `:actuation/dispatch-
                                       assembly`, INDEPENDENTLY
                                       recompute whether the
                                       assembly's own measured camber
                                       deviation falls outside its own
                                       recorded spec bounds
                                       (`structuralsteel.registry/
                                       assembly-camber-out-of-range?`)
                                       -- needs no proposal inspection
                                       or stored-verdict lookup at
                                       all. One of this fleet's two-
                                       sided range check family
                                       (`testlab.governor/within-
                                       tolerance-violations`/
                                       `conservation.governor/body-
                                       condition-out-of-range-
                                       violations`/`water.governor/
                                       contaminant-level-out-of-range-
                                       violations`/`steelworks.
                                       governor`/`turbine.governor`/
                                       `automotive.governor` established
                                       the priors).
    4. NDE defect unresolved       -- reported by THIS proposal itself
                                       (an `:nde-inspection/screen`
                                       that just found an unresolved
                                       defect), or already on file for
                                       the assembly (`:nde-inspection/
                                       screen`/`:actuation/issue-
                                       fabrication-certificate`).
                                       Evaluated UNCONDITIONALLY (not
                                       scoped to a specific op), the
                                       SAME discipline `casualty.
                                       governor/sanctions-violations`/
                                       ...(prior siblings)...
                                       established -- exercised in
                                       tests/demo via `:nde-inspection/
                                       screen` DIRECTLY, not via an
                                       actuation op against an
                                       unscreened assembly -- see this
                                       ns's own test suite.
    5. Confidence floor / actuation
       gate                          -- LLM confidence below threshold,
                                       OR the op is `:actuation/
                                       dispatch-assembly`/`:actuation/
                                       issue-fabrication-certificate`
                                       (REAL safety-critical acts) ->
                                       escalate.

  Two more guards, double-dispatch/double-certificate-issuance
  prevention, are enforced but NOT listed as numbered HARD checks
  above because they need no upstream comparison at all --
  `already-dispatched-violations`/`already-certified-violations`
  refuse to dispatch an assembly action/issue a fabrication
  certificate for the SAME assembly twice, off dedicated
  `:assembly-dispatched?`/`:fabrication-certified?` facts (never a
  `:status` value) -- the SAME 'check a dedicated boolean, not
  status' discipline every prior sibling governor's guards
  establish, informed by `cloud-itonami-isic-6492`'s status-
  lifecycle bug (ADR-2607071320)."
  (:require [structuralsteel.facts :as facts]
            [structuralsteel.registry :as registry]
            [structuralsteel.store :as store]))

(def confidence-floor 0.6)

(def high-stakes
  "Stakes grave enough to always require a human, even when clean.
  Dispatching a real robot assembly action on a safety-critical
  assembly and issuing a real fabrication certificate are the two
  real-world actuation events this actor performs -- a two-member
  set, matching every prior dual-actuation sibling's shape."
  #{:actuation/dispatch-assembly :actuation/issue-fabrication-certificate})

;; ----------------------------- checks -----------------------------

(defn- spec-basis-violations
  "A `:welding-procedure/verify` (or actuation) proposal with no
  spec-basis citation is a HARD violation -- never invent a
  jurisdiction's welding-procedure-qualification requirements."
  [{:keys [op]} proposal]
  (when (contains? #{:welding-procedure/verify :actuation/dispatch-assembly :actuation/issue-fabrication-certificate} op)
    (let [value (:value proposal)]
      (when (or (empty? (:cites proposal))
                (and (contains? value :spec-basis) (nil? (:spec-basis value))))
        [{:rule :no-spec-basis
          :detail "公式spec-basisの引用が無い提案は溶接施工要領要件として扱えない"}]))))

(defn- evidence-incomplete-violations
  "For `:actuation/dispatch-assembly`/`:actuation/issue-fabrication-
  certificate`, the jurisdiction's required welding-procedure-
  qualification-report/nde-inspection-report/weld-quality-chain-of-
  custody-record/material-certification-record evidence must actually
  be satisfied -- do not trust the advisor's self-reported confidence
  alone."
  [{:keys [op subject]} st]
  (when (contains? #{:actuation/dispatch-assembly :actuation/issue-fabrication-certificate} op)
    (let [a (store/assembly st subject)
          verification (store/requirements-verification-of st subject)]
      (when-not (and verification
                     (facts/required-evidence-satisfied?
                      (:jurisdiction a) (:checklist verification)))
        [{:rule :evidence-incomplete
          :detail "法域の必要書類(溶接施工要領書検証報告書/非破壊検査報告書/溶接品質連鎖記録/材料証明記録等)が充足していない状態での提案"}]))))

(defn- assembly-camber-out-of-range-violations
  "For `:actuation/dispatch-assembly`, INDEPENDENTLY recompute whether
  the assembly's own camber deviation falls outside its own recorded
  spec bounds via `structuralsteel.registry/assembly-camber-out-of-
  range?` -- needs no proposal inspection or stored-verdict lookup at
  all, since its inputs are permanent ground-truth fields already on
  the assembly."
  [{:keys [op subject]} st]
  (when (= op :actuation/dispatch-assembly)
    (let [a (store/assembly st subject)]
      (when (registry/assembly-camber-out-of-range? a)
        [{:rule :assembly-camber-out-of-range
          :detail (str subject " の実測キャンバー偏差(" (:camber-deviation-actual a)
                      ")が仕様範囲[" (:camber-deviation-min a) "," (:camber-deviation-max a) "]を逸脱")}]))))

(defn- nde-defect-unresolved-violations
  "An unresolved NDE-detected weld defect -- reported by THIS
  proposal (e.g. an `:nde-inspection/screen` that itself just found
  one), or already on file in the store for the assembly
  (`:nde-inspection/screen`/`:actuation/issue-fabrication-
  certificate`) -- is a HARD, un-overridable hold. Evaluated
  UNCONDITIONALLY (not scoped to a specific op) so the screening op
  itself can HARD-hold on its own finding."
  [{:keys [op subject]} proposal st]
  (let [hit-in-proposal? (= :unresolved (get-in proposal [:value :verdict]))
        assembly-id (when (contains? #{:nde-inspection/screen :actuation/issue-fabrication-certificate} op) subject)
        hit-on-file? (and assembly-id (= :unresolved (:verdict (store/nde-screen-of st assembly-id))))]
    (when (or hit-in-proposal? hit-on-file?)
      [{:rule :nde-defect-unresolved
        :detail "未解決の非破壊検査欠陥がある状態での製作証明書発行提案は進められない"}])))

(defn- already-dispatched-violations
  "For `:actuation/dispatch-assembly`, refuses to dispatch an
  assembly action for the SAME assembly twice, off a dedicated
  `:assembly-dispatched?` fact (never a `:status` value)."
  [{:keys [op subject]} st]
  (when (= op :actuation/dispatch-assembly)
    (when (store/assembly-already-dispatched? st subject)
      [{:rule :already-dispatched
        :detail (str subject " は既に組立実行済み")}])))

(defn- already-certified-violations
  "For `:actuation/issue-fabrication-certificate`, refuses to issue a
  fabrication certificate for the SAME assembly twice, off a
  dedicated `:fabrication-certified?` fact (never a `:status` value)."
  [{:keys [op subject]} st]
  (when (= op :actuation/issue-fabrication-certificate)
    (when (store/assembly-already-certified? st subject)
      [{:rule :already-certified
        :detail (str subject " は既に製作証明書発行済み")}])))

(defn check
  "Censors a Structural Fabrication Advisor proposal against the
  governor rules. Returns {:ok? bool :violations [..] :confidence c
  :escalate? bool :high-stakes? bool :hard? bool}."
  [request _context proposal st]
  (let [hard (into []
                   (concat (spec-basis-violations request proposal)
                           (evidence-incomplete-violations request st)
                           (assembly-camber-out-of-range-violations request st)
                           (nde-defect-unresolved-violations request proposal st)
                           (already-dispatched-violations request st)
                           (already-certified-violations request st)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        stakes? (boolean (high-stakes (:stake proposal)))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not stakes?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? stakes?))
     :high-stakes? stakes?}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
