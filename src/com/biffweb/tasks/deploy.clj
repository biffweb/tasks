(ns com.biffweb.tasks.deploy 
  (:require
   [com.biffweb.cljrun :as cljrun]
   [com.biffweb.tasks.util :as util]))

(defn deploy
  "Pushes code to the server and restarts the app.

   Uploads config and code to the server, using `rsync` if it's available, and
   `git push` otherwise. Then restarts the app.

   You must set up a server first. See https://biffweb.com/docs/reference/production/"
  []
  (util/with-ssh-agent (util/read-config)
    (cljrun/run-task "css" "--minify")
    (util/push-files (util/read-config))
    (cljrun/run-task "restart")))
