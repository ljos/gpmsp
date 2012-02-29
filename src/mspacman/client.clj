(ns mspacman.client
  (:import (java.net Socket InetAddress)
          (java.io InputStreamReader OutputStreamWriter)
          (clojure.lang LineNumberingPushbackReader))
  (:require [mspacman.gpmsp :as gp]
            [clojure.string :as string]))

(def ALL-MACHINES [;"mn121033"
                   "mn121034"	
                   "mn121035" "mn121036"
                   "mn121037"
                   ;"mn121038"
                   "mn121039" "mn121040"	
                   "mn121041" "mn121042"	
                   "mn121043" "mn121044"	
                   "mn121045" "mn121046"	
                   "mn121047" "mn121048"	
                   "mn121049" "mn121050"	
                   "mn121051" "mn121052"	
                   "mn121053" "mn121054"
                   "mn121055" "mn121056"	
                   "mn121057" "mn121058"
                   "mn121069" "mn121070"
                   "mn121071" "mn121072"
                   "mn121073" "mn121074"
                   "mn121075" "mn121077"
                   ;"mn121083"
                   "mn121084"
                   "mn121085"
                   ;"mn121086"	
                   "mn121087" "mn121088"
                   "mn121089" "mn121090"
                   "mn121091" "mn121092"
                   "mn121093" "mn121094"
                   "mn121095" "mn121096"
                   "mn121097" "mn121098"
                   "mn121099" "mn121100"
                   "mn121101" "mn121102"
                   "mn121103" "mn121104"
                   "mn121105" "mn121107"
                   "mn190142" "mn190143"
                   "mn190144" "mn190145"
                   "mn190146" 
                   "mn190148" "mn190149"
                   "mn190151" 
                   "mn190155"
                   "mn190156" "mn190157"
                   ])

(defn- find-useable-machines [machines]
  (letfn [(has-user? [machine]
            (try
              (let [socket (Socket. (format "%s.klientdrift.uib.no" machine) 50001)
                    rdr (LineNumberingPushbackReader.
                         (InputStreamReader.
                          (.getInputStream socket)))]
                (try
                  (when (zero? (read-string (.readLine rdr)))
                    machine)
                  (catch Exception e (println (.getMessage e)))
                  (finally
                   (when-not (.isClosed socket)
                     (doto socket
                       (.shutdownInput)
                      (.shutdownOutput)
                      (.close))))))
              (catch Exception e (println (.printStackTrace e)))))]
    (filter has-user? ALL-MACHINES)))

(defn- send-population [machines population]
  (let [out (doall 
             (pmap #(let [socket (.Socket (format "%s.klientdrift.uib.no" %1) 50000)
                          rdr (LineNumberingPushbackReader.
                               (InputStreamReader.
                                (.getInputStream socket)))
                          wtr (OutputStreamWriter.
                               (.getOutputStream socket))]
                      (try
                        (.write wtr (str %2) 0 (count (str %2)))
                        (.readLine rdr)
                        (finally
                         (when-not (.isClosed socket)
                           (doto socket
                             (.shutdownInput)
                             (.shutdownOutput)
                             (.close))))))
                   machines
                   (partition (int (/ gp/SIZE-OF-POPULATION
                                      (count machines)))
                              population)))]
    (println out)
    out))

(defn gp-over-cluster [population n]
  (let [machines  (find-useable-machines ALL-MACHINES)
        from-machines (send-population machines population)
        generation (sort-by :fitness > from-machines)]
    (newline)
    (println 'generation n)
    (spit (format "%s/generations/%s_generation_%tL.txt"
                  (System/getProperty "user.home")
                  (string/lower-case (.getHostName (InetAddress/getLocalHost)))
                  n)
          (str generation))
    (println (map #(:fitness %) generation)
             "average:"
             (int (/ (reduce + (map #(:fitness %) generation))
                     (count generation))))
    (newline)
    generation))

(defn- run-cluster [startp startn]
  (println "Started")
  (let [elitism (* gp/SIZE-OF-POPULATION gp/ELITISM-RATE)]
    (loop [population (gp-over-cluster startp 0)
           n (inc startn)]
      (if (< n gp/NUMBER-OF-GENERATIONS)
        (recur (gp-over-cluster (concat (take elitism population)
                                        (map #(struct gp/individual %  0)
                                             (gp/recombination elitism population)))
                                n)
               (inc n))
        (shutdown-agents)))))

(defn start-gp-cluster
  ([]
     (run-cluster (map #(struct gp/individual % 0)
                       (gp/create-random-population))
                  0))
  ([start-pop]
     (run-cluster (read-string (slurp start-pop))
                  0)))