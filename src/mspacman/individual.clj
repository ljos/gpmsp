(ns mspacman.individual
  (:require [clojure.data.priority-map :as pm]))

(import '(no.uib.bjo013.mspacman Game GfxMsPacman))
(import javax.swing.JFrame)
(import '(java.awt BorderLayout Point))

(def ^:dynamic msp nil)

(def ENTITY-LIST nil)
(def VALUE-LIST (for [x (range 0 11)]
                  (Math/pow 10 x)))
(def RADIUS-LIST (range 0 288))
(def FUNCTION-LIST '((adjust-point point value)
                     (adjust-circle point radius value)))
(def ITEM-LIST nil)
(def BOOL-LIST nil)
(def ATOM-LIST nil)
(def PILL-LIST (range 0 220))
(def SUPERPILL-LIST (range 0 4))
(def BLUE-LIST)
(def POINT-LIST '((get-mspacman)
                  (get-blinky)
                  (get-pinky)
                  (get-inky)
                  (get-sue)
                  (get-pill pill)
                  (get-superpill superpill)
                  (get-blue blue)
                  (translate-point point)))

(defmacro get-map [f]
  `(~f (.getMap msp)))

(defn get-mspacman []
  (get-map .getMsPacman))

(defn get-blinky []
  (get-map .getBlinky))

(defn get-pinky []
  (get-map .getPinky))

(defn get-inky []
  (get-map .getInky))

(defn get-sue []
  (get-map .getSue))

(defn get-pill [n]
  (.get (get-map .getPills) (Integer. n)))

(defn get-superpill [n]
  (.get (get-map .getSuperPills) (Integer. n)))

(defn get-blue [n]
  (try (.get (get-map .getBlueGhosts) 0)
       (catch IndexOutOfBoundsException e)))


(defn set-target [^Point point]
  {:pre [(= Point (type point))]}
  (.setTarget msp point))

(defn adjust-point [point n]
  {:pre [(= Point (type point))]}
  (.adjustScore msp point n))

(defn adjust-circle [origin radius value]
  (.adjustScore msp orogin radius value))

(defn translate-point [^Point point ^Number x ^Number y]
  {:pre [(= Point (type point))
         (number? x)
         (number? y)]
   :post [(= Point (type %))]}
  (Point. (+ x (.x point)) (+ y (.x point))))

(defn- path-distance [^Point p]
  (.size (.calculatePath (.getMap msp) p)))

(defn- distance [^Point p]
  (let [m (.getMsPacman (.getMap msp))]
    (.distance p m)))

(defn fitness [tries code]
  (binding [msp (Game.)]
    (loop [score 0
           times 0]
      (if (or (<= tries times)
              (and (<= 3 times)
                   (= (/ score times) 120)))
        (int (/ score times))
        (do (.start msp)
            (recur (+ score
                      (do (while (not (.isGameOver msp))
                            (eval`~code)
                            (.update msp))
                          (.getScore msp)))
                   (inc times)))))))

(defn fitness-graphic [tries code]
  (binding [msp (Game.)]
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
      (loop [score 0
             times 0]
        (if (or (<= tries times)
                (and (<= 3 times)
                     (= (/ score times) 120)))
          (do (.dispose frame)
              (.stop gfx)
              (locking gfx
                (.notify gfx))
              (.join thread)
              (int (/ score tries)))
          (do (.setBitmap gfx (.start msp))
              (recur (+ score
                        (do (while (and (not (.isGameOver msp)))
                              (eval `~code)
                              (.setBitmap gfx (.update msp))
                              (locking gfx
                                (.notify gfx)))
                            (.getScore msp)))
                     (inc times))))))))