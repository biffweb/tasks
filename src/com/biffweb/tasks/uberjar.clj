(ns com.biffweb.tasks.uberjar
   (:require
    [clojure.tools.build.api :as clj-build]
    [com.biffweb.cljrun :as cljrun]
    [com.biffweb.tasks.util :as util]))

(defn uberjar
  "Compiles the app into an Uberjar.

   Options:

     --no-clean
            Don't call the `clean` task before building the Uberjar."
  [& args]
  (let [{:biff.tasks/keys [main-ns generate-assets-fn] :as ctx} (util/read-config)
        class-dir "target/jar/classes"
        basis (clj-build/create-basis {:project "deps.edn"})
        uber-file "target/jar/app.jar"
        no-clean (some #{"--no-clean"} args)]
    (when-not no-clean
      (println "Cleaning...")
      (cljrun/run-task "clean"))
    (println "Generating CSS...")
    (cljrun/run-task "css" "--minify")
    (println "Calling" generate-assets-fn "...")
    ((requiring-resolve generate-assets-fn) ctx)
    (println "Compiling...")
    (clj-build/compile-clj {:basis basis
                            :ns-compile [main-ns]
                            :class-dir class-dir})
    (println "Building uberjar...")
    (clj-build/copy-dir {:src-dirs ["resources" "target/resources"]
                         :target-dir class-dir})
    (clj-build/uber {:class-dir class-dir
                     :uber-file uber-file
                     :basis basis
                     :main main-ns})
    (println "Done. Uberjar written to" uber-file)
    (println (str "Test with `BIFF_PROFILE=dev java -jar " uber-file "`"))))

