(defproject mspacman "1.0.0-SNAPSHOT"
  :description "Genetic programming of mspacman bot"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/data.zip "0.1.0"]]
  :java-source-path "./" 
  :jvm-opts ["-Xmx3G"
              "-Xms2G"
              "-Xmn1G"
              "-XX:+UseConcMarkSweepGC"
              "-Djava.awt.headless=true"]
  :main mspacman.core)
