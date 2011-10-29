(ns mspacman.gpmsp
  (:require [clojure.tools.trace :as trace]
            [mspacman.individual :as indv])
  (:import (java.awt.event.KeyEvent)
           (java.lang.Boolean)))

(defstruct individual
  :program
  :mspacman
  :fitness
  :finishing-time)

(def *SIZE-OF-POPULATION* 5)
(def *NUMBER-OF-GENERATIONS* 1000)
(def *MAX-STARTING-DEPTH* 50)
(def *MAX-STARTING-WIDTH-OF-EXPR* 5)

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
                               expr? (if (< (rand) 0.50)
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

(defn find-depth [tree]
  (reduce #(if (> %1 %2) %1 %2) (flatten (find-depth2 tree 0))))

(defn- find-depth2 [tree n]
  (if-not (seq? tree)
    n
    (map #(find-depth2 %1 (inc n)) tree)))