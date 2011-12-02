(ns mspacman.gpmsp
  (:require [clojure.zip :as zip]
            [clojure.string :as string]
            [clojure.data.zip :as dzip]
            [mspacman.individual :as ind]))

(import java.net.InetAddress)

(defstruct individual
  :program
  :fitness
  :finishing-time)

(def SIZE-OF-POPULATION 100)
(def NUMBER-OF-GENERATIONS 1000)
(def MAX-STARTING-DEPTH 10)
(def MAX-STARTING-WIDTH-OF-EXPR 5)
(def MUTATION-RATE 0.20)
(def REPRODUCTION-RATE 0.60)
(def MUTATION-DEPTH 5)
(def EXPR?-RATE 0.80)
(def FITNESS-RUNS 10)

(defn atomize [term]
  "Makes a leaf node"
  (cond (= term 'int)
        ,(rand-int 1000)
        (symbol? term)
        ,`~term
        :else
        ,term))

(defn expand [exprs depth]
  "Expands an expression; creates a code tree for an individual."
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
  "Creates a random code tree for an individual with a set depth."
  (expand '(do expr+) MAX-STARTING-DEPTH))

(defn create-random-population []
  "Creates code trees for a whole population."
  (take SIZE-OF-POPULATION (repeatedly #(create-random-individual))))

(defn fitness-proportionate-selection [population]
  "Select an individual from pop via fitness proportionate selection"
  (let [F (reduce + (map #(get %1 :fitness) population))
        r (rand)]
    (loop [pop population
           slice (/ (get (first population) :fitness) F)]
      (if (<= r slice)
        (first pop)
        (recur (rest pop)
               (+ slice (/ (get (second pop) :fitness) F)))))))

(defn select-random-node [tree]
  "selects a random node in a tree."
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
  "Takes two parents and creates a new individual from them."
  (zip/root
   (zip/replace (select-random-node (first parents))
                (zip/node (select-random-node (second parents))))))

(defn mutation [tree]
  "Using reservoir sampling to take a uniformly and randomly chosen element from the code tree"
  (zip/root
   (zip/replace (select-random-node tree)
                (expand (rand-nth ind/FUNCTION-LIST)
                        MUTATION-DEPTH))))

(defn recombination [population]
  "Takes a population and creates a new individual through recombination."
  (let [r (rand)]
    (cond (< r REPRODUCTION-RATE)
          ,(reproduction (repeatedly 2 #(get (fitness-proportionate-selection population)
                                             :program)))
          (< r (+ REPRODUCTION-RATE MUTATION-RATE))
          ,(mutation (get (fitness-proportionate-selection population)
                          :program))
          :else
          (get (fitness-proportionate-selection population)
               :program))))

(defn gp-run []
  "Does a gp-run"
  (println 'started)
  (use 'mspacman.individual)
  (loop [generation (sort-by :fitness >
                             (pmap #(struct individual %1 (ind/fitness FITNESS-RUNS %1) 0)
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
          (println (map #(get %1 :fitness) generation)
                   "average:"
                   (int (/ (reduce + (map #(get %1 :fitness) generation)) SIZE-OF-POPULATION)))
          (recur (sort-by :fitness >
                          (pmap  #(struct individual %1 (ind/fitness FITNESS-RUNS %1) 0)
                                 (repeatedly SIZE-OF-POPULATION #(recombination generation))))
                 (inc n))))))