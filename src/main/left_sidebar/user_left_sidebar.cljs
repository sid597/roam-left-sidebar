(ns left-sidebar.user-left-sidebar
  (:require
    [cljs.tools.reader.edn :as reader]
    [left-sidebar.utils :as utils]
    [clojure.string :as str]
    [clojure.edn :as edn]
    ["@blueprintjs/core" :refer [Collapse Icon]]
    [reagent.core :as r]))


(defn get-page-name-from-ref [list-of-pages]
  (remove nil? (map (fn [s]
                        (println s)
                        (second (re-find #"\[\[(.+)\]\]" s)))
                    list-of-pages)))

(defn custom-keyword [key]
  (println "+++" key)
  (if (.startsWith (str key) ":")
    (keyword (subs (str key) 2))
    (keyword key)))


(defn get-children-from-async-query [page-title]
  (let [query-block (str (utils/get-child-of-block-with-text-on-page "Query Builder" page-title))]
    (-> (.runQuery (.-queryBuilder (.-extension (.-roamjs js/window)))
                   (str query-block))
        (.then (fn [res]
                 (println "res " (first res))
                 (println "async query" (js->clj res))
                (js->clj res :value-fn custom-keyword))))))


(defn get-children-from-sync-query [page-title todos]
  (println "----"@todos)
  (let [query-result-block-uid (first (first (utils/q '[:find ?block-uid
                                                        :in $ ?page-title
                                                        :where [?e :node/title ?page-title]
                                                        [?e :block/children ?e-children]
                                                        [?s-children :block/string "Reactive query results"]
                                                        [?s-children :block/uid ?block-uid]]
                                                      page-title)))
        callback               (fn [_ after]
                                 (let [input-str (aget after ":block/children" 0 ":block/string")
                                       stripped-str (subs input-str 6 (- (count input-str) 6))
                                       cljs-data (reader/read-string stripped-str)]
                                   (println "sync query" cljs-data)
                                   cljs-data))]
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



(defn get-children-from-blocks [page-title])

(defn get-page-data [page-title]
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
        get-section-children  (cond (= results-from
                                       "sync query")  ()
                                    (= results-from
                                       "async query") ()
                                    (= results-from
                                       "blocks")             ())]

    (println "show-results-from" results-from "--" how-many)))



(get-page-data "sid/left-sidebar/my todos")


(defn create-sidebar-component-for-page [])

(defn get-user-left-sidebar-list [username]
  (let [sidebar-section-titles (get-page-name-from-ref (map first (utils/q '[:find ?children
                                                                             :in $ ?username
                                                                             :where [?e :node/title ?username]
                                                                             [?e :block/children ?e-children]
                                                                             [?e-children :block/string ?children]
                                                                             [(not= "" (clojure.string/trim ?children))]]
                                                                           (str username "/left-sidebar"))))]
    (println "section-titles" sidebar-section-titles (str username "/left-sidebar") (count sidebar-section-titles))))


