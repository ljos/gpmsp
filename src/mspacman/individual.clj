(ns mspacman.individual)

(import '(no.uib.bjo013.mspacman MsPacman NUIMsPacman GUIMsPacman))
(import javax.swing.JFrame)
(import java.awt.BorderLayout)
(import java.awt.event.KeyEvent)
(import java.lang.Boolean)

(def ^:dynamic msp nil)

(defn fitness [tries code]
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
               (loop [n# ~tries]
                 (if (< n# 1)
                   ()
                   (do (while (not (-> ~'msp .isGameOver))
                         ~code)
                       (recur (dec n#)))))
               (let [fitness-score# (-> ~'msp .getScore)]
                 (-> ~'msp  (.stop true))
                 fitness-score#)))))

(defn fitness-graphic [tries code]
  (println code)
  (eval `(let [~'msp (doto (new GUIMsPacman)
                       (.setSize 224 (+ 288 22)))
               frame# (doto (new JFrame)
                        (.setDefaultCloseOperation javax.swing.JFrame/EXIT_ON_CLOSE)
                        (.setSize 224 (+ 288 22))
                        (.setLocation 300 0)
                        (-> .getContentPane (.add ~'msp java.awt.BorderLayout/CENTER))
                        (.setVisible Boolean/TRUE))]
           (do (-> (new Thread ~'msp) .start)
               (Thread/sleep 7000)
               (-> ~'msp (.keyPressed KeyEvent/VK_5))
               (println "#### pressed 5 ####")
               (Thread/sleep 500)
               (-> ~'msp (.keyReleased KeyEvent/VK_5))
               (println "#### released 5")
               (Thread/sleep 500)
               (-> ~'msp (.keyPressed KeyEvent/VK_1))
               (println "#### pressed 1 ####")
               (Thread/sleep 500)
               (-> ~'msp (.keyReleased KeyEvent/VK_1))
               (println "#### released 1")
               (Thread/sleep 500)
               (loop [n# ~tries]
                 (println n#)
                 (if (< n# 1)
                   ()
                   (do (while (not (-> ~'msp .isGameOver))
                         ~code)
                       (recur (dec n#)))))
               (let [fitness-score# (-> ~'msp .getScore)]
                 (-> ~'msp  (.stop true))
                 fitness-score#)))))

(def FUNCTION-LIST '((move-left)
                     (move-right)
                     (move-up)
                     (move-down)
                     (do expr+)
                    ; (get-pixel x y)
                     (get-pixel int int)
                     (get-pixels)
                     (if expr expr expr?)
                     (rand-int 288)
                     (= expr+)
                     (msp> expr+)
                     (msp< expr+)
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
