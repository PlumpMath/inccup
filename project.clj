(defproject ewen/hiccup "1.0.0"
  :description "A fast library for rendering HTML in Clojure"
  :url "http://github.com/EwenG/hiccup"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :source-paths ["src"]
  :test-paths ["test"]
  :dependencies [[org.clojure/clojure "1.9.0-alpha4"]
                 [org.clojure/clojurescript "1.9.36"]]
  :profiles {:dev {:dependencies [[org.clojure/test.check "0.9.0"]]}})
