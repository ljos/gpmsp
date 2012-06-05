;; ITER 1 NOV 3
;; ITER 2 MAR 9
(ns data-calc.core
  (:import java.io.File))

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

(defn get-data-for [machine]
  (for [file (range 100)]
    (read-string (slurp (format "MN121037.klientdrift.uib.no_generation_%tL.txt" (long file))))))

(defn get-all-files [path]
  (remove nil?
          (map #(if (.isFile %) (.getName %))
               (vec (.listFiles (File. path))))))

(defn group-files [path]
  (let [files (get-all-files path)]
    (group-by-matching #"(\w\w\d{6}\.klientdrift\.uib\.no).+\d{3}\.txt" files)))

(defn convert-data [path]
  (let [data (group-files path)]
     (doseq [machine (keys data)
             :let [prd (for [mdata (sort (get data machine))]
                         (let  [d (sort > (map :fitness
                                               (read-string
                                                (slurp (format "%s/%s" path mdata)))))]
                           (first d)))]]
       (spit (format "/Users/bjarte/Dropbox/master/generation-data/clean-data/%s-%s.best.txt"
                     (second (re-find #"[a-z]+(\d+)" (.getName (File. path))))
                     (second (re-find #"(^\w\w\d{6}).+" machine)))
             (reduce (fn [string [idx dat]]
                       (str string idx " " dat "\n"))
                     ""
                     (map-indexed vector prd))))))