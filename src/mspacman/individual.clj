(ns mspacman.individual)

(import '(no.uib.bjo013.mspacman MsPacman NUIMsPacman))
(import java.awt.event.KeyEvent)

(defn fitness [tries code]
  (eval `(let [~'msp (new NUIMsPacman)
               t#  (new Thread ~'msp)]
           (-> t# .start)
           (Thread/sleep 6000)
           (loop [n# ~tries]
             (if (< n# 1)
               ()
               (while (not (-> ~'msp .isGameOver))
                 ~code)
               (recur (dec n#))))
           (let [fitness-score# (-> ~'msp .getScore)]
             (-> ~'msp .stop Boolean/TRUE)
             fitness-score#))))

(def FUNCTION-LIST '((move-left)
                     (move-right)
                     (move-up)
                     (move-down)
                     (do expr+)
                     (get-pixel x y)
                     (get-pixel int int)
                     (get-pixels)
                     (if expr expr expr?)
                     (msp-loop x y expr+)
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

(defmacro msp-loop [&code]
  `(doseq [~'x (range 224)
           ~'y (range 288)]
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
      (apply > l)))) (apply < (remove #(not (instance? Number %1)) keys)))

(defn msp-sleep []
  (Thread/sleep 100))

(defn move-left []
  (-> msp (.keyPressed KeyEvent/VK_LEFT))
  (Thread/sleep 100)
  (-> msp (.keyReleased KeyEvent/VK_LEFT)))

(defn move-right []
  (-> msp (.keyPressed KeyEvent/VK_RIGHT))
  (Thread/sleep 100)
  (-> msp (.keyReleased KeyEvent/VK_RIGHT)))

(defn move-up []
  (-> msp (.keyPressed KeyEvent/VK_UP))
  (Thread/sleep 100)
  (-> msp (.keyReleased KeyEvent/VK_UP)))

(defn move-down []
  (-> msp (.keyPressed KeyEvent/VK_DOWN))
  (Thread/sleep 100)
  (-> msp (.keyReleased KeyEvent/VK_DOWN)))

(defn get-pixel [^int i ^int j]
  (-> msp (.getPixel  (mod i 224) (mod j 288))))

(defn get-pixels []
  (-> msp .getPixels))