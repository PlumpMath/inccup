(ns ewen.inccup.utils-test
  (:require [clojure.test :refer [is]]
            [cljs.analyzer.api :as ana-api]
            [cljs.compiler.api :as comp-api]
            [cljs.repl]
            [backtick]))

(defn eval-js [repl-env js]
  (let [{:keys [status value]}
        (cljs.repl/-evaluate repl-env "<cljs repl>" 1 js)]
    (is (= :success status))
    value))

(defn compile-cljs [cljs-form]
  (comp-api/emit
   (ana-api/no-warn
    (ana-api/analyze
     (assoc (ana-api/empty-env) :context :expr)
     cljs-form
     {:optimizations :simple}))))

(defmacro with-eval [repl-env & body]
  `(eval-js
    ~repl-env
    (compile-cljs ~@body)))

(def aliases {"vdom" "ewen.inccup.incremental.vdom"
              "comp" "ewen.inccup.compiler"
              "comp-test" "ewen.inccup.compiler-test"
              "gen-client" "ewen.inccup.gen-client"
              "utils" "ewen.inccup.utils-test"
              "c" "cljs.core"})

(backtick/defquote cljs-test-quote
  (fn [sym]
    (let [sym-ns (namespace sym)
          sym-name (name sym)]
      (symbol (get aliases sym-ns) (str sym-name)))))
