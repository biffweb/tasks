(ns com.biffweb.tasks.install-tailwind
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [com.biffweb.tasks.config :as config]
   [com.biffweb.tasks.util :as util]
   [hato.client :as hato]))

(defn- infer-tailwind-file []
  (let [os-name (str/lower-case (System/getProperty "os.name"))
        os-type (cond
                  (str/includes? os-name "windows") "windows"
                  (str/includes? os-name "linux") "linux"
                  :else "macos")
        arch (case (System/getProperty "os.arch")
               ("amd64" "x86_64") "x64"
               "arm64")]
    (str "tailwindcss-" os-type "-" arch (when (= os-type "windows") ".exe"))))

(defn- local-tailwind-path []
  (if (util/windows?)
    "bin/tailwindcss.exe"
    "bin/tailwindcss"))

(defn install-tailwind
  "Downloads a Tailwind binary to bin/tailwindcss."
  [& [file]]
  (let [{:biff.tasks/keys [tailwind-build tailwind-version]} config/read
        [file inferred] (or (when file
                              [file false])
                            ;; Backwards compatibility.
                            (when tailwind-build
                              [(str "tailwindcss-" tailwind-build) false])
                            [(infer-tailwind-file) true])
        url (str "https://github.com/tailwindlabs/tailwindcss/releases/"
                 (if tailwind-version
                   (str "download/" tailwind-version)
                   "latest/download")
                 "/"
                 file)
        dest (io/file (local-tailwind-path))]
    (io/make-parents dest)
    (println "Downloading"
             (or tailwind-version "the latest version")
             "of" file "...")
    (when inferred
      (println "If that's the wrong file, run `clj -M:dev install-tailwind <correct file>`"))
    (println)
    (println "After the download finishes, you can avoid downloading Tailwind again for"
             "future projects if you copy it to your path, e.g. by running:")
    (println "  sudo cp" (local-tailwind-path) "/usr/local/bin/tailwindcss")
    (println)
    (io/copy (:body (hato/get url {:as :stream :http-client {:redirect-policy :normal}})) dest)
    (.setExecutable dest true)))
