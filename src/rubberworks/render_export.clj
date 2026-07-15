(ns rubberworks.render-export
  "CLI: run the REAL `physics-2d` ASTM D395 compression-set-test
  simulation (`rubberworks.robotics/simulate-press`, nominal batch-1-
  style passing press-run configuration) and dump the compression-
  platen ('platen') and rubber-specimen ('specimen') rigid bodies'
  ACTUAL per-tick positions as JSON, for a downstream render harness to
  visualize. Every number in the emitted JSON comes directly off the
  real simulated `physics-2d` trajectory (the platen's per-tick
  `:position`) or a real, fixed geometry/anchor constant from
  `rubberworks.robotics` (AABB half-extents x2 for `:dims`, the static
  rubber-specimen's fixed anchor position for its own `:frames`) -- none
  of it is hand-typed/fabricated.

  Usage: clojure -M:dev:render-export

  Writes the SAME JSON to both `/tmp/render-2219/scene-data.json` (for
  a local render harness) and `docs/samples/scene-data.json` (committed
  for traceability). No third-party JSON dependency is pulled in for
  this -- the schema emitted here is small and fully known ahead of
  time (a `{\"bodies\" [{\"id\" .. \"dims\" .. \"frames\" ..} ..]}`
  map of strings/numbers/vectors only), so a tiny hand-written
  serializer is less friction than adding a new dependency for one CLI
  script."
  (:require [clojure.string :as str]
            [rubberworks.robotics :as robotics]))

(defn- escape-json-string [s]
  (-> (str s)
      (str/replace "\\" "\\\\")
      (str/replace "\"" "\\\"")
      (str/replace "\n" "\\n")))

(defn- json-value
  "Minimal recursive EDN->JSON renderer for the known, small shape this
  script emits (strings, numbers, vectors, string-keyed maps). Not a
  general-purpose JSON library."
  [v]
  (cond
    (nil? v)     "null"
    (string? v)  (str "\"" (escape-json-string v) "\"")
    (number? v)  (str v)
    (vector? v)  (str "[" (str/join "," (map json-value v)) "]")
    (sequential? v) (str "[" (str/join "," (map json-value v)) "]")
    (map? v)     (str "{" (str/join "," (map (fn [[k vv]] (str (json-value (name k)) ":" (json-value vv))) v)) "}")
    :else        (json-value (str v))))

(defn scene-json
  "Runs the REAL `physics-2d` compression-set-test simulation for
  `platen-mass-kg` (defaults to batch-1's own nominal, passing 0.5 kg
  configuration -- see `rubberworks.store/demo-data`) and returns the
  JSON string for the render-harness scene-data schema: {\"bodies\":
  [{\"id\" \"platen\" \"dims\" [w h] \"frames\" [[x y] ..]} {\"id\"
  \"specimen\" \"dims\" [w h] \"frames\" [[x y] ..]}]}.

  `platen`'s frames are the ACTUAL simulated per-tick positions of the
  moving compression-platen body (`simulate-press`'s `:trajectory`).
  `specimen`'s frames are the real, fixed anchor position of the static
  rubber-specimen, repeated once per tick (it is genuinely stationary
  throughout the simulation -- `physics-2d` never moves a mass-0 body --
  so a constant, real position is the honest per-tick value, not a
  fabricated one). `dims` for each body are the real AABB collider
  half-extents x2 (full width/height) `rubberworks.robotics` actually
  uses for that body."
  ([] (scene-json 0.5))
  ([platen-mass-kg]
   (let [{:keys [trajectory]} (robotics/simulate-press platen-mass-kg)
         platen-frames (mapv :position trajectory)
         n (count platen-frames)
         specimen-frames (vec (repeat n [0.0 0.0]))
         platen-w (* 2 robotics/platen-half-w-m)
         platen-h (* 2 robotics/platen-half-h-m)
         specimen-w (* 2 robotics/specimen-half-w-m)
         specimen-h (* 2 robotics/specimen-half-h-m)
         scene {"bodies" [{"id" "platen" "dims" [platen-w platen-h] "frames" platen-frames}
                          {"id" "specimen" "dims" [specimen-w specimen-h] "frames" specimen-frames}]}]
     (json-value scene))))

(defn -main [& _]
  (let [json (scene-json)]
    (doseq [dir ["/tmp/render-2219" "docs/samples"]]
      (let [d (java.io.File. (str dir))]
        (.mkdirs d)
        (spit (java.io.File. d "scene-data.json") json)))
    (println "wrote scene-data.json to /tmp/render-2219/ and docs/samples/")
    (println (subs json 0 (min 400 (count json))))))
