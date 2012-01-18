(ns mspacman.gpmsp
  (:require [clojure.zip :as zip]
            [clojure.string :as string]
            [clojure.data.zip :as dzip]
            [mspacman.control :as con])
  (:use [mspacman.individual :as ind])
  (import java.net.InetAddress))

(defstruct individual
  :program
  :fitness)

(def SIZE-OF-POPULATION 50)
(def ELITISM-RATE 0.05)
(def NUMBER-OF-GENERATIONS 1000)
(def MAX-STARTING-DEPTH 10)
(def MAX-STARTING-WIDTH-OF-EXPR 5)
(def MUTATION-RATE 0.15)
(def REPRODUCTION-RATE 0.65)
(def MUTATION-DEPTH 5)
(def RAND-INT-RATE 0.25)
(def EXPR?-RATE 0.80)
(def FITNESS-RUNS 5)
(def SELECTION 'fitness-proportionate)
(def TOURNAMENT-SIZE 5)

(defn atomize [term]
  (cond (= term 'int)
        (rand-int 288)
        (= term 'item)
        ,(rand-nth ind/ITEM-LIST)
        (= term 'entity)
        ,(rand-nth ind/ENTITY-LIST)
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
  (use 'mspacman.individual)
  (sort-by :fitness > (pmap #(assoc % :fitness (ind/fitness FITNESS-RUNS (:program %)))
                            (read-string input))))

(defn gp-over-cluster [pop]
  (println "Started")
  (let [machines  
        (map :machine
             (filter #(= 0 (:status %))
                     (doall (map con/run-task
                                 (doall (map #(con/send-to-machine % (format "~/.scripts/check_for_user;"))
                                             con/ALL-MACHINES))))))
        population pop
        out (doall (map con/run-task
                        (doall (map #(con/send-to-machine %1
                                                          (format "cd mspacman; %s '%s'"
                                                                  "~/.lein/bin/lein run -m mspacman.gpmsp/run-gen"
                                                                  (apply list %2)))
                                    machines
                                    (doall (partition (int (/ SIZE-OF-POPULATION (count machines)))
                                                      population))))))]
    (sort-by :fitness > (mapcat read-string
                                (remove nil?
                                        (map :stdout out))))))

(defn start-gp-cluster []
  (println "Started")
  (let [elitism (* SIZE-OF-POPULATION ELITISM-RATE)]
    (loop [population (gp-over-cluster (map #(struct individual % 0)
                                            (create-random-population)))
           n NUMBER-OF-GENERATIONS]
      (if (< 0 n)
        (recur (gp-over-cluster (concat (take elitism population)
                                        (map #(struct individual %  0)
                                             (repeatedly (- SIZE-OF-POPULATION elitism)
                                                         #(recombination population)))))
               (dec n))
        (shutdown-agents)))))

(defn clustertest []
  (let  [out (doall (map con/run-task
                         (map #(con/send-to-machine % "~/.scripts/check_for_user; echo $(hostname) : $(date)")
                              con/ALL-MACHINES)))]
    (shutdown-agents)
    (map :stdout (filter #(= 0 (:status %)) out))))



