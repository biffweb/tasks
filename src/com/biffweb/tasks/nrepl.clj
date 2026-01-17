(ns com.biffweb.tasks.nrepl
  (:require
   [com.biffweb.tasks.util :as util]
   [nrepl.cmdline :as nrepl-cmd]))

(defn nrepl
  "Starts an nrepl server without starting up the application."
  []
  (let [{:biff.nrepl/keys [port args]} (util/read-config)]
    (spit ".nrepl-port" port)
    (apply nrepl-cmd/-main args)))
