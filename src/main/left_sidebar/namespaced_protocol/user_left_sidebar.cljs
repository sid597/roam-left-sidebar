(ns left-sidebar.namespaced-protocol.user-left-sidebar
  (:require
    [cljs.tools.reader.edn :as reader]
    [left-sidebar.namespaced-protocol.collapsable :as collapsable]
    [left-sidebar.utils :as utils]
    [clojure.string :as str]
    [reagent.dom :as rd]
    [clojure.edn :as edn]
    ["@blueprintjs/core" :refer [Collapse Icon]]
    [reagent.core :as r]))




(defn custom-keyword [key]
  (println "+++" key)
  (if (.startsWith (str key) ":")
    (keyword (subs (str key) 2))
    (keyword key)))


(defn get-children-from-async-query [page-title]
  (let [query-block (str (utils/get-child-of-block-with-text-on-page "Section-children" page-title))]
    (-> (.runQuery (.-queryBuilder (.-extension (.-roamjs js/window)))
                   (str query-block))
        (.then (fn [res]
                 (println "4.async query" (js->clj res))
                (js->clj res :value-fn custom-keyword))))))


(defn get-children-from-sync-query [page-title todos]
  (println "4.---- sync query todos " page-title @todos)
  (let [query-result-block-uid (ffirst (utils/q '[:find ?block-uid
                                                  :in $ ?page-title
                                                  :where [?e :node/title ?page-title]
                                                  [?e :block/children ?e-children]
                                                  [?s-children :block/string "Reactive-query-results"]
                                                  [?s-children :block/uid ?block-uid]]
                                                page-title))
        callback               (fn [_ after]
                                 (let [input-str (aget after ":block/children" 0 ":block/string")
                                       stripped-str (subs input-str 6 (- (count input-str) 6))
                                       cljs-data (reader/read-string stripped-str)]
                                   cljs-data))]
    (println "5.query-result-block-uid" query-result-block-uid)
    (-> (get-children-from-async-query page-title)
        (.then (fn [res]
                 (reset! todos res))))

    (js/Promise.
      (fn [resolve _]
          (.addPullWatch (.-data (.-roamAlphaAPI js/window))
                         "[:block/children :block/string {:block/children ...}]"
                         (str "[:block/uid \"" query-result-block-uid "\"]")
                         (fn [before after]
                           (let [result (callback before after)]
                             (reset! todos result)
                             (resolve result))))))))

(defn get-children-from-blocks [page-title]
  (let [children-from-blocks (remove nil? (mapv (fn [raw-block-str]
                                                  (let [raw-block-str (first raw-block-str)
                                                        page?  (second (re-find #"\[\[(.+)\]\]" raw-block-str))
                                                        block? (second (re-find #"\(\((.+)\)\)" raw-block-str))]
                                                      (cond page? {:block/string page?
                                                                   :block/uid (ffirst (utils/q '[:find ?block-uid
                                                                                                 :in $ ?page-title
                                                                                                 :where [?e :node/title ?page-title]
                                                                                                 [?e :block/uid ?block-uid]]
                                                                                               (second (re-find #"\[\[(.+)\]\]" raw-block-str))))}
                                                            block? {:block/uid block?
                                                                    :block/string (ffirst (utils/q '[:find ?block-string
                                                                                                     :in $ ?block-uid
                                                                                                     :where [?e :block/uid ?block-uid]
                                                                                                     [?e :block/string ?block-string]]
                                                                                                   (second (re-find #"\(\((.+)\)\)" raw-block-str))))})))


                                             (utils/q '[:find ?s-children
                                                           :in $ ?page-title
                                                           :where [?e :node/title ?page-title]
                                                           [?e :block/children ?e-children]
                                                           [?e-children :block/string "Section-children"]
                                                           [?e-children :block/children ?s-e-children]
                                                           [?s-e-children :block/string ?s-children]]
                                                      page-title)))]
    (println "4.children-from-blocks" children-from-blocks)
    children-from-blocks))


(defn section-child-item [block]
  (let [string (or(:block/string block) (get block ":block/string"))

        uid    (or (:block/uid block)(get block ":block/uid"))
        todo-block? (utils/is-todo-block? string)]
    (println "===" block string uid todo-block?)

    [:a {:href (str "/#/app/" (utils/get-graph-name) "/page/" uid)
         :style {:text-decoration "none"}}
     [:div {:class (str "section-child-item " uid)
            :style {:padding "4px 0 4px 4px"
                    :color "hsl(204,20%,45%)"
                    :font-family "Inter"
                    :line-height "1.5"
                    :font-size "15px"
                    :font-weight "500"
                    :border-radius "3px"}}
      (if todo-block?
        [:<>
         [:input {:type "checkbox"
                  :style {:margin-right "8px"
                          :margin-left "4px"
                          :accent-color "black"}

                  :on-click #(.updateBlock (.-roamAlphaAPI js/window) (clj->js {:block
                                                                                {:uid uid
                                                                                 :string (str/replace string "{{[[TODO]]}}" "{{[[DONE]]}}")}}))}]
         (-> string
             (str/replace "{{[[TODO]]}}" " "))]
        string)]]))

(defn section-child-component [page-title]
  (let [[results-from how-many] (mapv first (utils/q '[:find ?results-from
                                                       :in $ ?page-title
                                                       :where [?e :node/title ?page-title]
                                                       [?e :block/children ?e-children]
                                                       [?e-children :block/string "Settings"]
                                                       [?e-children :block/children ?s-children]
                                                       (or (and [?s-children :block/string "Show results from"]
                                                                [?s-children :block/children ?s-c-children]
                                                                [?s-c-children :block/string ?results-from])
                                                           (and [?s-children :block/string "Show how many results?"]
                                                                [?s-children :block/children ?s-c-children]
                                                                [?s-c-children :block/string ?results-from]))]
                                                     page-title))
        children                  (r/atom nil)
        on-success                (fn [res]
                                    (println "on success" res)
                                    (reset! children res))]
    (println "3.results from:" results-from)
    (cond (= results-from
             "sync query")  (-> (get-children-from-sync-query page-title children)
                                (.then on-success))
          #_#_(= results-from
                 "async query") (-> (get-children-from-async-query page-title)
                                    (.then on-success))
          (= results-from
             "blocks")      (on-success (get-children-from-blocks page-title)))


    (fn []
      [:div {:class (str "left-sidebar-section children" page-title)}
         (when @children
           (for [child @children
                 _ (do (println "6.child" child) nil)]
             ^{:key (or (:block/uid child)
                        (get child ":block/uid"))}
             [section-child-item child]))])))

;(section-child-component "sid/left-sidebar/personal-shortcuts")


(defn get-user-left-sidebar-list [username]
  (let [sidebar-section-titles (utils/get-page-name-from-ref (map first (utils/q '[:find ?children
                                                                                   :in $ ?username
                                                                                   :where [?e :node/title ?username]
                                                                                   [?e :block/children ?e-section]
                                                                                   [?e-section :block/string "Sections"]
                                                                                   [?e-section :block/children ?e-children]
                                                                                   [?e-children :block/string ?children]
                                                                                   [(not= "" (clojure.string/trim ?children))]]
                                                                                 (str username "/left-sidebar"))))]

    (cljs.pprint/pprint  (mapv #(section-child-component %) sidebar-section-titles))
    (println "1.section-titles" sidebar-section-titles  (count sidebar-section-titles))
    ;[section-child-component "sid/left-sidebar/personal-shortcuts"]
    (for [section-title sidebar-section-titles
          :let [section-title-for-comp (last (str/split  section-title #"/"))
                _ (do (println "2.section-title" section-title-for-comp) nil)]]
      ^{:key section-title}
      [collapsable/collapsable-section section-title-for-comp nil
       [section-child-component section-title]])))


;(get-user-left-sidebar-list "sid")


