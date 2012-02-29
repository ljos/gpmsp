(ns mspacman.server
  (:require [clojure.java.shell :as shell]
            [clojure.contrib.server-socket :as socket]
            [mspacman.gpmsp :as gp])
  (:import (clojure.lang LineNumberingPushbackReader)
           (java.io InputStreamReader OutputStreamWriter PrintWriter OutputStream)))

(defn- run-fitness [ins outs]
  (println "Run-fitness")
  (binding [*out* (OutputStreamWriter. outs)
            *err* (PrintWriter. ^OutputStream outs true)]
    (let [rdr (LineNumberingPushbackReader. (InputStreamReader. ins))
          inds (.readLine rdr)]
      (prn (gp/run-fitness-on inds)))))

(defn- test-server [ins outs]
  (println "Test-server")
  (println
   (binding [*out* (OutputStreamWriter. outs)
             *err* (PrintWriter. ^OutputStream outs true)]
     (let [out (:exit (shell/sh "check_for_user"))]
       (prn out)
       out))))

(defn start-server []
  [(socket/create-server 50000 run-fitness)
   (socket/create-server 50001 test-server)])