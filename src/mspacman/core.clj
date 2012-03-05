(ns mspacman.core
  (:require [mspacman.gpmsp :as gp]
            [mspacman.individual :as ind]
            [mspacman.client :as client]
            [mspacman.server :as server])
  (:use [mspacman.client :as client]))

(defn -main [& args]
  (cond (empty? args)
        ,(gp/gp-run)
        :else
        (gp/gp-run (first args)
                   (second args))))