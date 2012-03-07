(ns mspacman.individual
  (use [clojure.inspector :include (atom?)]))

(import '(no.uib.bjo013.mspacman MsPacman NUIMsPacman GUIMsPacman))
(import javax.swing.JFrame)
(import java.awt.BorderLayout)
(import java.awt.event.KeyEvent)
(import '(java.util.concurrent CountDownLatch TimeUnit Semaphore))


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
                  int)
                ITEM-LIST))

(def BOOLEAN-LIST '((msp-check-area-below entity)
                    (msp-check-area-above entity)
                    (msp-check-area-leftof entity)
                    (msp-check-area-rightof entity)
                    (msp> expr expr)
                    (msp< expr expr)
                    (or expr expr)
                    (and expr expr)
                    (= expr expr)
                    (msp-closer? entity item)))

(def FUNCTION-LIST (concat
                    '((msp-relative-distance entity item)
                      (if boolean expr expr?)
                      (msp- expr expr)
                      (msp+ expr expr))
                    BOOLEAN-LIST))

(def mspacman (atom {:name 'mspacman :colour 16776960}))
(def blinky (atom {:name 'blinky :colour 16711680}))
(def pinky (atom {:name 'pinky :colour 16759006}))
(def inky  (atom {:name 'inky :colour 65502}))
(def sue (atom {:name 'sue :colour 16758855}))
(def pills (atom {:name 'pills :colour 14606046}))
(def walkway (atom {:name 'walkway :colour 0}))

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
      (or (nil? prev-d)
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
                            #(new CountDownLatch 1)))
        lock (new Object)]
    (binding [msp (new NUIMsPacman signal lock (Thread/currentThread))]
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
                          (do (while (and (not (.isGameOver msp)) (.shouldContinue msp))
                                    (while (not= (.toString (.getState thread)) "WAITING"))
                                    (locking lock
                                      (let [start (. System (nanoTime))]
                                       (move-in-direction (eval `~code))
                                       (if (< 250 (/ (double (- (. System (nanoTime)) start))
                                                     1000000.0))
                                         (.stopMSP msp)))
                                      (.notify lock)
                                      (.wait lock))) 
                                  (.getScore msp)))
                       (inc times)))))))))

(defn fitness-graphic [tries code]
  (let [signal (into-array
                (repeatedly (inc tries)
                            #(new CountDownLatch 1)))
        lock (new Object)]
    (binding [msp (doto (new GUIMsPacman signal lock (Thread/currentThread))
                    (.setSize 224 (+ 288 22)))]
      (let [frame (doto (new JFrame)
                    (.setDefaultCloseOperation
                     javax.swing.JFrame/EXIT_ON_CLOSE)
                    (.setSize 224 (+ 288 22))
                    (.setLocation 100 0)
                    (-> .getContentPane
                        (.add msp java.awt.BorderLayout/CENTER))
                    (.setVisible Boolean/TRUE))
            thread (new Thread msp)]
        (do (.start thread)
            (loop [score 0
                   times 0]
              (if (or (<= tries times)
                      (and (<= 3 times)
                           (= (/ score times) 120)))
                (do (locking msp
                      (.stopMSP msp))
                    (.join thread)
                    (.dispose frame)
                    (int (/ score tries)))
                (do (.await (nth signal times))
                    (recur (+ score
                              (do (while (and (not (.isGameOver msp)) (.shouldContinue msp))
                                    (while (not= (.toString (.getState thread)) "WAITING"))
                                    (locking lock
                                      (let [start (. System (nanoTime))]
                                       (move-in-direction (eval `~code))
                                       (if (< 250 (/ (double (- (. System (nanoTime)) start))
                                                     1000000.0))
                                         (.stopMSP msp)))
                                      (.notify lock)
                                      (.wait lock))) 
                                  (.getScore msp)))
                        (inc times))))))))))