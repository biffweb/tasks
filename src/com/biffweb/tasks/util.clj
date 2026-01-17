(ns com.biffweb.tasks.util
  "Helper functions that don't have external dependencies.

   Or at least, if they do have external deps, they use `requiring-resolve` so as not to slow down
   other tasks."
  (:refer-clojure :exclude [future])
  (:require
   [clojure.java.io :as io]
   [clojure.java.shell :as sh]
   [clojure.string :as str]
   [clojure.stacktrace :as st]))

(defmacro future [& body]
  `(clojure.core/future
     (try
       ~@body
       (catch Exception e#
         (binding [*err* *out*]
           (st/print-stack-trace e#))))))

(defn windows? []
  (-> (System/getProperty "os.name")
      (str/lower-case)
      (str/includes? "windows")))

(defn which [& args]
  (apply (requiring-resolve 'babashka.fs/which) args))

(defn bun-pkg-installed? [package-name]
  (and (which "bun")
       (str/includes? (:out (sh/sh "bun" "pm" "ls"))
                      package-name)))

(defn local-tailwind-path []
  (if (windows?)
    "bin/tailwindcss.exe"
    "bin/tailwindcss"))

(defn sh-success? [& args]
  (try
    (= 0 (:exit (apply sh/sh args)))
    (catch Exception _
      false)))

(defn exists? [f]
  (.exists (io/file f)))

(defn tailwind-installation-info []
  (let [local-bin-installed (exists? (local-tailwind-path))]
    {:local-bin-installed local-bin-installed
     :tailwind-cmd
     (cond
       (bun-pkg-installed? "tailwindcss")                    :bun
       (sh-success? "npm" "list" "tailwindcss")              :npm
       (and (which "tailwindcss") (not local-bin-installed)) :global-bin
       :else                                                 :local-bin)}))

(def read-config
  (memoize (fn []
             ((requiring-resolve 'com.biffweb.config/use-aero-config)
              {:biff.config/skip-validation true}))))

(def ^:dynamic *shell-env* nil)

(defn shell
  "Difference between this and clojure.java.shell/sh:

   - inherits std{in,out,err}
   - throws on non-zero exit code
   - puts *shell-env* in the environment"
  [& args]
  (apply (requiring-resolve 'babashka.process/shell)
         {:extra-env *shell-env*}
         args))

(defn get-env-from [cmd]
  (let [{:keys [exit out]} (sh/sh "sh" "-c" (str cmd "; printenv"))]
    (when (= 0 exit)
      (->> out
           str/split-lines
           (map #(vec (str/split % #"=" 2)))
           (filter #(= 2 (count %)))
           (into {})))))

(defn with-ssh-agent* [{:keys [biff.tasks/skip-ssh-agent]} f]
  (if-let [env (and (not skip-ssh-agent)
                    (which "ssh-agent")
                    (not (sh-success? "ssh-add" "-l"))
                    (nil? *shell-env*)
                    (if (windows?)
                      {}
                      (get-env-from "eval $(ssh-agent)")))]
    (binding [*shell-env* env]
      (try
        (try
          (shell "ssh-add")
          (println "Started an ssh-agent session. If you set up `keychain`, you won't have to enter your password"
                   "each time you run this command: https://www.funtoo.org/Funtoo:Keychain")
          (catch Exception e
            (binding [*out* *err*]
              (st/print-stack-trace e)
              (println "\nssh-add failed. You may have to enter your password multiple times. You can avoid this if you set up `keychain`:"
                       "https://www.funtoo.org/Funtoo:Keychain"))))
        (f)
        (finally
          (sh/sh "ssh-agent" "-k" :env *shell-env*))))
    (f)))

(defmacro with-ssh-agent [ctx & body]
  `(with-ssh-agent* ~ctx (fn [] ~@body)))

(defn ssh-run [{:keys [biff.tasks/server]} & args]
  (apply shell "ssh" (str "app@" server) args))

(defn push-files-rsync [{:biff.tasks/keys [server deploy-untracked-files]}]
  (let [files (->> (:out (sh/sh "git" "ls-files"))
                   str/split-lines
                   (map #(str/replace % #"/.*" ""))
                   distinct
                   (concat deploy-untracked-files)
                   (filter exists?))]
    (when (and (not (windows?)) (exists? "config.env"))
      ((requiring-resolve 'babashka.fs/set-posix-file-permissions) "config.env" "rw-------"))
    (->> (concat ["rsync" "--archive" "--verbose" "--relative" "--include='**.gitignore'"
                  "--exclude='/.git'" "--filter=:- .gitignore" "--delete-after" "--protocol=29"]
                 files
                 [(str "app@" server ":")])
         (apply shell))))

(defn push-files-git [{:biff.tasks/keys [deploy-cmd
                                         git-deploy-cmd
                                         deploy-from
                                         deploy-to
                                         deploy-untracked-files
                                         server]}]
  (when-some [files (not-empty (filterv exists? deploy-untracked-files))]
    (when-some [dirs (->> files
                          (keep (comp not-empty (requiring-resolve 'babashka.fs/parent)))
                          not-empty)]
      (apply shell "ssh" (str "app@" server) "mkdir" "-p" dirs))
    (doseq [file files]
      (shell "scp" file (str "app@" server ":" file))))
  ;; deploy-cmd, deploy-from, and deploy-to are all deprecated (but still supported for backwards compatibility)
  (if-some [git-deploy-cmd (or git-deploy-cmd deploy-cmd)]
    (apply shell git-deploy-cmd)
    (shell "git" "push" deploy-to deploy-from)))

(defn push-files [{:keys [biff.tasks/deploy-with] :as ctx}]
  (let [deploy-with (or deploy-with
                        (if (which "rsync")
                          :rsync
                          :git))]
    (case deploy-with
      :rsync (push-files-rsync ctx)
      :git (push-files-git ctx)
      (binding [*out* *err*]
        (println "Unrecognized config option `:biff.tasks/deploy-with " deploy-with "`. Valid options are "
                 ":rsync and :git")
        (System/exit 2)))))
