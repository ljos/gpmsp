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

(defn get-all-files [path]
  (remove nil?
          (map #(if (.isFile %) (.getName %))
               (vec (.listFiles (File. path))))))

(defn group-files [path]
  (let [files (get-all-files path)]
    (group-by-matching #"(\w\w\d{6}).+\d{3}\.txt" files)))

(defn convert-data [path]
  (let [data (group-files path)]
     (doseq [machine (keys data)
             :let [data (for [mdata (sort (get data machine))]
                          (sort > (map :fitness
                                       (let [population
                                             (read-string
                                              (slurp (format "%s/%s" path mdata)))]
                                         (if-not (:population population)
                                           population
                                           (:population population))))))
                   best (map first data)
                   average (map #(int (/ (reduce + %) (count %))) data)
                   filename "/Users/bjarte/Dropbox/master/generation-data/clean-data/%s-%s.%s.txt"
                   date (second (re-find #"[a-z]+(\d+)" (.getName (File. path))))
                   mname (second (re-find #"(^\w\w\d{6}).*" machine))
                   f (fn [dat] (reduce (fn [string [idx dat]]
                                         (str string idx " " dat "\n"))
                                       ""
                                       (map-indexed vector dat)))]]
       (spit (format filename date mname "best")
             (f best))
       (spit (format filename date mname "average")
             (f average)))))

(defn convert-all-data [path]
  (doall
   (pmap convert-data
         (remove nil?
                 (map #(if (.isDirectory %) (.getAbsolutePath %))
                      (vec (.listFiles (File. path)))))))
  (println 'done))