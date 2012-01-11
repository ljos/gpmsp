(defcluster :default-cluster
  :user "bjo013"
  :clients [{:host "localhost" :user "root"}])

(defcluster :H4 
  :user "bjo013"
  :addresses ["mn121033"	
              "mn121034"	
              "mn121035"	
              "mn121036"	
              "mn121037"	
              "mn121038"	
              "mn121039"	
              "mn121040"	
              "mn121041"	
              "mn121042"	
              "mn121043"	
              "mn121044"	
              "mn121045"
              "mn121046"	
              "mn121047"	
              "mn121048"	
              "mn121049"	
              "mn121050"	
              "mn121051"	
              "mn121052"	
              "mn121053"	
              "mn121054"
              "mn121055"
              "mn121056"	
              "mn121057"	
              "mn121058"	
              "mn121069"	
              "mn121071"	
              "mn121072"	
              "mn121073"	
              "mn121074"	
              "mn121075"	
              "mn121077"])

(defcluster :H3
  :user "bjo013"
  :addresses ["mn121083"	
              "mn121085"	
              "mn121086"	
              "mn121087"	
              "mn121088"	
              "mn121090"	
              "mn121091"	
              "mn121092"
              "mn121093"	
              "mn121094"	
              "mn121095"	
              "mn121096"	
              "mn121097"	
              "mn121098"	
              "mn121099"	
              "mn121100"	
              "mn121101"	
              "mn121102"	
              "mn121103"	
              "mn121104"	
              "mn121105"	
              "mn121107"])

(defcluster :RFB1
  :user "bjo013"
  :addresses ["mn190142"	
              "mn190143"	
              "mn190144"	
              "mn190145"	
              "mn190146"	
              "mn190147"	
              "mn190148"	
              "mn190149"	
              "mn190150"	
              "mn190151"	
              "mn190153"	
              "mn190154"	
              "mn190155"	
              "mn190156"	
              "mn190157"])

(defcluster :test
  :parallel true
  :user "bjo013"
  :log true
  :results true
  :addresses ["mn121037"	
              "mn121038"	
              "mn121039"	
              ;;"mn121040"
              ])

(loop [machines '("mn121033"	
                  "mn121034"	
                  "mn121035"	
                  "mn121036"	
                  "mn121037"	
                  "mn121038"	
                  "mn121039"	
                  "mn121040"	
                  "mn121041"	
                  "mn121042"	
                  "mn121043"	
                  "mn121044"	
                  "mn121045"
                  "mn121046"	
                  "mn121047"	
                  "mn121048"	
                  "mn121049"	
                  "mn121050"	
                  "mn121051"	
                  "mn121052"	
                  "mn121053"	
                  "mn121054"
                  "mn121055"
                  "mn121056"	
                  "mn121057"	
                  "mn121058"	
                  "mn121069"	
                  "mn121071"	
                  "mn121072"	
                  "mn121073"	
                  "mn121074"	
                  "mn121075"	
                  "mn121077")
       n 0]
  (when (not (empty? machines))
    (do (defcluster (keyword (str 'H4 "_" n))
          :parallel true
          :user "bjo013"
          :log true
          :results true
          :addresses (vec (list (first machines))))
        (recur (rest machines)
               (inc n)))))

(deftask :run-gp "Runs gp on the cluster" []
  (ssh (cd "mspacman"
           (run "~/.scripts/check_for_user; ~/.lein/bin/lein run -m mspacman.gpmsp/run-gen '({:program (do (msp-sleep) (move-down) (and mspacman blinky) (move-right) (move-down)), :fitness 390})'"))))

(deftask :kill-gp "Ends the gp run (prematurely)" []
  (ssh (run "killall java")))

(deftask :test-for-user "" []
  (ssh (run "~/.scripts/check_for_user")))

(deftask :run-gen "" [input]
  (ssh (cd "mspacman"
           (run (format "~/.lein/bin/lein run -m mspacman.gpmsp/run-gen '%s'"
                        input)))))
