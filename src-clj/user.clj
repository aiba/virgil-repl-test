(ns user
  (:require [clojure.tools.namespace.dir :as dir]
            [clojure.tools.namespace.repl :as tnr]
            [clojure.tools.namespace.track :as track]
            [virgil.repl :as virgil-repl]))

(tnr/set-refresh-dirs "src-clj")
(virgil-repl/set-java-source-dirs! ["src-java"])

(defn start []
  (println "----"))

(defn stop []
  nil)

(defn reset []
  (println "reseting...")
  (stop)
  (virgil-repl/refresh)
  (tnr/refresh :after 'user/start))

(defn hard-reset []
  (tnr/clear)
  (virgil-repl/clear)
  (reset))

(comment
  (hard-reset)
  )
