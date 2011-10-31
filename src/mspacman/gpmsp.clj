(ns mspacman.gpmsp
  (:require [clojure.tools.trace :as trace]
            [mspacman.individual :as indv]
            [clojure.zip :as zip])
  (:import (java.awt.event.KeyEvent)
           (java.lang.Boolean)))

(defstruct individual
  :program
  :fitness
  :finishing-time)

(def *SIZE-OF-POPULATION* 5)
(def *NUMBER-OF-GENERATIONS* 5)
(def *MAX-STARTING-DEPTH* 10)
(def *MAX-STARTING-WIDTH-OF-EXPR* 5)
(def *MUTATION-RATE* 0.02)
(def *MUTATION-DEPTH* 5)
(def *EXPR?-RATE* 0.3)
(def *FITNESS-RUNS* 1)

(defn expand [exprs depth]
  (cond (= exprs 'int)
        ,(rand-int 1000)
        (or (empty? exprs) (< depth 1))
        ,()
        :else
        ,(cons (first exprs) 
               (loop [terms (rest exprs)
                      acc ()
                      expr-width (rand-int *MAX-STARTING-WIDTH-OF-EXPR*)]
                 (if (empty? terms)
                   acc
                   (let [term (first terms)
                         exp (case term
                               (expr expr+) (expand (rand-nth indv/FUNCTION-LIST)
                                                    (dec depth))
                               expr? (if (< (rand) *EXPR?-RATE*)
                                       (expand (rand-nth indv/FUNCTION-LIST)
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
  (expand '(do expr+) *MAX-STARTING-DEPTH*))

(defn create-random-population []
  (apply pcalls (repeat *SIZE-OF-POPULATION* #(create-random-individual))))

(defn mutate [tree]
  (loop [loc (zip/seq-zip tree)]
    (cond (zip/end? loc)
          ,(zip/root loc)
          (and (not (symbol? (zip/node loc)))
               (not (nil? (zip/node loc)))
               (> *MUTATION-RATE* (rand)))
          ,(zip/root (zip/replace loc
                                  (expand (rand-nth indv/FUNCTION-LIST)
                                          *MUTATION-DEPTH*)))
          :else
          ,(recur (zip/next loc)))))

(defn -main [& args]
  (loop [generation (sort-by :fitness
                             >
                             (pmap #(struct individual %1 (indv/fitness *FITNESS-RUNS* %1) 0)
                                   (create-random-population)))
         n *NUMBER-OF-GENERATIONS*]
    (recur (pmap #(let [mutated (assoc %1 :program (mutate (get %1 :program)))]
                    (assoc mutated :fitness
                           (indv/fitness (get mutated :program))))
                 '(NEED NEW GENERATION HERE!))
           (dec n))))