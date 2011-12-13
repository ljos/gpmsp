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

(defcluster :H3
  :parallel true
  :user "bjo013"
  :addresses ["mn121083"	
              "mn121084"	
              "mn121085"	
              "mn121086"	
              "mn121087"	
              "mn121088"	
              "mn121089"	
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

(defcluster :RF1
  :parallel true
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
              "mn190152"	
              "mn190153"	
              "mn190154"	
              "mn190155"	
              "mn190156"	
              "mn190157"
              "mn190070"])

(defcluster :RF3
  :parallel true
  :user "bjo013"
  :addresses ["mnr3-190117"	
              "mnr3-190118"	
              "mnr3-190119"	
              "mnr3-190120"	
              "mnr3-190121"	
              "mnr3-190122"	
              "mnr3-190123"	
              "mnr3-190124"	
              "mnr3-190125"	
              "mnr3-190126"	
              "mnr3-190127"	
              "mnr3-190128"	
              "mnr3-190129"	
              "mnr3-190130"	
              "mnr3-190134"	
              "mnr3-190136"	
              "mnr3-190138"	
              "mnr3-190139"
              "mnr3-190140"])

(deftask :date "echo date on cluster"  []
  (ssh "date"))

(deftask :check-for-user "Checks who is logged on" []
  (ssh "~/.scripts/check_for_user"))
