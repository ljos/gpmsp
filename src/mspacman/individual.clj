(ns mspacman.individual
  (:require [clojure.tools.logging :only (info) :as log]))

(import '(no.uib.bjo013.mspacman NOAGame GfxMsPacman))
(import javax.swing.JFrame)
(import '(java.awt BorderLayout event.KeyEvent))

(def ^:dynamic msp nil)
(def ENTITY-LIST '(mspacman
                   blinky
                   pinky
                   inky
                   sue
                   closest-pill
                   closest-superpill
                   closest-blue
                   farthest-blue
                   farthest-superpill))
(def BOOLEAN-LIST '((msp-check-area-below entity)
                    (msp-check-area-above entity)
                    (msp-check-area-leftof entity)
                    (msp-check-area-rightof entity)
                    (or bool bool)
                    (and bool bool)))
(def FUNCTION-LIST '((if bool expr)
                     (if bool expr expr)
                     (move-down)
                     (move-up)
                     (move-left)
                     (move-right)
                     (direction-of entity)
                     (oposite-direction-of entity)
                     ()))
(def EXPR-LIST (concat BOOLEAN-LIST
                       FUNCTION-LIST))

(def mspacman 0)
(def blinky 1)
(def pinky 2)
(def inky 3)
(def sue 4)
(def closest-pill 5)
(def closest-superpill 6)
(def closest-blue 7)
(def farthest-blue 8)
(def farthest-superpill 9)

(defmacro msp-defn [name args & body]
  `(defn ~name ~args
     (try (try ~@body (catch NullPointerException f#
                        (.printStackTrace f#)))
       (catch Exception e#
         (.printStackTrace e#)
         (throw e#)))))

(defmacro get-map [f]
  `(~f (.getMap msp)))

(msp-defn move-down []
  KeyEvent/VK_DOWN)
(msp-defn move-up []
  KeyEvent/VK_UP)
(msp-defn move-left []
  KeyEvent/VK_LEFT)
(msp-defn move-right []
  KeyEvent/VK_RIGHT)

(msp-defn msp-check-area-below [entity]
  (if (number? entity)
    (.checkArea (.getMap msp) entity 0 1)))
(msp-defn msp-check-area-above [entity]
  (if (number? entity)
    (.checkArea (.getMap msp) entity 0 -1)))
(msp-defn msp-check-area-leftof [entity]
  (if (number? entity)
    (.checkArea (.getMap msp) entity -1 0)))
(msp-defn msp-check-area-rightof [entity]
  (if (number? entity)
    (.checkArea (.getMap msp) entity 1 0)))

(msp-defn direction-of [entity]
  (if (number? entity)
    (.directionOf (.getMap msp) entity)))

(msp-defn oposite-direction-of [entity]
  (if (number? entity)
    (.opositeDirectionOf (.getMap msp)  entity)))

(defn set-direction! [direction]
  (if (number? direction)
    (.setDirection msp direction)))

(defn fitness [tries code time]
  (binding [msp (NOAGame. time)]
    (loop [[score time] [0 0]
           times 0] 
      (if (or (<= tries times)
              (and (<= 3 times)
                   (= (/ score times) 120)))
        [(int (/ score times))
         (long (/ time times))]
        (do (.start msp)
            (.update msp)
            (recur
             (do (while (not (.isGameOver msp))
                   (set-direction! (eval code))
                   (.update msp))
                 [(+ (.getScore msp) score)
                  (+ (.getTime msp) time)])
             (inc times)))))))

(defn fitness-graphic [tries code time]
  (binding [msp (NOAGame. time)]
    (let [gfx (doto (GfxMsPacman. (.initialize msp))
                (.setSize 224 (+ 288 22)))
          frame (doto (JFrame.)
                  (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
                  (.setSize 224 (+ 288 22))
                  (.setLocation 100 0)
                  (-> .getContentPane
                      (.add gfx BorderLayout/CENTER))
                  (.setVisible Boolean/TRUE))
          thread (Thread. gfx)]
      (.start thread)
      (loop [[score time] [0 0]
             times 0]
        (if (or (<= tries times)
                (and (<= 3 times)
                     (= (/ score times) 120)))
          (do (.dispose frame)
              (.stop gfx)
              (locking gfx
                (.notify gfx))
              (.join thread)
              [(int (/ score times))
               (long (/ time times))])
          (do (.setBitmap gfx (.start msp))
              (recur 
               (do (while (and (not (.isGameOver msp)))
                     (set-direction! (eval code))
                     (.setBitmap gfx (.update msp))
                     (locking gfx
                       (.notify gfx)))
                   [(+ (.getScore msp) score)
                    (+ (.getTime msp) time)])
                     (inc times))))))))