(ns com.biffweb.tasks.prod-dev 
  (:require
   [com.biffweb.cljrun :as cljrun]
   [com.biffweb.tasks.util :as util]
   [nextjournal.beholder :as beholder])
  (:import
   [java.util Timer TimerTask]))

;; https://gist.github.com/oliyh/0c1da9beab43766ae2a6abc9507e732a
(defn- debounce
  ([f] (debounce f 1000))
  ([f timeout]
   (let [timer (Timer.)
         task (atom nil)]
     (with-meta
      (fn [& args]
        (when-let [t ^TimerTask @task]
          (.cancel t))
        (let [new-task (proxy [TimerTask] []
                         (run []
                           (apply f args)
                           (reset! task nil)
                           (.purge timer)))]
          (reset! task new-task)
          (.schedule timer new-task timeout)))
      {:task-atom task}))))

(defn- auto-soft-deploy [{:biff.tasks/keys [watch-dirs]
                          :or {watch-dirs ["src" "dev" "resources" "test"]}}]
  (cljrun/run-task "soft-deploy")
  (apply beholder/watch
         (debounce (fn [_]
                     (cljrun/run-task "soft-deploy"))
                   500)
         watch-dirs))

(defn prod-dev
  "Runs the soft-deploy task whenever a file is modified. Also runs prod-repl and logs."
  []
  (when-not (util/which "rsync")
    (binding [*out* *err*]
      (println "`rsync` command not found. Please install it.")
      (println "Alternatively, you can deploy without downtime by running"
               "`git add .; git commit; bb soft-deploy`"))
    (System/exit 1))
  (util/with-ssh-agent (util/read-config)
    (auto-soft-deploy (util/read-config))
    (util/future (cljrun/run-task "prod-repl"))
    (cljrun/run-task "logs")))
