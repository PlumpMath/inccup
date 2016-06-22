(ns ewen.inccup.incremental.core-test
  (:require [cljs.test :refer-macros [deftest testing is run-tests]]
            [ewen.inccup.compiler
             :refer-macros [with-opts! register-tagged-literal! h]
             :as comp]
            [ewen.inccup.incremental.vdom :as vdom
             :refer [Component render! update!]]
            [cljs.pprint :refer [pprint] :refer-macros [pp]]
            [goog.array]
            [goog.dom])
  (:require-macros [ewen.inccup.incremental.core-test
                    :refer [multi-defn]]))

(register-tagged-literal! h)

(set-print-fn! #(.log js/console %))

(defn node= [node1 node2]
  (let [equal-nodes (volatile! true)]
    (if (not= (.-nodeName node1) (.-nodeName node2))
      (vreset! equal-nodes false)
      (let [attrs1 (.-attributes node1)
            attrs2 (.-attributes node2)]
        (when (not (and (nil? attrs1) (nil? attrs2)))
          (if (not= (.-length attrs1) (.-length attrs2))
            (vreset! equal-nodes false)
            (loop [index 0
                   l (.-length attrs1)]
              (when (and @equal-nodes (< index l))
                (when (not= (.-name (aget attrs1 index))
                            (.-name (aget attrs2 index)))
                  (vreset! equal-nodes false))
                (when (not= (.-value (aget attrs1 index))
                            (.-value (aget attrs2 index)))
                  (vreset! equal-nodes false))
                (recur (inc index) l)))))))
    (when @equal-nodes
      (let [children1 (.-childNodes node1)
            children2 (.-childNodes node2)]
        (if (not= (.-length children1) (.-length children2))
          (vreset! equal-nodes false)
          (loop [index 0
                 l (.-length children1)]
            (when (and @equal-nodes (< index l))
              (when-not (node= (aget children1 index)
                               (aget children2 index))
                (vreset! equal-nodes false))
              (recur (inc index) l))))))
    @equal-nodes))

(defn new-root []
  (let [old-root (.getElementById js/document "root")
        new-root (goog.dom/createDom "div" #js {:id "root"})]
    (if old-root
      (goog.dom/replaceNode new-root old-root)
      (goog.dom/appendChild (.-body js/document) new-root))
    new-root))

(defn root []
  (.querySelector js/document "#root"))

(multi-defn comp1 [x] (h [:div#ii.cc {} x 4]))

(defn def2 [x y z] #h [x y z])
(defn def3 [x] #h [:div#ii.cc x])
(defn def4 [] #h [:div "<content"])
(defn def5 [x] #h [x "content"])

#_(deftest test1
  (testing "test1"
    (let [comp (render! (new-root) def1 "e")]
      true)))

(comment
  (render! (new-root) comp1 "e")
  (str (comp1-string "e"))

  (-> (render! (new-root) def1 "e")
      (update! def1 "f")
      (update! def1 "g"))

  (def cc (render! (new-root) def2 :p {:e "e"} "t"))
  (update! cc def2 :p {:class "c2"} "t")

  (-> (render! (new-root) def2 :p {:class "c"} "t")
      (update! def2 :p {:class "c2" :e "e"} "t"))

  (-> (render! (new-root) def3 "e")
      (update! def3 "f")
      (update! def3 {:id "i"}))

  (-> (render! (new-root) def4)
      (update! def4))

  (def cc (render! (new-root) def5 :p))
  (update! cc def5 :div)
  (update! cc def5 :p)
  (update! cc def5 :input)
  )

(defn template1 [x] #h [:p#ii.cc {:e x :class x} x "4"])
(defn template2 [x z] #h [:p {} (count x) #h [:p z]
                          (for [y x] (template1 y))])

(comment
  (def cc (render! (new-root) template2 (list 1 2) nil))
  (update! cc template2 (list 1 3) #h [:div])
  (update! cc template2 (list 4) {:class "c"})
  (update! cc template2 (list 4) {:class "e"})
  )

(defn template3 [x] #h [:p {:class x} nil x])
(defn template4 [x] #h [:p {}
                        (for [y x]
                          (with-opts! {:key y}
                            (template3 (inc y))))])
(defn template44 [x z] (let [cc #h [:p]]
                         #h [:div [:p {}
                                   (list
                                    (for [y x]
                                      (if (and (= 2 y) z)
                                        (with-opts! {:key "tt"} cc)
                                        (with-opts! {:key y}
                                          (template3 (inc y)))))
                                    5)]
                             (when (not z)
                               (with-opts! {:key "tt"} cc))]))

(comment
  (def cc (render! (new-root) template4 (list 1 2)))
  (update! cc template4 (list 2 1))
  (update! cc template4 (list 1 2))
  #_(def cc (render! (new-root) template4 (list 1 2)))
  (def cc (render! (new-root) template4 (list 2 3 0)))
  (update! cc template4 (list 0 1))
  #_(update! cc template4 (list 3 0 1))
  (update! cc template4 (list 2 3 0))
  (update! cc template4 (list 3 0 1))
  (def ll (atom (cycle (range 20))))
  (let [n (take 19 @ll)]
    (update! cc template4 n)
    (do (swap! ll #(drop 19 %)) nil))

  (def cc (render! (new-root) template44 (list 1 2 3) true))
  (update! cc template44 (list 1 3 2) true)
  (update! cc template44 (list 1 3 2) false)
  (update! cc template44 (list 1 2 3) true)
  )

(defn template5 [x y] #h [:p {}
                          (let [cc #h [:div "else"]]
                            (if y
                              #h [:div (with-opts! {:key 1} cc)]
                              (with-opts! {:key 1} cc)))])

(comment
  (def cc (render! (new-root) template5 3 false))
  (update! cc template5 3 false)
  (update! cc template5 3 true)
  )

#_(deftest keyedChildren
  (testing "keyed children"
    (let [comp (template4 (list 1 2))]
      (create-comp comp)
      (.log js/console @(update-comp (template4 (list 2 1)) comp)))))


(comment
  (run-tests 'ewen.inccup.incremental.core-test)

  )



(defn large [a b] #h [:div {} "e" [:p {} "e" 3] [:p {} "e" 3 [:p {} "e" 3 [:p {} "e" 3]]] [:p {} "e" 3 [:p {} "e" 3 [:p {} "e" 3 [:p {} "e" 3 [:p {} "e" 3 [:p {} "e" 3 [:p {} "e" 3 [:p {} "e" 3] [:p {} "e" 3] [:p {} "e" 3] [:p {} "e" 3] [:p {} "e" 3] [:p {} "e" 3] [:p {} "e" 3] [:p {} "e" 3] [:p {} "e" 3 [:p {} "e" 3 [:p {} "e" 3 [:p {} a 3 [:p {} "e" 3 [:p {} "e" 3 [:p {} "e" 3 [:p {} "e" 3 [:p {} "e" 3 [:p {} "e" 3 [:p {} "e" b]]]]]]]]]]]]]]]]]] [:p {} "e" 3]])



(comment
  (def cc (render! (new-root) large #h [:div 1 2 3] 3))
  (update! cc large (large (large 3 (large 5 6)) 2) (large 3 4))

  )
