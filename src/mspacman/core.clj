(ns mspacman.core
  (:require [mspacman.gpmsp :as gp]
            [mspacman.individual :as ind]))

(defn -main [& args]
  (cond (empty? args)
        ,(gp/gp-run)
        :else
        (gp/gp-run (first args)
                   (second args))))