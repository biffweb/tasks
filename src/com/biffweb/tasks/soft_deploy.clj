(ns com.biffweb.tasks.soft-deploy
  (:require
   [com.biffweb.cljrun :as cljrun]
   [com.biffweb.tasks.util :as util]))

(defn soft-deploy
  "Pushes code to the server and evaluates changed files.

   1. Builds css
   2. Uploads files
   3. `eval`s any changed files
   4. Regenerates static html files

   Does not refresh or restart, so there isn't any downtime."
  []
  (let [{:biff.tasks/keys [soft-deploy-fn on-soft-deploy]
         :keys [biff.nrepl/port]
         :as ctx} (util/read-config)]
    (util/with-ssh-agent ctx
      (cljrun/run-task "css" "--minify")
      (util/push-files ctx)
      (util/ssh-run ctx
                    "trench"
                    "-p" port
                    "-e" (or on-soft-deploy
                             ;; backwards compatibility
                             (str "\"(" soft-deploy-fn " @com.biffweb/system)\""))))))
