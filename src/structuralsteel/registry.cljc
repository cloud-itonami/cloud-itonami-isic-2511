(ns structuralsteel.registry
  "Pure-function assembly-dispatch + fabrication-certificate record
  construction -- an append-only structural-steel-fabricator book-of-
  record draft.

  Like every sibling actor's registry, there is no single
  international check-digit standard for an assembly-dispatch or
  fabrication-certificate reference number -- every fabricator/
  jurisdiction assigns its own reference format. This namespace does
  NOT invent one; it builds a jurisdiction-scoped sequence number and
  validates the record's required fields, the same honest, non-
  fabricating discipline `structuralsteel.facts` uses.

  `assembly-camber-out-of-range?` is another instance of this fleet's
  two-sided range check family (`testlab.registry/within-tolerance?`
  established the first, `conservation.registry/body-condition-out-
  of-range?` the second, `water.registry/contaminant-level-out-of-
  range?` the third, `steelworks.registry/heat-chemistry-out-of-
  range?`/`turbine.registry/unit-tolerance-out-of-range?`/
  `automotive.registry/vehicle-emissions-out-of-range?` further
  siblings), applying the SAME lo/hi bounds-comparison shape to an
  assembly's own measured camber deviation against the assembly's own
  recorded fabrication-tolerance spec bounds.

  This namespace is pure data + pure functions -- no I/O, no network
  call to any real fab-shop control system. It builds the RECORD a
  fabricator would keep, not the act of dispatching the robot
  assembly action or issuing the fabrication certificate itself (that
  is `structuralsteel.operation`'s `:actuation/dispatch-assembly`/
  `:actuation/issue-fabrication-certificate`, always human-gated --
  see README `Actuation`)."
  (:require [clojure.string :as str]))

(defn- unsigned-certificate
  "Every certificate this actor produces is UNSIGNED -- signature is the
  fabricator's own act, not this actor's. See README `Actuation`."
  [kind subject record-id]
  {"@context" ["https://www.w3.org/ns/credentials/v2"]
   "type" ["VerifiableCredential" kind]
   "credentialSubject" {"id" subject "record" record-id}
   "proof" nil
   "issued_by_registry" false
   "status" "draft-unsigned"})

(defn- zero-pad [n w]
  (let [s (str n)]
    (str (apply str (repeat (max 0 (- w (count s))) "0")) s)))

(defn assembly-camber-out-of-range?
  "Does `assembly`'s own `:camber-deviation-actual` fall outside its
  own `[:camber-deviation-min :camber-deviation-max]` recorded
  fabrication-tolerance spec-bounds? A pure ground-truth check
  against the assembly's own permanent fields -- no upstream
  comparison needed. One of this fleet's two-sided range check
  family (see ns docstring)."
  [{:keys [camber-deviation-actual camber-deviation-min camber-deviation-max]}]
  (and (number? camber-deviation-actual) (number? camber-deviation-min) (number? camber-deviation-max)
       (or (< camber-deviation-actual camber-deviation-min)
           (> camber-deviation-actual camber-deviation-max))))

(defn register-assembly-dispatch
  "Validate + construct the ASSEMBLY-DISPATCH registration DRAFT --
  the fabricator's own act of dispatching a real robot welding/fit-up
  action to complete a structural-steel assembly. Pure function --
  does not touch any real fab-shop control system; it builds the
  RECORD a fabricator would keep. `structuralsteel.governor`
  independently re-verifies the assembly's own camber-deviation
  sufficiency against its own spec bounds, and a double-dispatch for
  the same assembly, before this is ever allowed to commit."
  [assembly-id jurisdiction sequence]
  (when-not (and assembly-id (not= assembly-id ""))
    (throw (ex-info "assembly-dispatch: assembly_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "assembly-dispatch: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "assembly-dispatch: sequence must be >= 0" {})))
  (let [dispatch-number (str (str/upper-case jurisdiction) "-ASM-" (zero-pad sequence 6))
        record {"record_id" dispatch-number
                "kind" "assembly-dispatch-draft"
                "assembly_id" assembly-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "dispatch_number" dispatch-number
     "certificate" (unsigned-certificate "AssemblyDispatch" dispatch-number dispatch-number)}))

(defn register-fabrication-certificate
  "Validate + construct the FABRICATION-CERTIFICATE registration
  DRAFT -- the fabricator's own act of issuing a real fabrication
  certificate certifying an assembly as release-worthy. Pure
  function -- does not touch any real fab-shop control system; it
  builds the RECORD a fabricator would keep. `structuralsteel.
  governor` independently re-verifies the assembly's own NDE defect
  resolution status, and a double-issuance for the same assembly,
  before this is ever allowed to commit."
  [assembly-id jurisdiction sequence]
  (when-not (and assembly-id (not= assembly-id ""))
    (throw (ex-info "fabrication-certificate: assembly_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "fabrication-certificate: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "fabrication-certificate: sequence must be >= 0" {})))
  (let [evidence-number (str (str/upper-case jurisdiction) "-FAB-" (zero-pad sequence 6))
        record {"record_id" evidence-number
                "kind" "fabrication-certificate-draft"
                "assembly_id" assembly-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "evidence_number" evidence-number
     "certificate" (unsigned-certificate "FabricationCertificate" evidence-number evidence-number)}))

(defn append [history result]
  (conj (vec history) (get result "record")))
