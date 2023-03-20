(ns left-sidebar.utils
  (:require [cljs.pprint :as pp]
            [cljs.reader :as reader]
            [clojure.string :as str]))

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

(defn get-current-user []
  (let [user-uid (get-uid-from-localstorage)
        username (first (flatten (q '[:find ?username
                                      :in $ ?user-uid
                                      :where
                                      [?eid :user/uid ?user-uid]
                                      [?eid :user/display-page ?duid]
                                      [?duid :node/title ?username]]
                                    user-uid)))]
    username))

(defn get-all-users []
  (q '[:find ?user
       :where
       [?eid :create/user ?uid]
       [?duid :user/display-page ?user]]))

(defn get-graph-name []
  (let [url (.-href js/location)
        url-parts (str/split url #"/")]
    ;; 5 because there are 4 slashes in the url
    (nth url-parts 5)))



(comment

  ;; Functions below are not used anymore, but I'm keeping them here
  ;; because I like them.
  (defn get-starred-pages-as-hiccup []
    (let [starred-pages         (.querySelector js/document ".starred-pages")
          shortcut-links        (.-children starred-pages)
          parsed-shortcut-links (h/parse-fragment (apply str (for [link shortcut-links]
                                                               (.-outerHTML link))))
          shortcut-links-hiccup (r/as-element (map h/as-hiccup parsed-shortcut-links))]
      (pp/pprint shortcut-links-hiccup)
      shortcut-links-hiccup))


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

  ;; If we want to render the todos as  block we can use the following function
  (defn todo-item-as-block [block]
    (let [uid (:uid block)]
      (r/create-class
        {:component-did-mount
         (fn [this]
           (let [el (rd/dom-node this)]
             (-> (js/Promise.resolve
                   (.renderBlock js/roamAlphaAPI.ui.components
                                 #js {:uid uid, :el el, "zoom-path?" false}))
                 (.catch (fn [e] (js/console.error e))))))
         :component-will-unmount
         (fn [this]
           (let [el (rd/dom-node this)]
             (rd/unmount-component-at-node el)))
         :reagent-render
         (fn [_]
           [:div.block-container])}))))
