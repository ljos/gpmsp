(ns mspacman.gpmsp
  (:require clojure.tools.trace)
  (:import (java.awt.event.KeyEvent)
           (java.lang.Boolean)))


(defstruct individual
  :program
  :mspacman
  :fitness
  :finishing-time)

(def *SIZE-OF-POPULATION* 50)
(def *NUMBER-OF-GENERATIONS* 1000)
(def *MAX-STARTING-DEPTH* 10)
(def *MAX-WIDTH-OF-EXPR* 5)
(def *MAX-DEPTH* 10)

(defn expand [exprs depth]
  (cond (= exprs 'int)
        ,(rand-int 1000)
        (or (empty? exprs) (< depth 1))
        ,()
        :else
        ,(cons (first exprs) 
               (loop [terms (rest exprs)
                      acc ()
                      expr-width (rand-int *MAX-WIDTH-OF-EXPR*)]
                 (if (empty? terms)
                   acc
                   (let [term (first terms)
                         exp (case term
                               (expr expr+) (expand (rand-nth mspacman.individual/FUNCTION-LIST)
                                                    (dec depth))
                               expr? (if (< (rand) 0.50)
                                       (expand (rand-nth mspacman.individual/FUNCTION-LIST)
                                               (dec depth))
                                       ())
                               int (expand term (dec depth))
                               `~term)]
                     (recur (if (and (= term 'expr+)
                                     (> expr-width 0))
                              terms
                              (rest terms))
                            (cons exp acc)
                            (dec expr-width))))))))

(defn create-random-individual []
  (expand '(do expr+) *MAX-DEPTH*))

(defn create-random-population []
  (apply pcalls (repeat *SIZE-OF-POPULATION* #(create-random-individual))))


(defn -main [& args]
  ())

