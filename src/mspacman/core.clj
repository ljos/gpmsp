(ns mspacman.core
  (:require [mspacman.gpmsp :as gp]
            [mspacman.individual :as ind]))

(defn -main [& args]
  (gp/gp-run))