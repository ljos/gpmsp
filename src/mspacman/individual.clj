(ns mspacman.individual)

(import '(no.uib.bjo013.mspacman MsPacman NUIMsPacman GUIMsPacman))
(import javax.swing.JFrame)
(import java.awt.BorderLayout)
(import java.awt.event.KeyEvent)
(import java.lang.Boolean)

(defn fitness [tries code]
  (eval `(let [~'msp (doto (new GUIMsPacman)
                       (.setSize 224 (+ 288 22)))
               frame# (doto (new JFrame)
                        (.setDefaultCloseOperation javax.swing.JFrame/EXIT_ON_CLOSE)
                        (.setSize 224 (+ 288 22))
                        (.setLocation 300 0)
                        (-> .getContentPane (.add ~'msp java.awt.BorderLayout/CENTER))
                        (.setVisible Boolean/TRUE))]
           (do (-> (new Thread ~'msp) .start)
               (Thread/sleep 6000)
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
                         (do (println 'while) ~code))
                       (recur (dec n#)))))
               (let [fitness-score# (-> ~'msp .getScore)]
                 (-> ~'msp  (.stop true))
                 fitness-score#)))))

(def FUNCTION-LIST '((move-left msp)
                     (move-right msp)
                     (move-up msp)
                     (move-down msp)
                     (do expr+)
                     (get-pixel x y msp)
                     (get-pixel int int msp)
                     (get-pixels msp)
                     (if expr expr expr?)
                     ;(msp-loop expr+ int int)
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

(defmacro msp-loop [i j & code]
  `(doseq [~'x  (/ ~i)
           ~'y ~j]
     ~@code))

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

(defn move-left [msp]
  (println 'move-left)
  (do (-> msp (.keyPressed KeyEvent/VK_LEFT))
      (Thread/sleep 10)
      (-> msp (.keyReleased KeyEvent/VK_LEFT))))

(defn move-right [msp]
  (println 'move-right)
  (do (-> msp (.keyPressed KeyEvent/VK_RIGHT))
      (Thread/sleep 10)
      (-> msp (.keyReleased KeyEvent/VK_RIGHT))))

(defn move-up [msp]
  (println 'move-up)
  (do (-> msp (.keyPressed KeyEvent/VK_UP))
      (Thread/sleep 10)
      (-> msp (.keyReleased KeyEvent/VK_UP))))

(defn move-down [msp]
  (println 'move-down)
  (do (-> msp (.keyPressed KeyEvent/VK_DOWN))
       (Thread/sleep 10)
       (-> msp (.keyReleased KeyEvent/VK_DOWN))))

(defn get-pixel [msp ^long i ^long j]
  (-> msp (.getPixel  (mod i 224) (mod j 288))))

(defn get-pixels [msp]
  (-> msp .getPixels))
