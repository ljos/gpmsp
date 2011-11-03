(ns mspacman.core
  (:require [mspacman.gpmsp :as gp])
  (:use mspacman.individual))

(import '(no.uib.bjo013.mspacman MsPacman GUIMsPacman NUIMsPacman))
(import javax.swing.JFrame)
(import java.awt.BorderLayout)
(import java.lang.Boolean)
(import java.awt.event.KeyEvent)


(defn start-GUIMsPacman []
  (let [msp (doto (new GUIMsPacman)
              (.setSize 224 (+ 288 22)))
        frame (doto (new JFrame)
                (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
                (.setSize 224 (+ 288 22))
                (.setLocation 300 0)
                (-> .getContentPane (.add msp BorderLayout/CENTER))
                (.setVisible Boolean/TRUE))]
    (-> (new Thread msp) .start)))

(defn start-NUIMsPacman-test []
  (let [msp (new NUIMsPacman)
        t (new Thread msp)]
    (-> t .start)
    (dotimes [n 6]
      (Thread/sleep 1000)
      (println n))
    (-> msp (.keyPressed KeyEvent/VK_5))
    (println "#### pressed 5 ####")
    (Thread/sleep 500)
    (-> msp (.keyReleased KeyEvent/VK_5))
    (println "#### released 5")
    (Thread/sleep 500)
    (-> msp (.keyPressed KeyEvent/VK_1))
    (println "#### pressed 1 ####")
    (Thread/sleep 500)
    (-> msp (.keyReleased KeyEvent/VK_1))
    (println "#### released 1")
    (Thread/sleep 500)
    (-> msp (.keyPressed KeyEvent/VK_LEFT))
    (println "#### pressed LEFT ####")
    (Thread/sleep 500)
    (-> msp (.keyReleased KeyEvent/VK_LEFT))
    (println "#### released LEFT")
    (Thread/sleep 500)
    (dotimes [i 30]
      (Thread/sleep 1000)
      (println i))
    (print (-> msp .getScore))
    (print " ")
    (println (-> t .isAlive))
    (-> msp  (.stop  Boolean/TRUE))
    (Thread/sleep 1000)
    (println (-> t .isAlive))))

(defn -main [& args]
  (gp/gp-run))