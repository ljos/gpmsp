(ns mspacman.gpmsp
  (:use (mspacman.individual))
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
(def *MAX-WIDTH-OF-EXPR* 10)
(def *MAX-DEPTH* 10)


(defn create-random-individual []
  (expand '(do expr+) *MAX-DEPTH*))

(defn- expand [exprs depth]
  (println exprs)
  (if (or (not (coll? exprs)) (< (count exprs) 1) (<= depth 0)) 
    (if (or (empty exprs)
            (some #(or (= 'expr %1)
                       (= 'expr+ %1)
                       (= 'expr? %1)
                       (= 'int %1)
                       (nil? %1)) exprs))
      ()
      exprs)
    (cons (first exprs)
          (apply concat 
                 (for [term  (rest exprs)]
                   (do (print 'term)
                       (println term)
                       (case term
                         expr  (list (let [t (rand-nth mspacman.individual/FUNCTION-LIST)]
                                       (do (print 'expr)
                                           (println t)
                                           (expand (if (= t 'int) (list t) t) (dec depth)))))
                         expr+ (for [t (take (inc (rand-int *MAX-WIDTH-OF-EXPR*))
                                             (repeatedly
                                              #(rand-nth mspacman.individual/FUNCTION-LIST)))]
                                 (do (print 'expr+)
                                     (println t)
                                     (if (= t 'int)
                                       (list 'int)
                                       (let [e (expand t (dec depth))]
                                         (print e)
                                         (println e)
                                         (if (every? empty (rest e))
                                           ()
                                           e)))))
                         expr? (if (rand-nth '(true false))
                                 (list (expand (rand-nth mspacman.individual/FUNCTION-LIST)
                                               (dec depth)))
                                 ())
                         int   (list (rand-int 1000))
                         (do (print 'default)
                             (println term)
                              (list term)))))))))

(defn create-random-population []
  (apply pcalls (repeat #(create-random-individual))))


(defn -main [& args]
  ())

