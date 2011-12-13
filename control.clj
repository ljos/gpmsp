(defcluster :default-cluster
  :clients [
    {:host "localhost" :user "root"}
    ])

(defcluster :H4
  :user "bjo013"
  :clients [
            "mn121055"	
            "mn121068"	
            "mn121041"	
            "mn121036"	
            "mn121042"	
            "mn121034"	
            "mn121035"	
            "mn121061"	
            "mn121066"	
            "mn121033"	
            "mn121046"	
            "mn121079"	
            "mn121047"	
            "mn121080"	
            "mn121067"	
            "mn121044"	
            "mn121037"	
            "mn121063"	
            "mn121057"	
            "mn121038"	
            "mn121070"	
            "mn121056"	
            "mn121040"	
            "mn121062"	
            "mn121073"	
            "mn121059"	
            "mn121075"	
            "mn121065"	
            "mn121071"	
            "mn121054"	
            "mn121069"	
            "mn121078"	
            "mn121060"	
            "mn121050"	
            "mn121072"	
            "mn121074"	
            "mn121053"	
            "mn121051"	
            "mn121064"	
            "mn121049"	
            "mn121077"	
            "mn121058"	
            "mn121039"	
            "mn121052"	
            "mn121048"	
            "mn121043"	
            "mn121045"])

(deftask :date "echo date on cluster"  []
  (ssh "date"))
