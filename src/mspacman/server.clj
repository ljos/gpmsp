(ns mspacman.server
  (:require [clojure.java.shell :as shell]
            [mspacman.serversocket :as socket]
            [mspacman.gpmsp :as gp])
  (:import (clojure.lang LineNumberingPushbackReader)
           (java.io InputStreamReader OutputStreamWriter PrintWriter OutputStream)))

(defn- run-fitness [ins outs]
  (println "Run-fitness")
  (let [rdr (LineNumberingPushbackReader. (InputStreamReader. ins))
        inds (read-string (.readLine rdr))
        out (time (gp/run-fitness-on inds))]
    (println "finished")
    (binding [*out* (OutputStreamWriter. outs)]
      (prn out))))

(defn- test-server [ins outs]
  (println "Test-server")
  (println
   (binding [*out* (OutputStreamWriter. outs)]
     (let [user-on-machine (:exit (shell/sh "check_for_user"))]
       (prn user-on-machine)
       user-on-machine))))

(defn start-server []
  [(socket/create-server 50000 run-fitness)
   (socket/create-server 50001 test-server)])