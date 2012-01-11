(ns mspacman.core
  (:require [mspacman.gpmsp :as gp]
            [mspacman.individual :as ind]))

(defn -main [& args]
  (cond (empty? args)
        ,(gp/gp-run)
        (= 1 (count args))
        ,(gp/contrl)
        :else
        (gp/gp-run (first args)
                   (second args))))