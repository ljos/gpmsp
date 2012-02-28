(ns mspacman.server
  (:require [clojure.java.shell :as shell]
            [clojure.contrib.server-socket :as socket]
            [mspacman.gpmsp :as gp])
  (:import (clojure.lang LineNumberingPushbackReader)
           (java.io InputStreamReader OutputStreamWriter PrintWriter)))

(defn- run-fitness [ins outs]
  (binding [rdr (LineNumberingPushbackReader. (InputStreamReader. ins))
            *out* (OutputStreamWriter. outs)
            *err* (PrintWriter. ^OutputStream outs true)]
    (let [inds (.getLine rdr)]
      (prn (gp/run-fitness-on inds)))))

(defn- test-server [ins outs]
  (binding [*out* (OutputStreamWriter. outs)
            *err* (PrintWriter. ^OutputStream outs true)]
    (prn (:exit (shell/sh "check_for_user")))))

(defn start-server []
  (create-server 50000 run-fitness)
  (create-server 50001 test-server))