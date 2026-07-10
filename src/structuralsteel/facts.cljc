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
                              "Werkstoffzertifikat (material-certification-record)"]}})

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
