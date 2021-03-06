(ns mspacman.individual
  (:require [clojure.tools.logging :only (info) :as log]))

(import '(no.uib.bjo013.mspacman Game GfxMsPacman))
(import javax.swing.JFrame)
(import '(java.awt BorderLayout Point))

(def ^:dynamic msp nil)

(def VALUE-LIST (concat [100 1000 10000 100000 1000000 10000000 100000000
                         1000000000 Integer/MAX_VALUE]
                        [-100 -1000 -10000 -100000 -1000000 -10000000 -100000000
                         -1000000000 Integer/MIN_VALUE]
                        (range -100 100)))
(def RADIUS-LIST (range 0 288))
(def X-LIST (range -224 224))
(def Y-LIST (range -288 288))
(def FUNCTION-LIST '((adjust-point point value)
                     (adjust-neighbor point radius value)))
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

(msp-defn adjust-neighbor [origin radius value]
  (when (= Point (type origin))
    (.adjustNeighbors msp origin radius (double value))))

(msp-defn translate-point [^Point point ^Number x ^Number y]
  (when (= Point (type point))
    (Point. (+ x (.x point)) (+ y (.x point)))))

(defn fitness [tries code time]
  (binding [msp (Game. time)]
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
                   (eval`~code)
                   (.update msp))
                 [(+ (.getScore msp) score)
                  (+ (.getTime msp) time)])
             (inc times)))))))

(defn fitness-graphic [tries code time]
  (binding [msp (Game. time)]
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
                     (eval `~code)
                     (.setBitmap gfx (.update msp))
                     (locking gfx
                       (.notify gfx)))
                   [(+ (.getScore msp) score)
                    (+ (.getTime msp) time)])
                     (inc times))))))))