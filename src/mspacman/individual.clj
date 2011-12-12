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
                                        (.setDefaultCloseOperation javax.swing.JFrame/EXIT_ON_CLOSE)
                                        (.setSize 224 (+ 288 22))
                                        (.setLocation 100 0)
                                        (-> .getContentPane (.add ~'msp java.awt.BorderLayout/CENTER))
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
                                 fitness-score#))))))
             (dec t)))))

(def INT-LIST '(mspacman
                   blinky
                   pinky
                   inky
                   sue
                   pills
                   walkway
                   wall1
                   wall2
                   wall3
                   wall4
                   wall5
                   wall6
                   wall7
                   wall8
                   (x)
                   (y)
                   (get-pixelxy)
                   (msp-rand-int)))

(def ATOM-LIST (concat
                '((move-left)
                  (move-right)
                  (move-up)
                  (move-down)
                  (msp-sleep)
                  ())
                INT-LIST))

(def FUNCTION-LIST (concat
                    '((do expr+)
                      (get-pixel int int)
                      (find-colour int)
                      (if expr expr expr?)
                      (= expr+)
                      (msp> expr+)
                      (msp< expr+)
                      (msp+ expr+)
                      (msp- expr+)
                      (or expr+)
                      (and expr+))
                    ATOM-LIST))

(def x1 (ref 0))
(defn x [] @x1)

(def y1 (ref 0))
(defn y [] @y1)

(def mspacman 16776960)
(def blinky 16711680)
(def pinky 16759006)
(def inky  65502)
(def sue 16758855)
(def pills 14606046)
(def walkway 0)
(def wall1  14587719)
(def wall2 16759006)
(def wall3 4700382)
(def wall4 2171358)
(def wall5 65280)
(def wall6 4700311)
(def wall7 16758935)
(def wall8 14606046)

(defn msp-rand-int []
  (rand-int 10000))

(defn msp> [& keys]
  (let [l (remove #(not (instance? Number %1))
                  keys)]
    (if (empty? l)
      true
      (apply > l))))


(defn msp< [& keys]
  (let [l (remove #(not (instance? Number %1))
                  keys)]
    (if (empty? l)
      true
      (apply > l))))

(defn msp+ [& keys]
  (let [l (remove #(not (instance? Number %1))
                  keys)]
    (if (empty? l)
      0
      (apply + l))))

(defn msp- [& keys]
  (let [l (remove #(not (instance? Number %1))
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

(defn get-pixel [i j]
  (-> msp (.getPixel  (if (number? i)
                        (mod (int i) 224)
                        (rand-int 224))
                      (if (number? j)
                        (mod (int j) 288)
                        (rand-int 288)))))

(defn get-pixels []
  (-> msp .getPixels))

(defn get-pixelxy []
  (get-pixel @x1 @y1))

(defn find-colour [c]
  (loop [i 0]
    (if (> i 224)
      (do (loop [j 0]
            (cond (> j 288)
                  ,(dosync (ref-set x1 -1)
                           (ref-set y1 -1))
                  (= (get-pixel i j) c)
                  ,(dosync (ref-set x1 i)
                           (ref-set y1 j))
                  :else
                  ,(recur (inc j))))
         (recur (inc i))))))