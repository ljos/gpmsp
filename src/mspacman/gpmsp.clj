(ns mspacman.gpmsp
  (:require [clojure.zip :as zip]))

(import java.net.InetAddress)
(use 'mspacman.individual)

(defstruct individual
  :program
  :fitness
  :finishing-time)

(def SIZE-OF-POPULATION 40)
(def NUMBER-OF-GENERATIONS 100)
(def MAX-STARTING-DEPTH 40)
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
                               (expr expr+) (expand (rand-nth FUNCTION-LIST)
                                                    (dec depth))
                               expr? (if (< (rand) EXPR?-RATE)
                                       (expand (rand-nth FUNCTION-LIST)
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
  (apply pcalls (repeat SIZE-OF-POPULATION #(create-random-individual))))

(defn mutate [tree]
  (loop [loc (zip/seq-zip tree)]
    (cond (zip/end? loc)
          ,(zip/root loc)
          (and (not (symbol? (zip/node loc)))
               (not (nil? (zip/node loc)))
               (> MUTATION-RATE (rand)))
          ,(zip/root (zip/replace loc
                                  (expand (rand-nth FUNCTION-LIST)
                                          MUTATION-DEPTH)))
          :else
          ,(recur (zip/next loc)))))

(defn gp-run []
  (println 'started)
  (loop [generation (sort-by :fitness
                             >
                             (pmap #(struct individual %1 (fitness FITNESS-RUNS %1) 0)
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
          (let [F (reduce + (map #(get %1 :fitness) generation))]
            (recur (sort-by :fitness >
                            (doall (pmap #(let [mutated (assoc %1 :program (mutate (get %1 :program)))]
                                            (assoc mutated :fitness
                                                   (fitness FITNESS-RUNS (get mutated :program))))
                                         (take SIZE-OF-POPULATION
                                               (repeatedly #(let [r (rand)]
                                                              (loop [pop generation
                                                                     slice 0]
                                                                (let [score (+ slice (/ (get (first pop)
                                                                                             :fitness) F))]
                                                                  (if (<= r score)
                                                                    (first pop)
                                                                    (recur (rest pop) score))))))))))
                   (inc n)))))))




(defn gp-test []
  (println 'started)
  (loop [generation '({:program (do (move-left)) :fitness 120 :finishing-time 0})
         n 0]
    (println generation)
    (if (>= n NUMBER-OF-GENERATIONS)
      (println 'finished)
      (do (println 'generation n)
          (spit (format "%s/generations/%s_generation_%s.txt"
                        (System/getProperty "user.home")
                        (.getHostName (InetAddress/getLocalHost))
                        n)
                (str generation))
          (println (map #(get %1 :fitness) generation))
          (let [F (reduce + (map #(get %1 :fitness) generation))]
            (recur (sort-by :fitness >
                            (doall (pmap #(let [mutated (assoc %1 :program (mutate (get %1 :program)))]
                                      (assoc mutated :fitness
                                             (fitness FITNESS-RUNS (get mutated :program))))
                                   (take SIZE-OF-POPULATION
                                         (repeatedly #(let [r (rand)]
                                                        (loop [pop generation
                                                               slice 0]
                                                          (let [score (+ slice (/ (get (first pop) :fitness) F))]
                                                            (if (<= r score)
                                                              (first pop)
                                                              (recur (rest pop) score))))))))))
                   (inc n)))))))