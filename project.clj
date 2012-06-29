(defproject mspacman "1.0.0-SNAPSHOT"
  :description "Genetic programming of mspacman bot"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/data.zip "0.1.0"]
                 [org.clojure/tools.logging "0.2.3"]]
  :java-source-path "./" 
  :jvm-opts ["-Xmx4G"
             "-Xms2G"
             "-Xmn2G"
             "-XX:+UseParallelGC"
             "-XX:+AggressiveOpts"]
  :main mspacman.core)
