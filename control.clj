(defcluster :default-cluster
  :clients [
    {:host "localhost" :user "root"}
    ])

(defcluster :H4 
  :parallel true
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

(deftask :date "echo date on cluster"  []
  (ssh "date"))
