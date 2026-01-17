(ns com.biffweb.tasks.css
  (:require
   [clojure.java.io :as io]
   [com.biffweb.cljrun :as cljrun]
   [com.biffweb.tasks.util :as util]))

(defn css
  "Generates the target/resources/public/css/main.css file.

   The logic for running and installing Tailwind is:

   1. If tailwindcss has been installed via npm or bun, then that installation
      will be used.

   2. Otherwise, if the tailwindcss standalone binary has been downloaded to
      ./bin/, that will be used.

   3. Otherwise, if the tailwindcss standalone binary has been installed to the
      path (e.g. /usr/local/bin/tailwindcss), that will be used.

   4. Otherwise, the tailwindcss standalone binary will be downloaded to ./bin/,
      and that will be used."
  [& tailwind-args]
  (let [{:biff.tasks/keys [css-output]} (util/read-config)
        {:keys [local-bin-installed tailwind-cmd]} (util/tailwind-installation-info)]
    (when (and (= tailwind-cmd :local-bin) (not local-bin-installed))
      (cljrun/run-task "install-tailwind"))
    (when (= tailwind-cmd :local-bin)
      ;; This normally will be handled by install-tailwind, but we set it here in case that function
      ;; was interrupted. Assuming the download was incomplete, the 139 exit code (segfault) handler will be
      ;; triggered below. I've also had a report of exit code 137 (sigkill) being triggered.
      (.setExecutable (io/file (util/local-tailwind-path)) true))
    (try
      (apply util/shell (concat (case tailwind-cmd
                                  :npm        ["npx" "tailwindcss"]
                                  :bun        ["bunx" "tailwindcss"]
                                  :global-bin [(str (util/which "tailwindcss"))]
                                  :local-bin  [(util/local-tailwind-path)])
                                ["-c" "resources/tailwind.config.js"
                                 "-i" "resources/tailwind.css"
                                 "-o" css-output]
                                tailwind-args))
      (catch Exception e
        (if (and (#{137 139} (:exit (ex-data e)))
                 (#{:local-bin :global-bin} tailwind-cmd))
          (binding [*out* *err*]
            (println "It looks like your Tailwind installation is corrupted."
                     "Try deleting it and running this command again:")
            (println)
            (println "  rm" (if (= tailwind-cmd :local-bin)
                              (util/local-tailwind-path)
                              (str (util/which "tailwindcss"))))
            (println))
          (throw e))))))
