(ns mspacman.core)

(import mspacman.GUIMsPacman)
(import javax.swing.JFrame)
(import java.awt.BorderLayout)
(import java.lang.Boolean)

(defn start-GUIMsPacman [& args]
  (let [msp (doto (new GUIMsPacman)
              (.setSize 224 (+ 288 22)))
        frame (doto (new JFrame)
                (.setDefaultCloseOperation (. JFrame EXIT_ON_CLOSE))
                (.setSize 224 (+ 288 22))
                (.setLocation 300 0)
                (-> .getContentPane (.add msp (. BorderLayout CENTER)))
                (.setVisible (. Boolean TRUE)))]
    (-> (new Thread msp) .start)))

(defn -main [& args]
  (start-GUIMsPacman))