(ns left-sidebar.utils
  (:require [cljs.pprint :as pp]
            [cljs.reader :as reader]))

(defn q
  ([query]
   (let [serialised-query (pr-str query)
         roam-api         (.-data (.-roamAlphaAPI js/window))
         q-fn             (.-q roam-api)]
     (-> (.apply q-fn roam-api (array serialised-query))
         (js->clj :keywordize-keys true))))
  ([query & args]
   (let [serialised-query (pr-str query)
         roam-api         (.-data (.-roamAlphaAPI js/window))
         q-fn             (.-q roam-api)]
     (-> (.apply q-fn roam-api (apply array (concat [serialised-query] args)))
         (js->clj :keywordize-keys true)))))

(defn get-all-users []
  (q '[:find ?user
       :where
       [?eid :create/user ?uid]
       [?duid :user/display-page ?user]]))

(defn get-uid-from-localstorage []
  (let [data (nth (reader/read-string
                    (.getItem (.-localStorage js/window) "globalAppState"))
                  6)]
    (loop [remaining data
           prev nil]
      (when (seq remaining)
        (if (= "~:uid" prev)
          (first remaining)
          (recur (rest remaining) (first remaining)))))))


;; Used the following before I knew `:user/uid` existed
(defn get-username []
  ;; Create a random page, and find out who created it, that's the user.
  (let [random-page-name "oeiarlnst89342lhnw8"]
    (-> (.createPage (.-roamAlphaAPI js/window)
                     (clj->js {:page {:title random-page-name}}))
        (.then (fn []
                 (let [[[username ruid]]  (q '[:find ?username ?ruid
                                               :in $ ?random-page-name
                                               :where
                                               [?eid :node/title ?random-page-name]
                                               [?eid :block/uid ?ruid]
                                               [?eid :create/user ?uid]
                                               [?uid :user/display-page ?duid]
                                               [?duid :node/title ?username]]
                                             random-page-name)]

                   (-> (.deletePage (.-roamAlphaAPI js/window) (clj->js {:page {:uid ruid}}))
                       (.then (fn []
                                (println "Page deleted successfully!"))))

                   username))))))

(comment
  (defn get-starred-pages-as-hiccup []
    (let [starred-pages         (.querySelector js/document ".starred-pages")
          shortcut-links        (.-children starred-pages)
          parsed-shortcut-links (h/parse-fragment (apply str (for [link shortcut-links]
                                                               (.-outerHTML link))))
          shortcut-links-hiccup (r/as-element (map h/as-hiccup parsed-shortcut-links))]
      (pp/pprint shortcut-links-hiccup)
      shortcut-links-hiccup)))
