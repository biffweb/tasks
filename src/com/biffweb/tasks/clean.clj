(ns com.biffweb.tasks.clean
  (:require [clojure.tools.build.api :as clj-build]))

(defn clean
  "Deletes generated files"
  []
  (clj-build/delete {:path "target"}))
