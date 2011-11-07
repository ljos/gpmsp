(ns mspacman.gpmsp
  (:require [clojure.zip :as zip]
            [mspacman.individual :as ind]))


(import java.net.InetAddress)


(defstruct individual
  :program
  :fitness
  :finishing-time)

(def SIZE-OF-POPULATION 40)
(def NUMBER-OF-GENERATIONS 100)
(def MAX-STARTING-DEPTH 10)
(def MAX-STARTING-WIDTH-OF-EXPR 5)
(def MUTATION-RATE 0.02)
(def MUTATION-DEPTH 5)
(def EXPR?-RATE 0.3)
(def FITNESS-RUNS 2)

(defn expand [exprs depth]
  (cond (= exprs 'int)
        ,(rand-int 1000)
        (or (empty? exprs) (< depth 1))
        ,()
        :else
        ,(cons (first exprs) 
               (loop [terms (rest exprs)
                      acc ()
                      expr-width (rand-int MAX-STARTING-WIDTH-OF-EXPR)]
                 (if (empty? terms)
                   acc
                   (let [term (first terms)
                         exp (case term
                               (expr expr+) (expand (rand-nth ind/FUNCTION-LIST)
                                                    (dec depth))
                               expr? (if (< (rand) EXPR?-RATE)
                                       (expand (rand-nth ind/FUNCTION-LIST)
                                               (dec depth))
                                       ())
                               int (expand term
                                           (dec depth))
                               `~term)]
                     (recur (if (and (= term 'expr+)
                                     (> expr-width 0))
                              terms
                              (rest terms))
                            (cons exp acc)
                            (dec expr-width))))))))

(defn create-random-individual []
  (expand '(do expr+) MAX-STARTING-DEPTH))

(defn create-random-population []
  (take SIZE-OF-POPULATION (repeatedly #(create-random-individual))))

(defn fitness-proportionate-selection [population]
  (let [F (reduce + (map #(get %1 :fitness) population))]
    (loop [acc ()]
      (let [r (rand)]
        (if (= (count acc) SIZE-OF-POPULATION)
          acc
          (recur (conj acc
                       (loop [pop population
                              slice (/ (get (first population) :fitness) F)]
                         (if (<= r slice)
                           (first pop)
                           (recur (rest pop)
                                  (+ slice
                                     (/ (get (second pop) :fitness)
                                        F))))))))))))

(defn mutate [tree]
  (loop [loc (zip/seq-zip tree)]
    (cond (zip/end? loc)
          ,(zip/root loc)
          (and (not (symbol? (zip/node loc)))
               (not (nil? (zip/node loc)))
               (> MUTATION-RATE (rand)))
          ,(zip/root (zip/replace loc
                                  (expand (rand-nth ind/FUNCTION-LIST)
                                         MUTATION-DEPTH)))
          :else
          (recur (zip/next loc)))))

(defn gp-run []
  (println 'started)
  (use 'mspacman.individual)
  (loop [generation (sort-by :fitness
                             >
                             (pmap #(struct individual %1 (ind/fitness FITNESS-RUNS %1) 0)
                                   (create-random-population)))
         n 0]
    (if (>= n NUMBER-OF-GENERATIONS)
      (println 'finished)
      (do (println 'generation n)
          (spit (format "%s/generations/%s_generation_%s.txt"
                        (System/getProperty "user.home")
                        (.getHostName (InetAddress/getLocalHost))
                        n)
                (str generation))
          (println (map #(get %1 :fitness) generation))
          (recur (sort-by :fitness
                          >
                          (pmap  #(struct individual %1 (ind/fitness FITNESS-RUNS %1) 0)
                                 (map #(mutate (get %1 :program))
                                      (fitness-proportionate-selection generation))))
                 (inc n))))))