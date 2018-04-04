
(defproject virgil-repl-test "0.1.0-SNAPSHOT"
  :pedantic? :abort
  :plugins [[cider/cider-nrepl "0.17.0-SNAPSHOT"]
            [refactor-nrepl "2.4.0-SNAPSHOT"]]
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/tools.nrepl "0.2.13"]
                 [virgil "0.1.8"]]
  :source-paths ["src-clj"
                 "src-virgil"]
  :java-source-paths ["src-java"]
  :target-path "target/%s"
  :compile-path "%s/classy-files")
