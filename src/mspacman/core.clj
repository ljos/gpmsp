(ns mspacman.core
  (:require [mspacman.gpmsp :as gp]
            [mspacman.individual :as ind]))

(defn -main [& args]
  (if (empty? args)
    (gp/gp-run)
    (gp/gp-run (first args)
               (second args))))