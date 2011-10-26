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


(defn create-random-individual []
  (expand '(do expr+) *MAX-DEPTH*))

(defn expand [exprs depth]
  (if (or (= exprs 'int) (empty? exprs) (< depth 1))
    (if (= exprs 'int)
      (rand-int 1000)
      ())
    (cons  (first exprs)
           (loop [terms (rest exprs)
                  acc ()
                  expr-width (rand-int *MAX-WIDTH-OF-EXPR*)]
             (if  (or (empty? terms) (< depth 1))
               acc
               (let  [exp (case (first terms)
                             (expr expr+) (expand (rand-nth mspacman.individual/FUNCTION-LIST)
                                                  (dec depth))
                             expr? (if (< (rand) 0.50)
                                     (expand (rand-nth mspacman.individual/FUNCTION-LIST)
                                             (dec depth))
                                     ())
                             int (rand-int 1000)
                             (first terms))]
                 (recur (if (and (= (first terms) 'expr+)
                                 (> expr-width 0))
                          terms
                          (rest terms))
                        (cons exp acc)
                        (dec expr-width))))))))

(defn create-random-population []
  (apply pcalls (repeat *SIZE-OF-POPULATION* #(create-random-individual))))


(defn -main [& args]
  ())

