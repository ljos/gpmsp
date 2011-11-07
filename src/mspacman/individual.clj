(ns mspacman.individual)

(import '(no.uib.bjo013.mspacman MsPacman NUIMsPacman GUIMsPacman))
(import javax.swing.JFrame)
(import java.awt.BorderLayout)
(import java.awt.event.KeyEvent)
(import java.lang.Boolean)

(def ^:dynamic msp nil)

(defn fitness [tries code]
  (loop [score 0
         t tries]
    (if (= t 0)
      (int (/ score tries))
      (recur (+ score
                (eval `(binding [~'msp (new NUIMsPacman)]
                         (do (-> (new Thread ~'msp) .start)
                             (Thread/sleep 7000)
                             (-> ~'msp (.keyPressed KeyEvent/VK_5))
                             (Thread/sleep 500)
                             (-> ~'msp (.keyReleased KeyEvent/VK_5))
                             (Thread/sleep 500)
                             (-> ~'msp (.keyPressed KeyEvent/VK_1))
                             (Thread/sleep 500)
                             (-> ~'msp (.keyReleased KeyEvent/VK_1))
                             (Thread/sleep 500)
                             (while (not (-> ~'msp .isGameOver))
                               ~code)
                             (let [fitness-score# (-> ~'msp .getScore)]
                               (println fitness-score#)
                               (Thread/sleep 50)
                               (-> ~'msp  (.stop true))
                               fitness-score#)))))
             (dec t)))))

(defn fitness-graphic [tries code]
  (int
   (/
    (reduce +
            (doall
             (pmap (fn [c#]
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
                                    (Thread/sleep 500)
                                    (-> ~'msp (.keyReleased KeyEvent/VK_5))
                                    (Thread/sleep 500)
                                    (-> ~'msp (.keyPressed KeyEvent/VK_1))
                                    (Thread/sleep 500)
                                    (-> ~'msp (.keyReleased KeyEvent/VK_1))
                                    (Thread/sleep 500)
                                    (while (not (-> ~'msp .isGameOver))
                                      ~c#)
                                    (let [fitness-score# (-> ~'msp .getScore)]
                                      (-> ~'msp  (.stop true))
                                      (-> frame# .dispose)
                                      
                                      fitness-score#))))))
                   (repeat tries code))))
    tries)))

(def FUNCTION-LIST '((move-left)
                     (move-right)
                     (move-up)
                     (move-down)
                     (do expr+)
                     (get-pixel int int)
                     (get-pixels)
                     (if expr expr expr?)
                     (rand-int 10000)
                     (= expr+)
                     (msp> expr+)
                     (msp< expr+)
                     (msp+ expr+)
                     (msp- expr+)
                     (or expr+)
                     (and expr+)
                     (msp-sleep)
                     int
                     ()))

(def x 0)
(def y 0)
(def ATOM-LIST '(x
                 y))

(defn msp> [& keys]
  (let [l (remove #(not (instance? Number %1)) keys)]
    (if (empty? l)
      true
      (apply > l))))


(defn msp< [& keys]
  (let [l (remove #(not (instance? Number %1)) keys)]
    (if (empty? l)
      true
      (apply > l))))

(defn msp+ [& keys]
  (let [l (remove #(not (instance? Number %1)) keys)]
    (if (empty? l)
      0
      (apply + l))))

(defn msp- [& keys]
  (let [l (remove #(not (instance? Number %1)) keys)]
    (if (empty? l)
      0
      (apply - l))))

(defn msp-sleep []
  (Thread/sleep 100))

(defn move-left []
  (do (-> msp (.keyPressed KeyEvent/VK_LEFT))
      (Thread/sleep 10)
      (-> msp (.keyReleased KeyEvent/VK_LEFT))))

(defn move-right []
  (do (-> msp (.keyPressed KeyEvent/VK_RIGHT))
      (Thread/sleep 10)
      (-> msp (.keyReleased KeyEvent/VK_RIGHT))))

(defn move-up []
  (do (-> msp (.keyPressed KeyEvent/VK_UP))
      (Thread/sleep 10)
      (-> msp (.keyReleased KeyEvent/VK_UP))))

(defn move-down []
  (do (-> msp (.keyPressed KeyEvent/VK_DOWN))
       (Thread/sleep 10)
       (-> msp (.keyReleased KeyEvent/VK_DOWN))))

(defn get-pixel [i j]
  (-> msp (.getPixel  (if (number? i) (mod (int i) 224) (rand-int 224))
                      (if (number? j) (mod (int j) 288) (rand-int 288)))))

(defn get-pixels []
  (-> msp .getPixels))
