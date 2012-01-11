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

(def INT-LIST '(mspacman
                blinky
                pinky
                inky
                sue
                pills
                walkway
                wall1
             ;;   wall2
                wall3
                wall4
                wall5
                wall6
                wall7
                wall8
                (x)
                (y)
                (msp-get-areaxy)))

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
                      (msp-find-colour int)
                      (msp-get-area expr expr)
                      (msp-get-area int int)
                      (msp-get-area-below int)
                      (msp-get-area-above int)
                      (msp-get-area-leftof int)
                      (msp-get-area-rightof int)
                      (if expr expr expr?)
                      ;;(= expr+)
                      (= expr expr)
                      ;;(msp- expr+)
                      (msp- expr expr)
                      ;;(msp+ expr+)
                      (msp+ expr expr)
                      ;;(msp> expr+)
                      (msp> expr expr)
                      ;;(msp< expr+)
                      (msp< expr expr)
                      ;;(or expr+)
                      (or expr expr)
                      ;;(and expr+)
                      (and expr expr)
                      (msp-ghost? int)
                      (msp-wall? int))
                    ATOM-LIST))

(def ^:private x1 (ref 0))
(defn x [] @x1)

(def ^:private y1 (ref 0))
(defn y [] @y1)

(def mspacman 16776960)
(def blinky 16711680)
(def pinky 16759006)
(def inky  65502)
(def sue 16758855)
(def pills 14606046)
(def walkway 0)
(def wall1 14587719)
;;(def wall2 16759006)
(def wall3 4700382)
(def wall4 2171358)
(def wall5 65280)
(def wall6 4700311)
(def wall7 16758935)
(def wall8 14606046)

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

(defn get-area [x y]
  (let [col-freq (frequencies (for [i (range (* y 8) (+ (* y 8) 8))
                                    j (range (* x 8) (+ (* x 8) 8))]
                                (-> msp (.getPixel i j))))]
    (cond (and (contains? col-freq mspacman) (< 10 (get col-freq mspacman)))
          ,mspacman
          (= {0 22, 16711680 32, 14587719 8, 14606046 2} col-freq)
          ,0
          :else
          ,(loop [colours (keys col-freq)
                  colour (first colours)]
             (cond (empty? colours)
                   ,colour
                   (or (and (not (zero? (first colours)))
                            (<= (get col-freq colour)
                                (get col-freq (first colours))))
                       (= colour 0))
                   ,(recur (rest colours) (first colours))
                   :else
                   ,(recur (rest colours) colour))))))

(defn msp-get-area [x y]
  (let [i (if (number? x)
            (mod (int x) 36)
            0)
        j (if (number? y)
            (mod (int y) 28)
            0)]
    (get-area i j)))

(defn msp-get-areaxy []
  (msp-get-area @x1 @y1))

(defn find-colour [c]
  (loop [x 0
         y 0]
    (cond (= y 28)
          ,(recur (inc x) 0)
          (= x 36)
          ,(dosync (ref-set x1 -1)
                   (ref-set y1 -1)
                   false)
          (= c (msp-get-area x y))
          ,(dosync (ref-set x1 x)
                   (ref-set y1 y)
                   true)
          :else
          ,(recur x (inc y)))))

(defn msp-find-colour [c]
  (if (number? c)
    (find-colour c)
    false))

(defn msp-get-area-leftof [character]
  (do (msp-find-colour character)
      (msp-get-area @x1 (- @y1 1))))

(defn msp-get-area-rightof [character]
  (do (msp-find-colour character)
      (msp-get-area @x1 (+ @y1 2))))

(defn msp-get-area-above [character]
  (do (msp-find-colour character)
      (msp-get-area (- @x1 1) @y1)))

(defn msp-get-area-below [character]
  (do (msp-find-colour character)
      (msp-get-area (+ @x1 1) @y1)))

(defn msp-ghost? [character]
  (some #(= character %) (list blinky inky pinky sue)))

(defn msp-wall? [character]
  (some #(= character %) (list wall1 wall3 wall4 wall4 wall6 wall7 wall8)))
