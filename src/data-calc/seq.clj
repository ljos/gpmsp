(ns data-calc.seq)

(defn group-by-matching
  "Takes a regex and a string-list with optional key :no-match.
   Returns a map consiting of the strings mapped to how they satisfy the
   regex. If :no-match is present string that does not match the regex is put in here.

   Ex.:

   user=> (group-by-matching #\"(\\d+).+\" [\"123asd\" \"123tre\" \"54sd\" \"sdf\"])
   { \"123\" (\"123asd\" \"123tre\"), \"54\" (\"54sd\") }

   user=> (group-by-matching #\"(\\d+).+\" [\"123asd\" \"123tre\" \"54sd\" \"sdf\"] :no-match)
   {:no-match (\"sdf\") \"123\" (\"123asd\" \"123tre\"), \"54\" (\"54sd\") }"
  [regex string-list & [key]]
 (letfn [(fold-match [group [string matches]]
            (reduce #(assoc! %1 %2 (conj (get %1 %2 []) string))
                    group
                    (if (and (empty? matches)
                             (= key :no-match))
                      [:no-match]
                      matches)))
         (match-groups [string]
           [string (rest (re-matches regex string))])]
   (persistent!
    (reduce fold-match
            (transient {})
            (map match-groups string-list)))))