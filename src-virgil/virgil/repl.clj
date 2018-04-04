(ns virgil.repl
  (:require [clojure.set :as set]
            [clojure.tools.namespace.dir :as ctn-dir]
            [clojure.tools.namespace.file :as ctn-file]
            [clojure.tools.namespace.find :as ctn-find]
            [clojure.tools.namespace.parse :as ctn-parse]
            [clojure.tools.namespace.repl :as ctn-repl]
            [virgil.compile :refer [compile-all-java]]
            [clojure.java.io :as io]
            [clojure.string :as string])
  (:import java.io.File
           java.util.Arrays))

(defonce ^:private java-source-dirs (atom ["src-java"]))
(defonce ^:private last-javac-result (atom []))
(defonce ^:private last-java-source-mtimes (atom {}))

(defn java-source-mtimes []
  (->> @java-source-dirs
       (mapcat #(file-seq (io/file %)))
       #_(filter (fn [^File f] (and (.isFile f)
                                  (string/ends-with? (.getName f) ".java"))))
       #_(reduce (fn [m ^File f]
                  (assoc m  (.getPath f) (.lastModified f)))
                {})))

(.listFiles (io/file "src-java"))

(defn set-java-source-dirs! [dirs]
  (reset! java-source-dirs dirs)
  (reset! last-java-source-mtimes (java-source-mtimes)))

(defn clear []
  (reset! last-javac-result [])
  (reset! last-java-source-mtimes {}))

;; Takes results of two (compile-all-java) calls and returns set of modified
;; classes, as symbols.
#_(defn- changed-classes [r1 r2]
   (let [m1 (into {} r1)
         m2 (into {} r2)]
     (set
      (for [k (set/union (set (keys m1))
                         (set (keys m2)))
            :let [^bytes b1 (m1 k)
                  ^bytes b2 (m2 k)]
            :when (not (and b1 b2 (Arrays/equals b1 b2)))]
        (symbol k)))))

;; returns set of classes that have changed since last compile
#_(defn- recompile-all-java []
   (let [r @last-javac-result
         r' (compile-all-java @java-source-dirs)]
     (reset! last-javac-result r')
     (changed-classes r r')))

(defn recompile-all-java []
  (let [mtimes @last-java-source-mtimes
        mtimes' (java-source-mtimes)]
    (reset! last-java-source-mtimes mtimes')
    (when (not= mtimes mtimes')
      (let [r (compile-all-java @java-source-dirs)]
        (map #(symbol (first %)) r)))))

;; steal private vars
(def ^:private deps-from-libspec #'clojure.tools.namespace.parse/deps-from-libspec)
(def ^:private find-files #'clojure.tools.namespace.dir/find-files)
(def ^:private refresh-dirs #'clojure.tools.namespace.repl/refresh-dirs)

(defn- all-clj-files []
  (find-files refresh-dirs) ;; for tools.namespace 0.2.11
  ;;(find-files refresh-dirs ctn-find/clj) ;; for tools.namespace 0.3.0-alpha4
  )

(defn- ns-form->imports [ns-form]
  (->> ns-form
       ;; Get all the :imports
       (mapcat (fn [x]
                 (when (and (sequential? x)
                            (= (first x) :import))
                   (rest x))))
       ;; Parse fully qualified class names
       (mapcat #(deps-from-libspec nil %))
       (set)))

(defn- all-java-deps []
  (->> (all-clj-files)
       (pmap (fn [f]
               (let [nsf (ctn-file/read-file-ns-decl f)]
                 (for [klass (ns-form->imports nsf)]
                   [klass f]))))
       (apply concat)
       (reduce (fn [m [klass f]]
                 (update m klass set/union #{f}))
               {})))

;; Given a set of classes (as symbols), return set of clojure files that depend on
;; any of the given classes.
(defn- ns-dependants [klasses]
  (set (mapcat (all-java-deps) klasses)))

;; Main function to be used alongside of clojure.tools.namespace.repl/refresh.
;; Detects which clojure files depend on changed java classes and touches those
;; files, which will cause tools.namespace.repl/refresh to recompile them.
(defn refresh []
  (println "Recompiling java...")
  (let [changed (recompile-all-java)]
    (when (seq changed)
      (println "  Updated" (count changed) "java classes.")
      (let [dirty (ns-dependants changed)
            now (System/currentTimeMillis)]
        (println "  Dirtying" (count dirty) "clj files")
        (doseq [^File f dirty]
          (.setLastModified f now))))))
