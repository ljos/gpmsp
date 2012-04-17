(defproject mspacman "1.0.0-SNAPSHOT"
  :description "Genetic programming of mspacman bot"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/data.zip "0.1.0"]
                 [org.clojure/data.priority-map "0.0.1"]]
  :java-source-path "./" 
  :jvm-opts ["-Xmx3G"
             "-Xms2G"
             "-Xmn1G"
             "-XX:+UseConcMarkSweepGC"]
  :main mspacman.core)
