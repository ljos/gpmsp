(ns mspacman.individual)

(import '(no.uib.bjo013.mspacman Game GfxMsPacman))
(import javax.swing.JFrame)
(import '(java.awt BorderLayout Point))

(def ^:dynamic msp nil)
(def ^:dynamic targets nil)

(def ENTITY-LIST nil)
(def FUNCTION-LIST nil)
(def ITEM-LIST nil)
(def BOOL-LIST nil)
(def ATOM-LIST nil)

(def mspacman 'mspacman)
(def blinky 'blinky)
(def pinky 'pinky)
(def inky 'inky)
(def sue 'sue)
(def pill 'pill)
(def superpill 'superpill)
(def blue 'blue)

(def entity-list [mspacman
                  blinky
                  pinky
                  inky
                  sue
                  pill
                  superpill
                  blue])

(def blues (map #(symbol (str "b" %)) (range 0 4)))
(def pills (map #(symbol (str "p" %)) (range 0 220)))
(def superpills (map #(symbol (str "s" %)) (range 0 4)))

(defn get-point [^clojure.lang.Symbol entity]
  {:pre [(symbol? entity)]
   :post [(= Point (type %))]}
  (let [m (.getMap msp)]
    (case entity
     mspacman (.getMsPacman m)
     blinky (.getBlinky m)
     pinky (.getPinky m)
     inky (.getInky m)
     sue (.getSue m)
     pill (.getClosestPill m)
     superpill (.getClosestSuperPill m)
     blue (.getClosestBlue m))))

(defn set-target [^Point point]
  {:pre [(= Point (type point))]}
  (.setTarget msp point))

(defn remove-point [^Point point]
  {:pre [(= Point (type point))]}
  (.removePoint point))

(defn translate-point [^Point point ^Number x ^Number y]
  {:pre [(= Point (type point))
         (number? x)
         (number? y)]
   :post [(= Point (type %))]}
  (Point. (+ x (.x point)) (+ y (.x point))))

(defn rotate
  "rotate list from n to m 1 time"
  [^Number n ^Number m l]
  {:pre [(seq? l)
         (number? n)
         (number? m)]
   :post (seq? %)}
  (concat (take n l)
          (let [o (drop n (take m l))]
            (concat (rest o) (list (first o))))
          (drop m l)))

(defn fitness [tries code]
  (binding [msp (Game.)]
    (loop [score 0
           times 0]
      (if (or (<= tries times)
              (and (<= 3 times)
                   (= (/ score times) 120)))
        (int (/ score times))
        (do (.start msp)
            (recur (+ score
                      (do (while (not (.isGameOver msp))
                            (eval`~code)
                            (.update msp))
                          (.getScore msp)))
                   (inc times)))))))

(defn fitness-graphic [tries code]
  (binding [msp (Game.)]
    (let [gfx (doto (GfxMsPacman. (.initialize msp))
                (.setSize 224 (+ 288 22)))
          frame (doto (JFrame.)
                  (.setDefaultCloseOperation
                   javax.swing.JFrame/EXIT_ON_CLOSE)
                  (.setSize 224 (+ 288 22))
                  (.setLocation 100 0)
                  (-> .getContentPane
                      (.add gfx java.awt.BorderLayout/CENTER))
                  (.setVisible Boolean/TRUE))
          thread (Thread. gfx)]
      (.start thread)
      (loop [score 0
             times 0]
        (if (or (<= tries times)
                (and (<= 3 times)
                     (= (/ score times) 120)))
          (do (.dispose frame)
              (.stop gfx)
              (locking gfx
                (.notify gfx))
              (.join thread)
              (int (/ score tries)))
          (do (.setBitmap gfx (.start msp))
              (recur (+ score
                        (do (while (and (not (.isGameOver msp)))
                              (eval `~code)
                              (.setBitmap gfx (.update msp))
                              (locking gfx
                                (.notify gfx)))
                            (.getScore msp)))
                     (inc times))))))))

(defn- distance [^Point p]
  (.size (.calculatePath (.getMap msp) p 200)))

(defn most-valuable [l]
  (let [memd (memoize distance)]
    (reduce  (fn [^Point a ^Point b]
               (if (< (memd a) (memd b))
                a b))
            l)))