(ns com.biffweb.tasks.ssh
  (:require
   [com.biffweb.tasks.util :as util]))

(defn logs
  "Tails the server's application logs."
  ([]
   (logs "300"))
  ([n-lines]
   (util/ssh-run (util/read-config) "journalctl" "-u" "app" "-f" "-n" n-lines)))

(defn restart
  "Restarts the app process via `systemctl restart app` (on the server)."
  []
  (util/ssh-run (util/read-config) "sudo systemctl reset-failed app.service; sudo systemctl restart app"))

(defn prod-repl
  "Opens an SSH tunnel so you can connect to the server via nREPL."
  []
  (let [{:keys [biff.tasks/server biff.nrepl/port]} (util/read-config)]
    (println "Connect to nrepl port" port)
    (spit ".nrepl-port" port)
    (util/shell "ssh" "-NL" (str port ":localhost:" port) (str "app@" server))))
