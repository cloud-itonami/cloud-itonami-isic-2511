(ns structuralsteel.render-html
  "Build-time HTML renderer for `docs/samples/operator-console.html`.

  Closes flagship checklist item 2 (com-junkawasaki/root ADR-2607189300):
  this repo previously shipped a hand-typed static placeholder at
  `docs/samples/operator-console.html` with no generator at all. This
  namespace drives the REAL actor stack (`structuralsteel.operation` ->
  `structuralsteel.governor` -> `structuralsteel.store`) through a
  scenario adapted from this repo's own `structuralsteel.sim` demo
  driver (`clojure -M:dev:run` -- request ids \"assembly-1\"..
  \"assembly-4\" DO match `structuralsteel.store/demo-data`, and every
  claimed HARD-hold rule in its printed banner was independently
  cross-checked against the actual `:violations` returned by a real
  run, so it was safe to adapt from rather than author from scratch),
  covering a genuine mix of dispositions this actor can reach, and
  rendered deterministically -- no invented numbers, no timestamps in
  the page content, byte-identical across reruns against the same seed
  (verify by diffing two consecutive runs before shipping).

  Usage: `clojure -M:dev:render-html [out-file]`
  (default `docs/samples/operator-console.html`)."
  (:require [jp-go-dds.skin]
            [clojure.string :as str]
            [structuralsteel.store :as store]
            [structuralsteel.operation :as op]
            [langgraph.graph :as g]))

;; ----------------------------- harness (unchanged across every repo
;; in this cluster -- do not rewrite, only copy) -----------------------

(def ^:private operator
  {:actor-id "op-1" :actor-role :certified-welding-inspector :phase 3})

(defn- exec! [actor tid request]
  (g/run* actor {:request request :context operator} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}}
          {:thread-id tid :resume? true}))

(defn run-demo!
  "Runs a fresh seeded store through a scenario mixing every disposition
  this actor can reach:

  Clean / approved paths (assembly-1, full lifecycle):
    - assembly-1 `:assembly/intake` phase-3 auto-commit (only member of
      phase 3's `:auto` set; no physical/financial risk).
    - assembly-1 `:welding-procedure/verify` ALWAYS escalates (never in
      any phase's `:auto`) -> human certified welding inspector
      approves -> commits.
    - assembly-1 `:nde-inspection/screen` (clean, no unresolved defect)
      escalates -> human approves -> commits.
    - assembly-1 `:actuation/dispatch-assembly` ALWAYS escalates
      (high-stakes, permanently absent from every phase's `:auto`) ->
      human approves -> commits (draft assembly-dispatch record).
    - assembly-1 `:actuation/issue-fabrication-certificate` ALWAYS
      escalates -> human approves -> commits (draft fabrication-
      certificate record).

  HARD-hold paths (never reach a human, no override possible):
    - assembly-2 `:welding-procedure/verify` with `:no-spec? true`
      (jurisdiction ATL has no entry in `structuralsteel.facts`) ->
      `:no-spec-basis`.
    - assembly-3 `:welding-procedure/verify` escalates then approved
      (sets up the out-of-spec dispatch test; verify itself is clean).
    - assembly-3 `:actuation/dispatch-assembly` (camber 0.35 outside
      [-0.10,0.10], independently recomputed from the assembly's own
      permanent fields) -> `:assembly-camber-out-of-range`.
    - assembly-4 `:nde-inspection/screen` (seeded
      `:nde-defect-unresolved? true`) -> `:nde-defect-unresolved`.
    - assembly-1 `:actuation/dispatch-assembly` AGAIN ->
      `:already-dispatched`.
    - assembly-1 `:actuation/issue-fabrication-certificate` AGAIN ->
      `:already-certified`.

  Returns the resulting store -- every field `render` below reads is
  real governor/store output, not a hand-typed copy."
  []
  (let [db (store/seed-db)
        actor (op/build db)]

    (exec! actor "t1-assembly-1-intake"
           {:op :assembly/intake :subject "assembly-1"
            :patch {:id "assembly-1"
                    :assembly-name "Sakura Moment-Frame Column Assembly MC-04"}})

    (exec! actor "t2-assembly-1-verify"
           {:op :welding-procedure/verify :subject "assembly-1"})
    (approve! actor "t2-assembly-1-verify")

    (exec! actor "t3-assembly-1-nde"
           {:op :nde-inspection/screen :subject "assembly-1"})
    (approve! actor "t3-assembly-1-nde")

    (exec! actor "t4-assembly-1-dispatch"
           {:op :actuation/dispatch-assembly :subject "assembly-1"})
    (approve! actor "t4-assembly-1-dispatch")

    (exec! actor "t5-assembly-1-certificate"
           {:op :actuation/issue-fabrication-certificate :subject "assembly-1"})
    (approve! actor "t5-assembly-1-certificate")

    (exec! actor "t6-assembly-2-no-spec"
           {:op :welding-procedure/verify :subject "assembly-2" :no-spec? true})

    (exec! actor "t7-assembly-3-verify"
           {:op :welding-procedure/verify :subject "assembly-3"})
    (approve! actor "t7-assembly-3-verify")

    (exec! actor "t8-assembly-3-camber"
           {:op :actuation/dispatch-assembly :subject "assembly-3"})

    (exec! actor "t9-assembly-4-nde"
           {:op :nde-inspection/screen :subject "assembly-4"})

    (exec! actor "t10-assembly-1-double-dispatch"
           {:op :actuation/dispatch-assembly :subject "assembly-1"})

    (exec! actor "t11-assembly-1-double-certificate"
           {:op :actuation/issue-fabrication-certificate :subject "assembly-1"})

    db))

;; ----------------------------- rendering -----------------------------

(defn- esc [v]
  (-> (str v)
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")))

(defn- kw
  "Keyword -> string keeping the namespace (name alone drops it)."
  [v]
  (cond
    (keyword? v) (if-let [n (namespace v)]
                   (str n "/" (name v))
                   (name v))
    (nil? v) "n-a"
    :else (str v)))

(defn- last-fact-for [ledger subject-id]
  (last (filter #(= (:subject %) subject-id) ledger)))

(defn- status-cell [ledger subject-id]
  (let [f (last-fact-for ledger subject-id)]
    (cond
      (nil? f) "<span class=\"muted\">no activity</span>"
      (= :committed (:t f)) "<span class=\"ok\">committed</span>"
      (= :approval-granted (:t f)) "<span class=\"ok\">approved &amp; committed</span>"
      (= :governor-hold (:t f))
      (let [rule (or (-> f :violations first :rule)
                     (first (:basis f)))]
        (str "<span class=\"critical\">HARD hold &middot; " (esc (kw (or rule :unknown))) "</span>"))
      (= :approval-requested (:t f)) "<span class=\"warn\">awaiting approval</span>"
      :else "<span class=\"muted\">in progress</span>")))

(defn- camber-cell [{:keys [camber-deviation-actual camber-deviation-min camber-deviation-max]}]
  (let [in-range? (and (number? camber-deviation-actual)
                       (number? camber-deviation-min)
                       (number? camber-deviation-max)
                       (<= camber-deviation-min camber-deviation-actual camber-deviation-max))]
    (if in-range?
      (str "<span class=\"ok\">" (esc camber-deviation-actual)
           " ∈ [" (esc camber-deviation-min) "," (esc camber-deviation-max) "]</span>")
      (str "<span class=\"err\">" (esc camber-deviation-actual)
           " out of [" (esc camber-deviation-min) "," (esc camber-deviation-max) "]</span>"))))

(defn- nde-cell [db {:keys [id nde-defect-unresolved?]}]
  ;; Advisor writes :verdict :resolved (clean) or :unresolved -- see
  ;; structuralsteel.structuralsteeladvisor/screen-nde-defect.
  (let [screen (store/nde-screen-of db id)
        v (:verdict screen)]
    (cond
      (= :unresolved v) "<span class=\"err\">unresolved</span>"
      (= :resolved v)   "<span class=\"ok\">resolved (screened)</span>"
      (= :clean v)      "<span class=\"ok\">clean (screened)</span>"
      nde-defect-unresolved? "<span class=\"err\">unresolved (seeded)</span>"
      :else "<span class=\"muted\">not screened</span>")))

(defn- draft-numbers [{:keys [dispatch-number evidence-number]}]
  (let [parts (cond-> []
                dispatch-number (conj (str dispatch-number))
                evidence-number (conj (str evidence-number)))]
    (if (seq parts) (str/join " · " parts) "—")))

(defn- assembly-row [db ledger {:keys [id assembly-name jurisdiction
                                        assembly-dispatched? fabrication-certified?] :as a}]
  (format "        <tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>"
          (esc id)
          (esc assembly-name)
          (esc jurisdiction)
          (camber-cell a)
          (nde-cell db a)
          (if assembly-dispatched? "<span class=\"ok\">yes</span>" "<span class=\"muted\">no</span>")
          (if fabrication-certified? "<span class=\"ok\">yes</span>" "<span class=\"muted\">no</span>")
          (esc (draft-numbers a))
          (status-cell ledger id)))

(defn- ledger-row [{:keys [t op subject disposition basis]}]
  (format "        <tr><td>%s</td><td><code>%s</code></td><td>%s</td><td>%s</td></tr>"
          (esc (kw t))
          (esc (kw (or op :n-a)))
          (esc subject)
          (esc (or (some->> basis (map kw) (str/join ", "))
                   (some-> disposition kw)
                   ""))))

(defn- history-row [kind rec]
  (let [num (or (get rec "record_id") (get rec "dispatch_number") (get rec "evidence_number")
                (get rec :record_id) (get rec :dispatch_number) (get rec :evidence_number) "—")
        assembly (or (get rec "assembly_id") (get rec :assembly_id) "—")
        jur (or (get rec "jurisdiction") (get rec :jurisdiction) "—")]
    (format "        <tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>"
            (esc kind) (esc num) (esc assembly) (esc jur))))
(def ^:private action-gate-rows
  ;; Static description of this actor's own op contract
  ;; (README / structuralsteel.governor / structuralsteel.phase) --
  ;; documentation of fixed behavior, not runtime telemetry.
  ["        <tr><td><code>:assembly/intake</code></td><td><span class=\"ok\">auto-commit when clean, phase 3</span> &middot; only member of phase 3 <code>:auto</code></td></tr>"
   "        <tr><td><code>:welding-procedure/verify</code></td><td><span class=\"warn\">ALWAYS human approval</span> &middot; <span class=\"critical\">HARD hold</span> on <code>:no-spec-basis</code> (no fabricated jurisdiction rules)</td></tr>"
   "        <tr><td><code>:nde-inspection/screen</code></td><td><span class=\"warn\">human approval when clean</span> &middot; <span class=\"critical\">HARD hold</span> on <code>:nde-defect-unresolved</code></td></tr>"
   "        <tr><td><code>:actuation/dispatch-assembly</code></td><td><span class=\"warn\">ALWAYS human approval</span> (safety-critical) &middot; <span class=\"critical\">HARD</span> camber-out-of-range / evidence-incomplete / already-dispatched</td></tr>"
   "        <tr><td><code>:actuation/issue-fabrication-certificate</code></td><td><span class=\"warn\">ALWAYS human approval</span> (safety-critical) &middot; <span class=\"critical\">HARD</span> NDE unresolved / evidence-incomplete / already-certified</td></tr>"])

(defn render
  "Renders the full operator-console.html document from a store `db`
  that has already run `run-demo!` (or any other real scenario)."
  [db]
  (let [ledger (vec (store/ledger db))
        assemblies (store/all-assemblies db)
        dispatches (store/dispatch-history db)
        evidences (store/evidence-history db)
        assembly-rows (str/join "\n" (map (partial assembly-row db ledger) assemblies))
        ledger-rows (str/join "\n" (map ledger-row ledger))
        dispatch-rows (str/join "\n" (map (partial history-row "assembly-dispatch-draft") dispatches))
        evidence-rows (str/join "\n" (map (partial history-row "fabrication-certificate-draft") evidences))]
    (str
     "<html><head><meta charset=\"utf-8\"><title>cloud-itonami-isic-2511 &middot; structural-steel fab shop</title><style>"
     (jp-go-dds.skin/dds+skin)
     "</style></head><body>\n"
     "<header class=\"bar\">\n"
     "  <h1>Structural-steel fab shop (ISIC 2511) — Operator Console</h1>\n"
     "  <span class=\"badge\">read-only sample · governor-gated · robot dispatch + certificate always human-approved</span>\n"
     "</header>\n"
     "<main>\n"
     "  <section class=\"card\">\n"
     "    <h2>Assemblies</h2>\n"
     "    <p class=\"muted\">Demo snapshot — build-time-generated from <code>structuralsteel.store</code> via <code>structuralsteel.render-html</code> (<code>clojure -M:dev:render-html</code>), regenerated from the REAL actor stack.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Assembly</th><th>Name</th><th>Jurisdiction</th><th>Camber</th><th>NDE</th><th>Dispatched?</th><th>Certified?</th><th>Draft #</th><th>Last op status</th></tr></thead>\n"
     "      <tbody>\n"
     assembly-rows "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "  <section class=\"card\">\n"
     "    <h2>Action gate (Structural Fabrication Governor)</h2>\n"
     "    <p class=\"muted\">HARD holds cannot be overridden. <code>:actuation/dispatch-assembly</code> and <code>:actuation/issue-fabrication-certificate</code> are permanently absent from every phase's <code>:auto</code> set — a human certified welding inspector must always approve.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Op</th><th>Gate</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" action-gate-rows) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "  <section class=\"card\">\n"
     "    <h2>Draft assembly-dispatch / fabrication-certificate records</h2>\n"
     "    <p class=\"muted\">Unsigned drafts only — offline signing and certification-body submission are the fabricator's own acts (never this actor).</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Kind</th><th>Number</th><th>Assembly</th><th>Jurisdiction</th></tr></thead>\n"
     "      <tbody>\n"
     (if (and (empty? dispatches) (empty? evidences))
       "        <tr><td colspan=\"4\" class=\"muted\">no drafts in this run</td></tr>"
       (str dispatch-rows
            (when (and (seq dispatches) (seq evidences)) "\n")
            evidence-rows))
     "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "  <section class=\"card\">\n"
     "    <h2>Audit ledger (this run)</h2>\n"
     "    <p class=\"muted\">Append-only decision-fact log — every proposal, hold and commit this scenario produced.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Fact</th><th>Op</th><th>Subject</th><th>Basis</th></tr></thead>\n"
     "      <tbody>\n"
     ledger-rows "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "</main>\n"
     "</body></html>\n")))

(defn -main [& args]
  (let [out (or (first args) "docs/samples/operator-console.html")
        db (run-demo!)
        html (render db)]
    (spit out html)
    (println "wrote" out "(" (count (store/ledger db)) "ledger facts,"
             (count (store/all-assemblies db)) "assemblies,"
             (count (store/dispatch-history db)) "dispatches,"
             (count (store/evidence-history db)) "certificates )")))
