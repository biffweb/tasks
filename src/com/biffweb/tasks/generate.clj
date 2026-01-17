(ns com.biffweb.tasks.generate 
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [com.biffweb.tasks.util :as util]))

(defn- new-secret [length]
  (let [buffer (byte-array length)]
    (.nextBytes (java.security.SecureRandom/getInstanceStrong) buffer)
    (.encodeToString (java.util.Base64/getEncoder) buffer)))

(defn generate-secrets
  "Prints new secrets to put in config.env."
  []
  (println "Put these in your config.env file:")
  (println)
  (println (str "COOKIE_SECRET=" (new-secret 16)))
  (println (str "JWT_SECRET=" (new-secret 32)))
  (println))

(defn generate-config
  "Creates a new config.env file if one doesn't already exist."
  []
  (if (util/exists? "config.env")
    (binding [*out* *err*]
      (println "config.env already exists. If you want to generate a new file, run `mv config.env config.env.backup` first.")
      (System/exit 3))
    (let [contents (slurp (io/resource "config.template.env"))
          contents (str/replace contents
                                #"\{\{\s+new-secret\s+(\d+)\s+\}\}"
                                (fn [[_ n]]
                                  (new-secret (parse-long n))))]
      (spit "config.env" contents)
      (println "New config generated and written to config.env."))))
