(defproject mspacman "1.0.0-SNAPSHOT"
  :description "Genetic programming of mspacman bot"
  :dependencies [[org.clojure/clojure "1.3.0"]]
  :java-source-path [["CottAGE_1.6_src/"]
                     ["MSPCRGP/"]]
  :java-opts ["-server"
              "-d64"
              "-Xmx2048M"
              "-Xms2048M"
              "-XX:MaxPermSize=256M"]
  :main mspacman.core)