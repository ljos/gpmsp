(ns nspacman.individual)

(import '(no.uib.bjo013.mspacman MsPacman NUIMsPacman))
(import java.awt.event.KeyEvent)

(defn fitness [tries code]
  (eval `(let [~'msp (new NUIMsPacman)
               t#  (new Thread ~'msp)]
           (-> t# .start)
           (Thread/sleep 6000)
           (loop [n# ~tries]
             (if (< n# 1)
               nil
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
                     (get-pixel int int)
                     (get-pixels)
                     (if expr expr expr?)
                     (msp-loop expr)
                     (= expr+)
                     (> expr+)
                     (< expr+)
                     (or expr+)
                     (and expr+)
                     ))

(def ATOM-LIST '(true
                 false
                 x
                 y))

(defmacro msp-loop [& code]
  `(doseq [~'x (range 224)
           ~'y (range 288)]
     ~@code))

(defn move-left []
  (-> msp (.keyPressed KeyEvent/VK_LEFT)))

(defn move-right []
  (-> msp (.keyPressed KeyEvent/VK_RIGHT)))

(defn move-up []
  (-> msp (.keyPressed KeyEvent/VK_UP)))

(defn move-down []
  (-> msp (.keyPressed KeyEvent/VK_DOWN)))

(defn get-pixel [^int i ^int j]
  (-> msp (.getPixel i j)))

((defn get-pixels []
   (-> msp .getPixels))