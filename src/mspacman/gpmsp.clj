(ns mspacman.gpmsp
  (:require [clojure.zip :as zip]
            [clojure.string :as string]
            [clojure.data.zip :as dzip]
            [clojure.java.shell :as shell]
            [mspacman.control :as con])
  (:use [mspacman.individual :as ind])
  (import java.net.InetAddress)
  (import java.util.Calendar)
  (import java.text.SimpleDateFormat))

(defstruct individual :program :fitness :time)

(def SIZE-OF-POPULATION 700)
(def ELITISM-RATE 0.05)
(def NUMBER-OF-GENERATIONS 1000)
(def MAX-STARTING-DEPTH 10)
(def MAX-STARTING-WIDTH-OF-EXPR 10)
(def MUTATION-RATE 0.15)
(def REPRODUCTION-RATE 0.75)
(def MUTATION-DEPTH 3)
(def RAND-INT-RATE 0.25)
(def EXPR?-RATE 0.50)
(def FITNESS-RUNS 5)

(def SELECTION 'fitness-proportionate)
(def TOURNAMENT-SIZE 10)

(def STARTED (.format (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ssZ")
                      (.getTime (Calendar/getInstance))))

(declare expand)
(defn atomize [term]
  (cond (= term 'value)
        ,(rand-nth ind/VALUE-LIST)
        (= term 'radius)
        ,(rand-nth ind/RADIUS-LIST)
        (= term 'pill)
        ,(rand-nth ind/PILL-LIST)
        (= term 'superpill)
        ,(rand-nth ind/SUPERPILL-LIST)
        (= term 'blue)
        ,(rand-nth ind/BLUE-LIST)
        (= term 'x)
        ,(rand-nth ind/X-LIST)
        (= term 'y)
        ,(rand-nth ind/Y-LIST)  
        (symbol? term)
        ,`~term
        (number? term)
        ,term
        :else
        ,(expand term 1)))

(defn expand [exprs depth]
  (if (or (symbol? exprs)
          (number? exprs)
          (empty? exprs)
          (< depth 1))
    (atomize exprs)
    (cons (first exprs) 
          (loop [terms (rest exprs)
                 acc ()
                 expr-width (rand-int MAX-STARTING-WIDTH-OF-EXPR)]
            (if (empty? terms)
              acc
              (let [term (first terms)
                    exp (case term
                          (expr expr+)
                          ,(expand (rand-nth ind/FUNCTION-LIST)
                                   (dec depth))
                          expr?
                          ,(if (< (rand) EXPR?-RATE)
                             (expand (rand-nth ind/FUNCTION-LIST)
                                     (dec depth))
                             ())
                          point
                          ,(expand (rand-nth ind/POINT-LIST)
                                   (dec depth))
                          ,(atomize term))]
                (recur (if (and (= term 'expr+)
                                (pos? expr-width))
                         terms
                         (rest terms))
                       (concat acc (list exp))
                       (dec expr-width))))))))

(defn create-random-individual []
  (expand '(do expr+) (inc (rand-int MAX-STARTING-DEPTH))))

(defn create-random-population []
  (take SIZE-OF-POPULATION (repeatedly #(create-random-individual))))

(defn fitness-proportionate-selection [population]
  (let [F (reduce + (map :fitness population))
        r (rand)]
    (loop [pop population
           slice (/ (:fitness (first population)) F)]
      (if (<= r slice)
        (first pop)
        (recur (rest pop)
               (+ slice (/ (:fitness (second pop)) F)))))))

(defn compare-fitness [indiv-1 indiv-2]
  (cond (> (:fitness indiv-1) (:fitness indiv-2))
        ,-1
        (< (:fitness indiv-1) (:fitness indiv-2))
        ,1
        (> (count (flatten (:program indiv-1)))
           (count (flatten (:program indiv-2))))
        ,1
        :else
        ,-1))

(defn tournament-selection [tournament-size population]
  (first (sort compare-fitness (repeatedly tournament-size #(rand-nth population)))))

(defn selection [population]
  (case SELECTION
    fitness-proportionate (fitness-proportionate-selection population)
    tournament-selection (tournament-selection TOURNAMENT-SIZE population)))

(defn reproduction [parents]
  (letfn [(reproduce [parent-1 parent-2]
            (concat '(do)
                    (take (/ (count (rest parent-1)) 2) (rest parent-1))
                    (drop (/ (count (rest parent-2)) 2) (rest parent-2))))]
    (vector (reproduce (first parents) (second parents))
            (reproduce (second parents) (first parents)))))

(defn find-relevant-expr [loc]
  (let [l (zip/node (zip/leftmost loc))
        n (count (zip/lefts loc))
        expr (first (filter #(and (not (symbol? %))
                                  (= (first %) l))
                            ind/EXPR-LIST))
        c (if (= (second expr) 'expr+)
            'expr+
            (nth expr n))]
    (try
      (case c 
        (expr expr? expr+) (rand-nth ind/FUNCTION-LIST)
        pill (rand-nth ind/PILL-LIST)
        superpill (rand-nth ind/SUPERPILL-LIST)
        point (rand-nth ind/POINT-LIST)
        blue (rand-nth ind/BLUE-LIST)
        value (rand-nth ind/VALUE-LIST)
        radius (rand-nth ind/RADIUS-LIST)
        x (rand-nth ind/X-LIST)
        y (rand-nth ind/Y-LIST))
      (catch java.lang.IllegalArgumentException e
        (throw (java.lang.IllegalArgumentException.
                (format "\n###\nL: %s\nN: %s\nE: %s\nC: %s\n###" l n expr c) e))))))

(defn select-random-node [tree]
  (if-not (seq? tree)
    (zip/seq-zip tree)
    (loop [loc (zip/next (zip/next (zip/seq-zip tree)))
           val loc
           n 2] ;start at two as first one is set as val
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

(defn mutation [tree]
  (case (rand-nth ['replace 'remove 'insert])
    replace (let [original (select-random-node tree)
                  replacement (expand (find-relevant-expr original)
                                      (inc (rand-int MUTATION-DEPTH)))]
              (zip/root
               (zip/replace original replacement)))
    remove (conj (let [r (rest tree)
                       l (split-at (rand-int (count r)) r)]
                   (concat (first l) (rest (second l))))
                 (first tree))
    insert (zip/root (zip/insert-right (zip/down (zip/seq-zip tree))
                                       (expand (rand-nth ind/FUNCTION-LIST)
                                               (inc (rand-int MUTATION-DEPTH)))))))

(defn recombination [elitism population]
  (loop [indivs (- SIZE-OF-POPULATION elitism)
         r (rand)
         acc '()]
    (if (neg? indivs)
      acc
      (let [indiv (cond (< r REPRODUCTION-RATE)
                        ,(reproduction (repeatedly 2 #(:program (selection population))))
                        (< r (+ REPRODUCTION-RATE MUTATION-RATE))
                        ,(mutation (:program (selection population)))
                        :else
                        (:program (selection population)))]
        (recur (if (vector? indiv) ;;If this is the case then we have reproduction
                 (- indivs 2)      ;;and that means two new indivs
                 (dec indivs))
               (rand)               
               (if (vector? indiv)
                 (concat acc indiv)
                 (conj acc indiv)))))))

(defn calc-time [times]
  (let [mean (/ (reduce + times) SIZE-OF-POPULATION)
        std (Math/sqrt (/ (reduce + (map #(Math/pow % 2)
                                         (map #(- % mean) times)))
                SIZE-OF-POPULATION))
        time (long (+ mean (* 2 std)))]
    (if (< time 30000)
      30000
      time)))

(defn run-generation [generation]
  (use 'mspacman.individual)
  (let [new-time (calc-time (map :time generation))
        elitism (* SIZE-OF-POPULATION ELITISM-RATE)]
    (println "Time: " new-time)
    (sort compare-fitness
          (pmap #(let [[fitness time] (ind/fitness FITNESS-RUNS % new-time)]
                   (struct individual % fitness time))
                (concat (map :program
                             (take elitism generation))
                        (recombination elitism generation))))))

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
                (str {:population generation
                      :population-size SIZE-OF-POPULATION
                      :elitism ELITISM-RATE
                      :max-start-depth MAX-STARTING-DEPTH
                      :max-starting-width MAX-STARTING-WIDTH-OF-EXPR
                      :mutation MUTATION-RATE
                      :reproduction REPRODUCTION-RATE
                      :mutation-depth MUTATION-DEPTH
                      :rand-int RAND-INT-RATE
                      :expr? EXPR?-RATE
                      :fitness-runs FITNESS-RUNS
                      :selection SELECTION
                      :date STARTED}))
          (println (map :fitness generation)
                   "average:"
                   (int (/ (reduce + (map :fitness generation)) (count generation))))
          (recur (run-generation generation)
                 (inc n))))))

(defn gp-run
  ([]
     (println "Started")
     (use 'mspacman.individual)
     (gp-go (sort compare-fitness
                  (pmap #(let [[fitness time] (ind/fitness FITNESS-RUNS %1 (* 5 60 1000))]
                           (struct individual %1 fitness time))
                        (create-random-population)))
            0))
  ([gen-file nb-gen]
     (println (format "Started at generation %s." nb-gen))
     (gp-go (run-generation (:population (read-string (slurp gen-file))))
            (read-string nb-gen))))

(defn run-fitness-on [individuals]
  (use 'mspacman.individual)
  (println (format "Running %s individuals." (count individuals)))
  (let [run-time (:time (first individuals))]
    (sort compare-fitness
          (doall
           (pmap #(let [[fitness time]
                        (ind/fitness FITNESS-RUNS (:program %) run-time)]
                    (assoc (assoc % :fitness fitness) :time time))
                 individuals)))))