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

(def SIZE-OF-POPULATION 300)
(def ELITISM-RATE 0.05)
(def NUMBER-OF-GENERATIONS 1000)
(def MAX-STARTING-DEPTH 10)
(def MAX-STARTING-WIDTH-OF-EXPR 5)
(def MUTATION-RATE 0.15)
(def REPRODUCTION-RATE 0.70)
(def MUTATION-DEPTH 5)
(def RAND-INT-RATE 0.25)
(def EXPR?-RATE 0.80)
(def FITNESS-RUNS 7)

(def SELECTION 'fitness-proportionate)
(def TOURNAMENT-SIZE 10)

(defn atomize [term]
  (cond (= term 'int)
        (rand-int 288)
        (= term 'entity)
        ,(rand-nth ind/ENTITY-LIST)
        (= term 'item)
        ,(rand-nth ind/ITEM-LIST)
        (symbol? term)
        ,`~term
        :else
        ,term))

(defn expand [exprs depth]
  (if (or (symbol? exprs)
          (empty? exprs)
          (< depth 1))
    (if (symbol? exprs)
      (atomize exprs)
      (atomize (rand-nth ind/ATOM-LIST)))
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
                       (concat acc (list exp))
                       (dec expr-width))))))))

(defn create-random-individual []
  (expand (rand-nth ind/FUNCTION-LIST) MAX-STARTING-DEPTH))

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
  (if (or (symbol? tree)
          (number? tree)
          (>= 1 (count tree)))
    (zip/seq-zip tree)
    (loop [loc (zip/seq-zip tree)
          val nil
          n 1]
     (cond (zip/end? loc)
           ,val
           (or (dzip/leftmost? loc) (nil? (zip/node loc)))
           ,(recur (zip/next loc) val n)
           :else
           ,(recur (zip/next loc)
                   (if (zero? (mod (rand-int n) n))
                     loc
                     val)
                   (inc n))))))

(defn reproduction [parents]
  (let [original (select-random-node (first parents))
        replacement (select-random-node (second parents))]
    (zip/root
     (zip/replace (if (or (zip/branch? original)
                          (symbol? (zip/root original))
                          (number? (zip/root original)))
                    original
                    (zip/up original))
                  (zip/node replacement)))))

(defn find-relevant-expr [loc]
  (let [l (zip/node (zip/leftmost loc))
        n (count (zip/lefts loc))
        expr (first (filter #(and (not (symbol? %))
                                  (= (first %) l))
                            ind/FUNCTION-LIST))
        c (if (= (second expr) 'expr+)
            'expr+
            (nth expr n))]
    (case c 
      (expr expr+) (rand-nth ind/FUNCTION-LIST)
      entity (rand-nth ind/ENTITY-LIST)
      item (rand-nth ind/ITEM-LIST))))

(defn mutation [tree]
  (let [original (select-random-node tree)
        replacement (if (or (symbol? tree)
                            (number? tree)
                            (=  (count tree)))
                      (expand (rand-nth ind/FUNCTION-LIST) MUTATION-DEPTH)
                      (expand (find-relevant-expr original) MUTATION-DEPTH))]
    (zip/root
     (zip/replace original replacement))))

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
             (pmap #(struct individual % (ind/fitness FITNESS-RUNS %))
                   (concat (map #(:program %)
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
                           (create-random-population)))
            0))
  ([gen-file nb-gen]
     (println (format "Started at generation %s." nb-gen))
     (gp-go (run-generation (read-string (slurp gen-file))) (read-string nb-gen))))

(defn run-gen [input]
  (use 'mspacman.individual)
  (sort-by :fitness > (doall (pmap #(assoc % :fitness (ind/fitness FITNESS-RUNS (:program %)))
                                   (read-string input)))))

(defn- find-useable-machines [machines]
  (map :machine
       (filter #(zero? (:status %))
               (doall
                (map con/run-task
                     (doall
                      (map #(con/send-to-machine % "~/.scripts/check_for_user;")
                           machines)))))))

(defn- send-population [machines population]
  (doall
   (map con/run-task
        (doall
         (map #(con/send-to-machine %1 (format "cd mspacman; %s '%s' 2>&1 | tee ~/log/$(hostname -s).log"
                                               "~/.lein/bin/lein trampoline run -m mspacman.gpmsp/run-gen"
                                               (apply list %2)))
              machines
              (doall (partition (int (/ SIZE-OF-POPULATION
                                        (count machines)))
                                population)))))))

 (defn gp-over-cluster [population n]
  (println "Started")
  (let [machines  (find-useable-machines con/ALL-MACHINES)
        from-machines (send-population machines population)
        generation (sort-by :fitness >
                            (mapcat read-string
                                    (remove nil?
                                            (map #(if (zero? (:status %))
                                                    (:stdout %))
                                                 from-machines))))]
    (newline)
    (println 'generation n)
    (spit (format "%s/generations/%s_generation_%tL.txt"
                  (System/getProperty "user.home")
                  (string/lower-case (.getHostName (InetAddress/getLocalHost)))
                  n)
          (str generation))
    (println (map #(:fitness %) generation)
             "average:"
             (int (/ (reduce + (map #(:fitness %)
                                    generation))
                     SIZE-OF-POPULATION)))
    (newline)
    generation))

(defn start-gp-cluster []
  (println "Started")
  (let [elitism (* SIZE-OF-POPULATION ELITISM-RATE)]
    (loop [population (gp-over-cluster (map #(struct individual % 0)
                                            (create-random-population))
                                       0)
           n 1]
      (if (< n NUMBER-OF-GENERATIONS)
        (recur (gp-over-cluster (concat (take elitism population)
                                        (map #(struct individual %  0)
                                             (repeatedly (- SIZE-OF-POPULATION elitism)
                                                         #(recombination population))))
                                n)
               (dec n))
        (shutdown-agents)))))

 (defn test-start-gp-c []
  (println "Started")
  (let [machines  (find-useable-machines con/ALL-MACHINES)
        from-machines (send-population machines (map #(struct individual % 0)
                                                     (create-random-population)))
        generation from-machines]
    generation))