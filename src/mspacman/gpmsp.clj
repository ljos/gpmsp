(ns mspacman.gpmsp)

(import '(no.uib.bjo013.mspacman MsPacman NUIMsPacman))
(import java.awt.event.KeyEvent)
(import java.lang.Boolean)

(defstruct individual
  :program
  :mspacman
  :fitness
  :finishing-time)



(defn create-random-population []
  (dotimes [@population-size n]
    ()))


(defn -main [& args]
  ())