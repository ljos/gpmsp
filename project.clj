(defproject mspacman "1.0.0-SNAPSHOT"
  :description "Genetic programming of mspacman bot"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/data.zip "0.1.0"]
                 [control "0.2.4-SNAPSHOT"]]
  :dev-dependencies [[control "0.2.3-SNAPSHOT"]]
  :java-source-path [["CottAGE_1.6_src/"]
                     ["MSPCRGP/"]]
  :jvm-opts ["-Xmx3G"
              "-Xms2G"
              "-Xmn1G"
              "-XX:+UseConcMarkSweepGC"
              "-Djava.awt.headless=true"]
  :main mspacman.core)
