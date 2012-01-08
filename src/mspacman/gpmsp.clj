(ns mspacman.gpmsp
  (:require [clojure.zip :as zip]
            [clojure.string :as string]
            [clojure.data.zip :as dzip])
  (:use [mspacman.individual :as ind]
        [control.core :as con]
        [control.commands :as con.comm])
  (import java.net.InetAddress))

(defstruct individual
  :program
  :fitness)

(def SIZE-OF-POPULATION 500)
(def ELITISM-RATE 0.05)
(def NUMBER-OF-GENERATIONS 1000)
(def MAX-STARTING-DEPTH 10)
(def MAX-STARTING-WIDTH-OF-EXPR 5)
(def MUTATION-RATE 0.15)
(def REPRODUCTION-RATE 0.65)
(def MUTATION-DEPTH 5)
(def RAND-INT-RATE 0.20)
(def EXPR?-RATE 0.80)
(def FITNESS-RUNS 10)

(defn atomize [term]
  (cond (= term 'int)
        ,(if (< (rand) RAND-INT-RATE)
           (rand-int 36)
           (rand-nth ind/INT-LIST))
        (symbol? term)
        ,`~term
        :else
        ,term))

(defn expand [exprs depth]
  (if (or (symbol? exprs)
          (empty? exprs)
          (< depth 1))
    (atomize (rand-nth ind/ATOM-LIST))
    (cons (first exprs) 
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
                          (atomize term))]
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
  (let [F (reduce + (map #(:fitness %1) population))
        r (rand)]
    (loop [pop population
           slice (/ (:fitness (first population)) F)]
      (if (<= r slice)
        (first pop)
        (recur (rest pop)
               (+ slice (/ (:fitness (second pop)) F)))))))

(defn select-random-node [tree]
  (loop [loc (zip/seq-zip tree)
         val nil
         n 1]
    (cond (zip/end? loc)
          ,val
          (or (dzip/leftmost? loc) (nil? (zip/node loc)))
          ,(recur (zip/next loc) val n)
          :else
          ,(recur (zip/next loc)
                  (if (= 0 (mod (rand-int n) n))
                    loc val)
                  (inc n)))))

(defn reproduction [parents]
  (zip/root
   (zip/replace (select-random-node (first parents))
                (zip/node (select-random-node (second parents))))))

(defn mutation [tree]
  (zip/root
   (zip/replace (select-random-node tree)
                (expand (rand-nth ind/FUNCTION-LIST)
                        MUTATION-DEPTH))))

(defn testds [inn]
  (mutation (read-string inn)))

(defn recombination [population]
  (let [r (rand)]
    (cond (< r REPRODUCTION-RATE)
          ,(reproduction (repeatedly 2 #(:program (fitness-proportionate-selection population))))
          (< r (+ REPRODUCTION-RATE MUTATION-RATE))
          ,(mutation (:program (fitness-proportionate-selection population)))
          :else
          (:program (fitness-proportionate-selection population)))))

(defn run-generation [generation]
  (use 'mspacman.individual)
  (let [elitism (* SIZE-OF-POPULATION ELITISM-RATE)]
    (sort-by :fitness >
             (pmap  #(struct individual %1 (ind/fitness FITNESS-RUNS %1))
                    (concat  (map #(:program %1)
                                  (take elitism generation))
                             (repeatedly (- SIZE-OF-POPULATION elitism)
                                         #(recombination generation)))))))

(defn gp-run
  ([]
     (println "Started")
     (use 'mspacman.individual)
     (loop [generation (sort-by :fitness >
                                (pmap #(struct individual %1 (ind/fitness FITNESS-RUNS %1))
                                      (create-random-population)))
            n 0]
       (if (>= n NUMBER-OF-GENERATIONS)
         (println 'finished)
         (do (println 'generation n)
             (spit (format "%s/generations/%s_generation_%tL.txt"
                           (System/getProperty "user.home")
                           (string/lower-case (.getHostName (InetAddress/getLocalHost)))
                           n)
                   (str generation))
             (println (map #(:fitness %1) generation)
                      "average:"
                      (int (/ (reduce + (map #(:fitness %1) generation)) SIZE-OF-POPULATION)))
             (recur (run-generation generation)
                    (inc n))))))
  ([gen-file nb-gen]
     (println "Started")
     (use 'mspacman.individual)
     (loop [generation (run-generation (read-string (slurp gen-file)))
            n (inc nb-gen)]
       (if (>= n NUMBER-OF-GENERATIONS)
         (println 'finished)
         (do (println 'generation n)
             (spit (format "%s/generations/%s_generation_%tL.txt"
                           (System/getProperty "user.home")
                           (string/lower-case (.getHostName (InetAddress/getLocalHost)))
                           n)
                   (str generation))
             (println (map #(:fitness %1) generation)
                      "average:"
                      (int (/ (reduce + (map #(:fitness %1) generation)) SIZE-OF-POPULATION)))
             (recur (run-generation generation)
                    (inc n)))))))

(defn run-gen [input]
  (run-generation (read-string input)))

(defn run-control [cluster task args]
  (use 'control.core)
  (use 'control.commands)
  (load-file "control.clj")
  (map #(read-string (:stdout %1))
       (filter #(zero? (:status %1))
               (con/do-begin (list cluster
                                   task
                                   args)))))