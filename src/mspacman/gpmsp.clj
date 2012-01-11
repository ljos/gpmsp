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

(def SIZE-OF-POPULATION 200)
(def ELITISM-RATE 0.05)
(def NUMBER-OF-GENERATIONS 1000)
(def MAX-STARTING-DEPTH 10)
(def MAX-STARTING-WIDTH-OF-EXPR 5)
(def MUTATION-RATE 0.15)
(def REPRODUCTION-RATE 0.65)
(def MUTATION-DEPTH 5)
(def RAND-INT-RATE 0.25)
(def EXPR?-RATE 0.80)
(def FITNESS-RUNS 10)
(def SELECTION 'fitness-proportionate)
(def TOURNAMENT-SIZE 5)

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

(defn tournament-selection [tournament-size population]
  (first (sort :fitness > (repeatedly tournament-size #(rand-nth population)))))

(defn selection [population]
  (case SELECTION
    fitness-proportionate (fitness-proportionate-selection population)
    tournament-selection (tournament-selection TOURNAMENT-SIZE population)))

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

(defn recombination [population]
  (let [r (rand)]
    (cond (< r REPRODUCTION-RATE)
          ,(reproduction (repeatedly 2 #(:program (selection population))))
          (< r (+ REPRODUCTION-RATE MUTATION-RATE))
          ,(mutation (:program (selection population)))
          :else
          (:program (selection population)))))

(defn run-generation [generation]
  (use 'mspacman.individual)
  (let [elitism (* SIZE-OF-POPULATION ELITISM-RATE)]
    (sort-by :fitness >
             (pmap  #(struct individual %1 (ind/fitness FITNESS-RUNS %1))
                    (concat  (map #(:program %)
                                  (take elitism generation))
                             (repeatedly (- SIZE-OF-POPULATION elitism)
                                         #(recombination generation)))))))

(defn- gp-go [gen gen-nb]
  (use 'mspacman.individual)
  (loop [generation gen 
         n gen-nb]
    (if (>= n NUMBER-OF-GENERATIONS)
         (println 'finished)
         (do (println 'generation n)
             (spit (format "%s/generations/%s_generation_%tL.txt"
                           (System/getProperty "user.home")
                           (string/lower-case (.getHostName (InetAddress/getLocalHost)))
                           n)
                   (str generation))
             (println (map #(:fitness %) generation)
                      "average:"
                      (int (/ (reduce + (map #(:fitness %) generation)) SIZE-OF-POPULATION)))
             (recur (run-generation generation)
                    (inc n))))))

(defn gp-run
  ([]
     (println "Started")
     (use 'mspacman.individual)
     (gp-go (sort-by :fitness >
                     (pmap #(struct individual %1 (ind/fitness FITNESS-RUNS %1))
                           (create-random-population))) 0))
  ([gen-file nb-gen]
     (println (format "Started at generation %s." nb-gen))
     (gp-go (run-generation (read-string (slurp gen-file))) nb-gen)))

(defn run-gen [input]
  (run-generation (read-string input)))

(defn run-control [cluster task]
  (use 'control.core)
  (use 'control.commands)
  (load-file "control.clj")
  (map #(read-string (:stdout %1))
       (filter #(zero? (:status %1))
               (con/do-begin (list cluster
                                   task)))))

(defn contrl []
  (let [out (doall (pmap #(binding [con/*enable-logging* false]
                      (exec % "bjo013" (list "date")))
                   '( ;;"mn121033"	
                     ;;"mn121034"	
                     ;;"mn121035"	
                     ;;"mn121036"	
                     ;;"mn121037"	
                     ;;"mn121038"	
                     "mn121039"	
                     ;;"mn121040"	
                     "mn121041"	
                     "mn121042"	
                     "mn121043"	
                     "mn121044"	
                     "mn121045"
                     "mn121046"	
                     "mn121047"	
                     "mn121048"	
                     "mn121049"	
                     "mn121050"	
                     "mn121051"	
                     "mn121052"	
                     "mn121053"	
                     "mn121054"
                     "mn121055"
                     "mn121056"	
                     "mn121057"	
                     "mn121058"	
                     "mn121069"	
                     "mn121071"	
                     "mn121072"	
                     "mn121073"	
                     "mn121074"	
                     "mn121075"	
                     "mn121077")))]
    (shutdown-agents)
    (map :stdout out)))

