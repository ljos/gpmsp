(ns mspacman.gpmsp)

(import '(no.uib.bjo013.mspacman MsPacman NUIMsPacman))
(import java.awt.event.KeyEvent)
(import java.lang.Boolean)

(defstruct individual
  :program
  :mspacman
  :fitness
  :finishing-time)

(def population-size  (atom 30))
(def fitness-tries (atom 5))

(defn fitness [code msp]
  (let [t (new Thread msp)]
    (-> t .start)
    (Thread/sleep 6000)
    (loop [n @fitness-tries]
      (if (< n 1)
        nil
        (while (not (-> msp isGameOver))
          (eval code))
        (recur (dec n))))
    (let [fitness-score (-> msp .getScore)]
      (-> msp .stop Boolean/TRUE)
      fitness-score)))

(defn create-random-population []
  (dotimes [@population-size n]
    ()))