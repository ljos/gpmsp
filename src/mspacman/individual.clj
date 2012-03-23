(ns mspacman.individual
  (:require [clojure.data.priority-map :as pm]))

(import '(no.uib.bjo013.mspacman Game GfxMsPacman))
(import javax.swing.JFrame)
(import '(java.awt BorderLayout Point))

(def ^:dynamic msp nil)

(def VALUE-LIST (concat (for [x (range 0 11)]
                          (Math/pow 10 x))
                        (list Double/MAX_VALUE)))
(def RADIUS-LIST (range 0 288))
(def X-LIST (range -224 224))
(def Y-LIST (range -288 288))
(def FUNCTION-LIST '((adjust-point point value)
                     (adjust-circle point radius value)))
(def PILL-LIST (range 0 220))
(def SUPERPILL-LIST (range 0 4))
(def BLUE-LIST (range 0 4))
(def POINT-LIST '((get-mspacman)
                  (get-blinky)
                  (get-pinky)
                  (get-inky)
                  (get-sue)
                  (get-pill pill)
                  (get-superpill superpill)
                  (get-blue blue)
                  (translate-point point x y)))
(def EXPR-LIST (concat '((do expr+))
                       FUNCTION-LIST
                       POINT-LIST))


(defmacro msp-defn [name args & body]
  `(defn ~name ~args
     (try (try ~@body (catch NullPointerException f#))
       (catch Exception e#
         (println (format "%s threw exception: " ~name))
         (throw e#)))))

(defmacro get-map [f]
  `(~f (.getMap msp)))

(msp-defn get-mspacman []
  (get-map .getMsPacman))

(msp-defn get-blinky []
  (get-map .getBlinky))

(msp-defn get-pinky []
  (get-map .getPinky))

(msp-defn get-inky []
  (get-map .getInky))

(msp-defn get-sue []
  (get-map .getSue))

(msp-defn get-pill [n]
  (.get (get-map .getPills) (Integer. n)))

(msp-defn get-superpill [n]
  (.get (get-map .getSuperPills) (Integer. n)))

(msp-defn get-blue [n]
  (try (.get (get-map .getBlueGhosts) 0)
       (catch IndexOutOfBoundsException e)))


(msp-defn set-target [^Point point]
  {:pre [(= Point (type point))]}
  (.setTarget msp point))

(msp-defn adjust-point [point n]
  (when (= Point (type point))
    (.adjustScore msp point (double n))))

(msp-defn adjust-circle [origin radius value]
  (when (= Point (type origin))
    (.adjustCircle msp origin radius (double value))))

(msp-defn translate-point [^Point point ^Number x ^Number y]
  (when (= Point (type point))
    (Point. (+ x (.x point)) (+ y (.x point)))))

(msp-defn path-distance [^Point p]
           (.size (.calculatePath (.getMap msp) p)))

(msp-defn distance [^Point p]
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
            (.update msp)
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