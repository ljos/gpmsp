(ns mspacman.individual
  (use [clojure.inspector :include (atom?)]))

(import '(no.uib.bjo013.mspacman MsPacman NUIMsPacman GUIMsPacman))
(import javax.swing.JFrame)
(import java.awt.BorderLayout)
(import java.awt.event.KeyEvent)
(import java.util.concurrent.CountDownLatch)
(import java.util.concurrent.TimeUnit)

(def ^:dynamic msp nil)

(def ENTITY-LIST '(mspacman
                   blinky
                   pinky
                   inky
                   sue))

(def ITEM-LIST (concat
                '(pills)
                ENTITY-LIST))

(def ATOM-LIST (concat
                '(move-left
                  move-right
                  move-up
                  move-down
                  (msp-sleep)
                  int)
                ITEM-LIST
                ENTITY-LIST))

(def FUNCTION-LIST (concat
                    '((do expr+)
                      (msp-relative-distance entity item)
                      (msp-check-area-below entity)
                      (msp-check-area-above entity)
                      (msp-check-area-leftof entity)
                      (msp-check-area-rightof entity)
                      (if expr expr expr?)
                      (= expr expr)
                      (msp- expr expr)
                      (msp+ expr expr)
                      (msp> expr expr)
                      (msp< expr expr)
                      (or expr expr)
                      (and expr expr))))

(def mspacman (atom {:name 'mspacman :colour 16776960}))
(def blinky (atom {:name 'blinky :colour 16711680}))
(def pinky (atom {:name 'pinky :colour 16759006}))
(def inky  (atom {:name 'inky :colour 65502}))
(def sue (atom {:name 'sue :colour 16758855}))
(def pills (atom {:name 'pills :colour 14606046}))
(def walkway (atom {::name 'walkway :colour 0}))

(def move-left 'move-left)
(def move-right 'move-right)
(def move-up 'move-up)
(def move-down 'move-down)

(defn msp> [& keys]
  (let [l (remove #(not (instance? Number %))
                  keys)]
    (if (empty? l)
      true
      (apply > l))))

(defn msp< [& keys]
  (let [l (remove #(not (instance? Number %))
                  keys)]
    (if (empty? l)
      true
      (apply > l))))

(defn msp+ [& keys]
  (let [l (remove #(not (instance? Number %))
                  keys)]
    (if (empty? l)
      0
      (apply + l))))

(defn msp- [& keys]
  (let [l (remove #(not (instance? Number %))
                  keys)]
    (if (empty? l)
      0
      (apply - l))))

(defn msp-sleep []
  (Thread/sleep 10))

(defn msp-move-left []
  (-> msp (.keyPressed KeyEvent/VK_LEFT)))

(defn msp-move-right []
  (-> msp (.keyPressed KeyEvent/VK_RIGHT)))

(defn msp-move-up []
  (-> msp (.keyPressed KeyEvent/VK_UP)))

(defn msp-move-down []
  (-> msp (.keyPressed KeyEvent/VK_DOWN)))

(defn msp-check-area-leftof [entity]
  (when (and (= (type entity) clojure.lang.Atom)
             (some #(= (:name @entity) %) ENTITY-LIST))
    (let [xy (-> msp (.getEntity (:colour @entity)))]
      (-> msp (.checkForEntity (:colour @entity) 0 (first xy) (second xy))))))

(defn msp-check-area-above [entity]
  (when (and (= (type entity) clojure.lang.Atom)
             (some #(= (:name @entity) %) ENTITY-LIST))
    (let [xy (-> msp (.getEntity (:colour @entity)))]
      (-> msp (.checkForEntity (:colour @entity) 1 (first xy) (second xy))))))

(defn msp-check-area-rightof [entity]
  (when (and (= (type entity) clojure.lang.Atom)
             (some #(= (:name @entity) %) ENTITY-LIST))
    (let [xy (-> msp (.getEntity (:colour @entity)))]
      (-> msp (.checkForEntity (:colour @entity) 2 (first xy) (second xy))))))

(defn msp-check-area-below [entity]
  (when (and (= (type entity) clojure.lang.Atom)
             (some #(= (:name @entity) %) ENTITY-LIST))
    (let [xy (-> msp (.getEntity (:colour @entity)))]
      (-> msp (.checkForEntity (:colour @entity) 3 (first xy) (second xy))))))

(defn msp-relative-distance [entity item]
  (when (and (= (type entity) clojure.lang.Atom)
             (= (type item) clojure.lang.Atom)
             (some #(= % (:name @entity)) ENTITY-LIST)
             (some #(= % (:name @item)) ITEM-LIST))
    (let [k (keyword (:name @item))]
      (k (swap! entity
                assoc
                k
                (-> msp
                    (.relativeDistance (:colour @entity)
                                           (:colour @item))))))))

(defn msp-closer? [entity item]
  (when (and (= (type entity) clojure.lang.Atom)
             (= (type item) clojure.lang.Atom)
             (some #(= % (:name @entity)) ENTITY-LIST)
             (some #(= % (:name @item)) ITEM-LIST))
    (let [k (keyword (:name @item))
          prev-d (k @entity)
          new-d (msp-relative-distance entity item)]
      (or (nil? (k entity))
          (< prev-d new-d)))))

(defn move-in-direction [direction]
  (case direction
    move-left (msp-move-left)
    move-right (msp-move-right)
    move-up (msp-move-up)
    move-down (msp-move-down)
    ())
  direction)

(defn fitness [tries code]
  (let [signal (into-array
                (repeatedly (inc tries)
                            #(new CountDownLatch 1)))]
    (binding [msp (new NUIMsPacman signal)]
      (let [thread (new Thread msp)]
        (.start thread)
        (loop [score 0
               times 0]
          (if (or (<= tries times)
                  (and (<= 3 times)
                       (= (/ score times) 120)))
            (do (locking msp
                  (.stopMSP msp))
                (.join thread)                
                (int (/ score times)))
            (do (.await (nth signal times))
                (recur (+ score
                          (do (while (not (.isGameOver msp))
                                (move-in-direction
                                 (eval `~code)))
                              (.getScore msp)))
                       (inc times)))))))))

(defn fitness-graphic [tries code]
  (binding [msp (doto (new GUIMsPacman)
                  (.setSize 224 (+ 288 22)))]
    (let [frame (doto (new JFrame)
                  (.setDefaultCloseOperation
                   javax.swing.JFrame/EXIT_ON_CLOSE)
                  (.setSize 224 (+ 288 22))
                  (.setLocation 100 0)
                  (-> .getContentPane
                      (.add msp java.awt.BorderLayout/CENTER))
                  (.setVisible Boolean/TRUE))]
      (do (-> (new Thread msp) .start)
          (Thread/sleep 7000)
          (loop [score 0
                 t tries]
            (if (zero? t)
              (do (-> msp  (.stop true))
                  (-> frame .dispose)
                  (int (/ score tries)))
              (recur (+ score
                        (do (-> msp (.keyPressed KeyEvent/VK_5))
                            (Thread/sleep 100)
                            (-> msp (.keyReleased KeyEvent/VK_5))
                            (Thread/sleep 100)
                            (-> msp (.keyPressed KeyEvent/VK_1))
                            (Thread/sleep 100)
                            (-> msp (.keyReleased KeyEvent/VK_1))
                            (Thread/sleep 500)
                            (while (not (-> msp .isGameOver))
                              (move-in-direction (eval `~code)))
                            (Thread/sleep 1000)
                            (-> msp .getScore)))
                     (dec t))))))))