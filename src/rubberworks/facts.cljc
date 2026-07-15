(ns rubberworks.facts
  "Rubber-products material-spec evidence catalog -- the G2-style
  spec-basis table the Compression-Seal Governor checks every
  `:material-spec-rules/verify` proposal against.

  Like `moldworks.facts`'s injection-molded-plastics material-spec
  catalog and `opticsworks.facts`'s optical-instrument catalog (the SAME
  honest structural observation), other-rubber-products conformance does
  not decompose into one scheme per ISO3 country: it is a mix of named
  engineering-standards-body specifications (ASTM/ISO/IEC) organized by
  PRODUCT CLASS (what the molded/extruded rubber part becomes) rather
  than by the plant's own country. This catalog's keys reflect that real
  structure honestly rather than forcing a false per-country shape:

    - \"AUTOMOTIVE-RUBBER\" -- automotive rubber components
      (weatherstripping/door seals, hoses, engine/body mounts, grommets).
      ASTM D2000 (Standard Classification System for Rubber Products in
      Automotive Applications) is the real, widely-cited SAE/ASTM
      line-callout grade-classification system for automotive rubber
      compounds; ISO 1817 (Rubber, vulcanized -- Determination of the
      effect of liquids) is the real, standard test method for a rubber
      compound's resistance to automotive fluids (fuel, oil, coolant,
      brake fluid) that weatherstripping/hoses/mounts are routinely
      exposed to.
    - \"ELECTRONICS-GASKET\" -- electronics/consumer molded-rubber
      protective cases and internal gaskets/seals (smartphone
      waterproofing gaskets around ports/buttons, protective-case
      overmolds). IEC 60529 (Degrees of protection provided by
      enclosures, IP Code) is the real, standard ingress-protection
      rating framework directly relevant to smartphone/device
      waterproofing gaskets; ASTM D395 (Standard Test Methods for Rubber
      Property -- Compression Set) is the real, standard compression-
      set/compression-force test method this actor's own
      `rubberworks.robotics` mission actually SIMULATES (a real
      `physics-2d` time-stepped compression-platen/rubber-specimen
      collision, not a hand-set field) -- see that namespace's docstring.

  This actor is deliberately DISTINCT from `cloud-itonami-isic-2211`
  (rubber TYRES, already implemented separately, its own material-spec
  catalog and its own governor) -- this catalog covers 'other rubber
  products' (seals/gaskets/hoses/mounts/cases), never tyres.

  Coverage is reported HONESTLY: a product class not in this table has
  NO spec-basis. Seed values cite official standards-owning bodies; this
  is a starting catalog (two product classes: the smartphone/consumer and
  automotive downstream consumers this actor's own README `Scope note`
  names), not a survey of every product class a rubber-products
  manufacturer might produce (e.g. industrial/marine rubber goods, which
  have their own distinct regulatory frameworks this catalog does NOT
  cite, honestly, rather than fabricating a citation this session was
  not confident of).

  HONEST CONFIDENCE DISCLOSURE on provenance URLs: `:provenance` below
  cites each standard's owning body and (for ISO/IEC) its technical
  committee, but deliberately does NOT include a specific numbered
  catalogue-page URL or edition year, since this session could not verify
  exact current-edition catalogue numbers without live web access -- a
  fabricated-looking but unverified specific URL would be LESS honest
  than a correct, general standards-body reference. Implementers should
  confirm the current edition via the standards body's own catalogue
  before relying on this catalog for compliance.")

(def catalog
  {"AUTOMOTIVE-RUBBER"
   {:name "Automotive rubber components (ASTM D2000 grade classification + ISO 1817 fluid-resistance basis)"
    :owner-authority "ASTM International (ASTM D2000, Committee D11 on Rubber and Rubber-like Materials) / International Organization for Standardization (ISO, ISO/TC 45 Rubber and rubber products)"
    :legal-basis "ASTM D2000 (Standard Classification System for Rubber Products in Automotive Applications) -- the real, widely-cited SAE/ASTM line-callout grade-classification system for automotive rubber compounds (weatherstripping, hoses, engine/body mounts, grommets); ISO 1817 (Rubber, vulcanized -- Determination of the effect of liquids) -- the standard test method for a rubber compound's resistance to automotive fluids (fuel, oil, coolant, brake fluid)"
    :national-spec "ASTM D2000 grade-classification conformance for the compound + ISO 1817 fluid-resistance conformance for automotive-fluid-exposed rubber parts"
    :provenance "https://www.astm.org/ (ASTM D2000, Committee D11) ; https://www.iso.org/ (ISO 1817, ISO/TC 45) -- standards-body/committee reference only, not a specific catalogue-page URL/edition year (honest-precision disclosure, see ns docstring)"
    :required-evidence ["ASTM D2000 rubber-grade line-callout classification record"
                        "ISO 1817 fluid-resistance test report"
                        "Shipment quality chain-of-custody record"]}
   "ELECTRONICS-GASKET"
   {:name "Electronics/consumer molded-rubber gaskets and protective cases (IEC 60529 IP rating + ASTM D395 compression-set basis)"
    :owner-authority "International Electrotechnical Commission (IEC, IEC TC 70 Degrees of protection provided by enclosures) / ASTM International (ASTM D395, Committee D11 on Rubber and Rubber-like Materials)"
    :legal-basis "IEC 60529 (Degrees of protection provided by enclosures, IP Code) -- the standard ingress-protection rating framework directly relevant to smartphone/device waterproofing gaskets around ports and buttons; ASTM D395 (Standard Test Methods for Rubber Property -- Compression Set) -- the real, standard compression-set/compression-force test method `rubberworks.robotics`'s own mission actually simulates via a real time-stepped `physics-2d` rigid-body collision"
    :national-spec "IEC 60529 IP-rating conformance for waterproofing gaskets/seals + ASTM D395 compression-set conformance for the molded rubber gasket/case compound"
    :provenance "https://www.iec.ch/ (IEC 60529, IEC TC 70) ; https://www.astm.org/ (ASTM D395, Committee D11) -- standards-body/committee reference only, same honest-precision disclosure as AUTOMOTIVE-RUBBER above"
    :required-evidence ["IEC 60529 IP-rating ingress-protection test report"
                        "ASTM D395 compression-set test report"
                        "Shipment quality chain-of-custody record"]}})

(defn spec-basis [scheme] (get catalog scheme))

(defn coverage
  ([] (coverage (keys catalog)))
  ([schemes]
   (let [have (filter catalog schemes)
         missing (remove catalog schemes)]
     {:requested (count schemes)
      :covered (count have)
      :covered-jurisdictions (vec (sort have))
      :missing-jurisdictions (vec (sort missing))
      :note (str "cloud-itonami-isic-2219 R0: " (count catalog)
                 " product-class rubber-material-spec schemes seeded "
                 "(AUTOMOTIVE-RUBBER: ASTM D2000 + ISO 1817 / "
                 "ELECTRONICS-GASKET: IEC 60529 + ASTM D395). "
                 "Extend `rubberworks.facts/catalog`, never fabricate a "
                 "product class's requirements or present an unverified "
                 "citation as certain.")})))

(defn required-evidence-satisfied?
  [scheme submitted]
  (when-let [{:keys [required-evidence]} (spec-basis scheme)]
    (let [need (count required-evidence)
          have (count (filter (set submitted) required-evidence))]
      (= need have))))

(defn evidence-checklist [scheme]
  (:required-evidence (spec-basis scheme) []))
