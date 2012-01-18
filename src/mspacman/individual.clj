(ns mspacman.individual)

(import '(no.uib.bjo013.mspacman MsPacman NUIMsPacman GUIMsPacman))
(import javax.swing.JFrame)
(import java.awt.BorderLayout)
(import java.awt.event.KeyEvent)
(import java.util.concurrent.CountDownLatch)

(def ^:dynamic msp nil)

(defn fitness [tries code]
  "Tests fitness of MsPacman bot."
  (loop [score 0
         t tries]
    (cond (= t 0)
          ,(int (/ score tries))
          (and (= tries 3) (= (/ score tries) 120))
          ,score
          :else
          ,(recur (+ score
                    (eval `(let [signal# (new CountDownLatch 1)]
                             (binding [~'msp (new NUIMsPacman signal#)]
                               (do (-> (new Thread ~'msp) .start)
                                   (-> signal# .await)
                                   (-> ~'msp (.keyPressed KeyEvent/VK_5))
                                   (Thread/sleep 200)
                                   (-> ~'msp (.keyReleased KeyEvent/VK_5))
                                   (Thread/sleep 200)
                                   (-> ~'msp (.keyPressed KeyEvent/VK_1))
                                   (Thread/sleep 200)
                                   (-> ~'msp (.keyReleased KeyEvent/VK_1))
                                   (Thread/sleep 700)
                                   (while (not (-> ~'msp .isGameOver))
                                     ~code)
                                   (let [fitness-score# (-> ~'msp .getScore)]
                                     (-> ~'msp  (.stop true))
                                     fitness-score#))))))
                 (dec t)))))

(defn fitness-graphic [tries code]
 (loop [score 0
         t tries]
    (if (= t 0)
      (int (/ score tries))
      (recur (+ score
                (eval `(binding [~'msp (doto (new GUIMsPacman)
                                         (.setSize 224 (+ 288 22)))]
                         (let [frame# (doto (new JFrame)
                                        (.setDefaultCloseOperation
                                         javax.swing.JFrame/EXIT_ON_CLOSE)
                                        (.setSize 224 (+ 288 22))
                                        (.setLocation 100 0)
                                        (-> .getContentPane
                                            (.add ~'msp java.awt.BorderLayout/CENTER))
                                        (.setVisible Boolean/TRUE))
                               t# (new Thread ~'msp)]
                           (do (-> t# .start)
                               (Thread/sleep 7000)
                               (-> ~'msp (.keyPressed KeyEvent/VK_5))
                               (Thread/sleep 100)
                               (-> ~'msp (.keyReleased KeyEvent/VK_5))
                               (Thread/sleep 100)
                               (-> ~'msp (.keyPressed KeyEvent/VK_1))
                               (Thread/sleep 100)
                               (-> ~'msp (.keyReleased KeyEvent/VK_1))
                               (Thread/sleep 500)
                               (while (not (-> ~'msp .isGameOver))
                                 ~code)
                               (let [fitness-score# (-> ~'msp .getScore)]
                                 (-> ~'msp  (.stop true))
                                 (-> frame# .dispose)
                                 (println fitness-score#)
                                 fitness-score#))))))
             (dec t)))))

(def ENTITY-LIST '(mspacman
                   blinky
                   pinky
                   inky
                   sue))

(def ITEM-LIST (concat
                '(pills)
                ENTITY-LIST))

(def ATOM-LIST (concat
                '((move-left)
                  (move-right)
                  (move-up)
                  (move-down)
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
                      (and expr expr))
                    ATOM-LIST))

(def mspacman (atom {:name 'mspacman :colour 16776960}))
(def blinky (atom {:name 'blinky :colour 16711680}))
(def pinky (atom {:name 'pinky :colour 16759006}))
(def inky  (atom {:name 'inky :colour 65502}))
(def sue (atom {:name 'sue :colour 16758855}))
(def pills (atom {:name 'pills :colour 14606046}))
(def walkway (atom {::name 'walkway :colour 0}))


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

(defn move-left []
  (do (-> msp (.keyPressed KeyEvent/VK_LEFT))
      (Thread/sleep 50)
      (-> msp (.keyReleased KeyEvent/VK_LEFT))))

(defn move-right []
  (do (-> msp (.keyPressed KeyEvent/VK_RIGHT))
      (Thread/sleep 50)
      (-> msp (.keyReleased KeyEvent/VK_RIGHT))))

(defn move-up []
  (do (-> msp (.keyPressed KeyEvent/VK_UP))
      (Thread/sleep 50)
      (-> msp (.keyReleased KeyEvent/VK_UP))))

(defn move-down []
  (do (-> msp (.keyPressed KeyEvent/VK_DOWN))
       (Thread/sleep 50)
       (-> msp (.keyReleased KeyEvent/VK_DOWN))))

(defn msp-check-area-leftof
  ([entity]
     (-> msp (.checkForGhostLeft (:colour @entity)))))

(defn msp-check-area-rightof
  ([entity]
     (-> msp (.checkForGhostRight (:colour @entity)))))

(defn msp-check-area-above
  ([entity]
     (-> msp (.checkForGhostTop (:colour @entity)))))

(defn msp-check-area-below
  ([entity]
     (-> msp (.checkForGhostDown (:colour @entity)))))

(defn msp-relative-distance [entity item]
  (swap! entity
         assoc (keyword (:name @item))
         (-> msp (.relativeDistance (:colour @entity) (:colour @item)))))

(defn msp-closer? [entity item]
  (let [k (keyword (:name @item))
        prev-d (k @entity)
        new-d (msp-relative-distance entity item)]
    (or (nil? (k entity))
        (< prev-d new-d))))


