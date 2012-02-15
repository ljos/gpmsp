(ns mspacman.control
  (:use [clojure.java.io :only [reader]]
        [clojure.string :only [join]]))

(def ^{:dynamic :private} *runtime* (Runtime/getRuntime))

(defstruct ExecProcess :machine :process :in :err :stdout :stderr :status)

(def ALL-MACHINES [;"mn121033"
                   "mn121034"	
                   "mn121035" "mn121036"
                   "mn121037"
                   ;"mn121038"
                   "mn121039" "mn121040"	
                   "mn121041" "mn121042"	
                   "mn121043" "mn121044"	
                   "mn121045" "mn121046"	
                   "mn121047" "mn121048"	
                   "mn121049" "mn121050"	
                   "mn121051" "mn121052"	
                   "mn121053" "mn121054"
                   "mn121055" "mn121056"	
                   "mn121057" "mn121058"
                   "mn121069" "mn121070"
                   "mn121071" "mn121072"
                   "mn121073" "mn121074"
                   "mn121075" "mn121077"
                   ;"mn121083"
                   "mn121084"
                   "mn121085"
                   ;"mn121086"	
                   "mn121087" "mn121088"
                   "mn121089" "mn121090"
                   "mn121091" "mn121092"
                   "mn121093" "mn121094"
                   "mn121095" "mn121096"
                   "mn121097" "mn121098"
                   "mn121099" "mn121100"
                   "mn121101" "mn121102"
                   "mn121103" "mn121104"
                   "mn121105" "mn121107"
                   "mn190142" "mn190143"
                   "mn190144" "mn190145"
                   "mn190146" 
                   "mn190148" "mn190149"
                   "mn190151" 
                   "mn190155"
                   "mn190156" "mn190157"
                   ])

(defn logon-all []
  (println "Creating new kerberos tickets.")
  (.waitFor (.exec *runtime* "sh ~/.scripts/logon_all"))
  (println "Finshed creating new kerberos tickets."))

(defn- spawn
  [machine cmdarray]
  (let [process (.exec *runtime* cmdarray)
        in (reader (.getInputStream process) :encoding "UTF-8")
        err (reader (.getErrorStream process) :encoding "UTF-8")
        execp (struct ExecProcess machine process in err)
        pagent (agent execp)]
    (send-off pagent
              (fn [exec-process]
                (assoc exec-process :stdout (str (:stdout exec-process)
                                                 (join "\r\n" (doall (line-seq in)))))))
    (send-off pagent
              (fn [exec-process]
                (assoc exec-process :stderr (str (:stderr exec-process)
                                                 (join "\r\n" (doall (line-seq err)))))))
    pagent))

(defn- await-process [pagent]
  (let [execp @pagent
        process (:process execp)
        in (:in execp)
        err (:err execp)]
    (await pagent)
    (.close in)
    (.close err)
    (.waitFor process)))

(defn run-task [pagent]
  (let [execp @pagent
        status (await-process pagent)]
    (println (str "Finshed job at " (:machine execp)))
    (assoc execp :status status)))

(defn send-to-machine [machine task]
  (println (str "Starting " task " at " machine))
  (spawn machine
         (into-array String
                     ["ssh"
                      "-q"
                      "-o ConnectTimeout=2"
                      "-o StrictHostKeyChecking=no"
                      "-o PasswordAuthentication=no"
                      (format "bjo013@%s" machine)
                      task])))

(defn cluster-kill []
  (let [out (doall
             (map run-task
                  (map #(send-to-machine % "killall java")
                       ALL-MACHINES)))]
    (shutdown-agents)
    out))