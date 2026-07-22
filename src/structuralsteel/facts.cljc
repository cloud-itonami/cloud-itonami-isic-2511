(ns structuralsteel.facts
  "Per-jurisdiction structural-steel welding-procedure-qualification and
  fabricator-certification catalog -- the G2-style spec-basis table the
  Structural Fabrication Governor checks every `:welding-procedure/
  verify` proposal against.

  Coverage is reported HONESTLY: a jurisdiction not in this table has
  NO spec-basis. Seed values cite official structural-steel-fabricator
  certification / welding-procedure-qualification authorities; this is
  a starting catalog, not a survey of every fabricator-certification
  scheme.")

(def catalog
  {"JPN" {:name "Japan"
          :owner-authority "一般財団法人日本建築センター (BCJ) / 日本鉄骨評価センター (参考)"
          :legal-basis "建築基準法 / JASS 6 鉄骨工事標準仕様書 / 工場認定制度(M・H・Sグレード) (参考)"
          :national-spec "鉄骨製作工場グレード認定(M/H/S)・溶接施工要領書(WPS)・非破壊検査要件"
          :provenance "https://www.bcj.or.jp/"
          :required-evidence ["溶接施工要領書検証報告書 (welding-procedure-qualification-report)"
                              "非破壊検査報告書 (nde-inspection-report)"
                              "溶接品質連鎖記録 (weld-quality-chain-of-custody-record)"
                              "材料証明記録 (material-certification-record)"]}
   "USA" {:name "United States"
          :owner-authority "American Institute of Steel Construction (AISC) / American Welding Society (AWS)"
          :legal-basis "AISC 207 Certification Standard for Steel Building Structures / AWS D1.1/D1.1M Structural Welding Code - Steel (reference)"
          :national-spec "US structural-steel fabricator certification and structural welding qualification requirements"
          :provenance "https://www.aisc.org/"
          :required-evidence ["welding-procedure-qualification-report"
                              "nde-inspection-report"
                              "weld-quality-chain-of-custody-record"
                              "material-certification-record"]}
   "GBR" {:name "United Kingdom"
          :owner-authority "BSI / UKCA + CE (EN 1090-1 execution-class conformity, GB adoption)"
          :legal-basis "BS EN 1090-1:2009+A1:2011 / BS EN 1090-2:2018 Execution Class (EXC) requirements (reference)"
          :national-spec "UK structural-steel/aluminium fabrication factory-production-control and UKCA/CE marking requirements"
          :provenance "https://www.bsigroup.com/"
          :required-evidence ["welding-procedure-qualification-report"
                              "nde-inspection-report"
                              "weld-quality-chain-of-custody-record"
                              "material-certification-record"]}
   "DEU" {:name "Germany"
          :owner-authority "DIN / EU-notifizierte Stelle (EN 1090-1 CE-Kennzeichnung, Ausführungsklassen-System)"
          :legal-basis "DIN EN 1090-1 / DIN EN 1090-2 Ausführung von Stahltragwerken (Referenz)"
          :national-spec "EU-Konformitätsbewertung für tragende Stahlbauteile (CE-Kennzeichnung nach Ausführungsklasse)"
          :provenance "https://www.din.de/"
          :required-evidence ["Schweißverfahrensprüfbericht (welding-procedure-qualification-report)"
                              "Prüfbericht zerstörungsfreie Prüfung (nde-inspection-report)"
                              "Schweißqualitäts-Rückverfolgbarkeitsnachweis (weld-quality-chain-of-custody-record)"
                              "Werkstoffzertifikat (material-certification-record)"]}
   ;; POL added 2026-07-23. Legal-basis layers, each independently fetched and
   ;; read this session (English glosses below are ours, not the primary text):
   ;;  - Rozporządzenie (UE) Nr 305/2011 (CPR) -- EU-wide harmonized-conditions
   ;;    baseline for placing construction products on the market (EUR-Lex).
   ;;  - Ustawa z dnia 16 kwietnia 2004 r. o wyrobach budowlanych (Dz.U. 2004
   ;;    nr 92 poz. 881), amended by ustawa z dnia 13 czerwca 2013 r. (Dz.U.
   ;;    2013 poz. 898) specifically "w celu dostosowania do rozporządzenia
   ;;    Nr 305/2011" -- Poland's national implementing act: names Główny
   ;;    Inspektor Nadzoru Budowlanego (GUNB) as the competent authority for
   ;;    construction-product matters (art. 3) and gives CPR 305/2011 domestic
   ;;    legal effect/enforcement machinery (art. 1). Distinct from the EU
   ;;    regulation alone, per dziennikustaw.gov.pl PDFs read directly.
   ;;  - Prawo budowlane art. 10 (consolidated text Dz.U. 2026 poz. 524, in
   ;;    force as of legalStatusDate 2026-03-19, fetched via api.sejm.gov.pl):
   ;;    conditions lawful use of a product in construction works on its
   ;;    having been placed on the market "zgodnie z przepisami odrębnymi"
   ;;    (i.e. per the wyroby budowlane act / CPR pathway above) -- the
   ;;    building-law bridge distinct from the product-marketing rules.
   ;; Gaps honestly left unstated rather than guessed: (1) PN-EN 1090-2's
   ;; specific edition year -- PKN's wiedza.pkn.pl norm search is a JS-only
   ;; form that returns no data over a plain fetch, so only the PN-EN 1090-1
   ;; edition ("+A1:2012") was independently confirmed, via the certification
   ;; body source below, not PKN's own catalog. (2) A specific EU notified-body
   ;; registration number for FPC/ZKP certification -- not found in any source
   ;; actually read. :owner-authority's named certification body is a verified
   ;; example, not asserted to be exclusive. Separately: is.gliwice.pl (that
   ;; body's live site) has an expired TLS certificate blocking a normal
   ;; fetch (not bot-detection); its content was instead read via the Wayback
   ;; Machine capture https://web.archive.org/web/20250208232848/https://is.gliwice.pl/
   ;; per the fallback-source rule, and is cited as such below.
   "POL" {:name "Poland"
          :owner-authority "PKN (Polski Komitet Normalizacyjny) / Główny Inspektor Nadzoru Budowlanego (GUNB) / jednostka notyfikowana UE ds. ZKP wg PN-EN 1090-1 (zweryfikowany przykład: Łukasiewicz – Centrum Spawalnictwa, dawniej Instytut Spawalnictwa, Gliwice)"
          :legal-basis "Rozporządzenie (UE) Nr 305/2011 (CPR) + PN-EN 1090-1+A1:2012 / PN-EN 1090-2 (wykonanie konstrukcji stalowych, referencja) + ustawa z dnia 16 kwietnia 2004 r. o wyrobach budowlanych (Dz.U. 2004 nr 92 poz. 881, dostosowana do rozporządzenia 305/2011 ustawą z dnia 13 czerwca 2013 r., Dz.U. 2013 poz. 898) + Prawo budowlane art. 10 (tekst jedn. Dz.U. 2026 poz. 524)"
          :national-spec "Ocena zgodności zakładowej kontroli produkcji (ZKP) dla konstrukcji stalowych/aluminiowych wg PN-EN 1090-1, system 2+, klasy wykonania EXC1-EXC4; oznakowanie CE wg rozporządzenia 305/2011; dopuszczenie wyrobu do robót budowlanych wg art. 10 Prawa budowlanego"
          :provenance "https://www.pkn.pl/"
          :required-evidence ["protokół kwalifikowania technologii spawania WPQR (welding-procedure-qualification-report)"
                              "sprawozdanie z badań nieniszczących (nde-inspection-report)"
                              "zapis identyfikowalności jakości spawania (weld-quality-chain-of-custody-record)"
                              "atest / świadectwo odbioru materiału (material-certification-record)"]}})

(defn spec-basis [iso3] (get catalog iso3))

(defn coverage
  ([] (coverage (keys catalog)))
  ([iso3s]
   (let [have (filter catalog iso3s)
         missing (remove catalog iso3s)]
     {:requested (count iso3s)
      :covered (count have)
      :covered-jurisdictions (vec (sort have))
      :missing-jurisdictions (vec (sort missing))
      :note (str "cloud-itonami-isic-2511 R0: " (count catalog)
                 " jurisdictions seeded. Extend `structuralsteel.facts/catalog`, "
                 "never fabricate a jurisdiction's requirements.")})))

(defn required-evidence-satisfied?
  [iso3 submitted]
  (when-let [{:keys [required-evidence]} (spec-basis iso3)]
    (let [need (count required-evidence)
          have (count (filter (set submitted) required-evidence))]
      (= need have))))

(defn evidence-checklist [iso3]
  (:required-evidence (spec-basis iso3) []))
