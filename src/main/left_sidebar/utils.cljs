(ns left-sidebar.utils
  (:require [cljs.pprint :as pp]
            [cljs.reader :as reader]
            [hickory.core :as h]
            [hickory.select :as h-select]
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


(defn get-personal-left-sidebar-page-uid [username]
  (first (flatten (q '[:find ?page-uid
                       :in $ ?username
                       :where
                       [?eid :node/title ?username]
                       [?eid :block/uid ?page-uid]]
                     (str username "/left-sidebar")))))

(defn get-my-todos-page-uid [username]
  (first (flatten (q '[:find ?page-uid
                       :in $ ?username
                       :where
                       [?eid :node/title ?username]
                       [?eid :block/uid ?page-uid]]
                     (str username "/left-sidebar/my-todos")))))

(defn get-block-uid-for-block-on-page [block-text page-title]
  (ffirst (q '[:find (pull ?block-eid [:block/string :block/uid {:block/children ...}])
                     :in $ ?block-text ?page-title
                     :where
                     [?page-eid :node/title ?page-title]
                     [?page-eid :block/children ?block-eid]
                     [?block-eid :block/string ?block-text]
                     [?block-eid :block/uid ?block-uid]]
                   block-text page-title)))
(defn get-child-of-block-with-text-on-page [block-text page-title]
  (first (flatten (q '[:find ?child-uid
                       :in $ ?block-text ?page-title
                       :where
                       [?page-eid :node/title ?page-title]
                       [?page-eid :block/children ?block-uid]
                       [?block-uid :block/string ?block-text]
                       [?block-uid :block/children ?child-eid]
                       [?child-eid :block/uid ?child-uid]]
                     block-text page-title))))

(defn get-page-name-from-ref [list-of-pages]
  (remove nil? (map (fn [s]
                      (println s)
                      (second (re-find #"\[\[(.+)\]\]" s)))
                    list-of-pages)))


(defn is-todo-block? [s]
  (let [first-word (first (clojure.string/split s #"\s+"))]
    (= first-word "{{[[TODO]]}}")))

(defn get-child-block-with-text [parent-uid child-text]
  (let [result (ffirst (q '[:find (pull ?ce [:block/string :block/uid {:block/children [:block/uid :block/order :block/string]}])
                            :in $ ?parent-uid ?child-text
                            :where
                            [?e :block/uid ?parent-uid]
                            [?e :block/children ?ce]
                            [?ce :block/string ?child-text]]
                          parent-uid child-text))]
    (if result
      (assoc result :children (sort-by :order (:children result)))
      nil)))
(get-child-block-with-text "en6IBo3by" "Children")


(defn get-child-of-child-under-block [block-uid child-text child-child-text]
  (println "block-uid" block-uid "child-text" child-text "child-child-text" child-child-text)
  (ffirst (q '[:find (pull ?cce [:block/string :block/uid])
                     :in $ ?block-uid ?child-text ?child-child-text
                     :where [?e :block/uid ?block-uid]
                            [?e :block/children ?ce]
                            [?ce :block/string ?child-text]
                            [?ce :block/children ?cce]
                            [?cce :block/string ?child-child-text]]
                   block-uid child-text child-child-text)))

(get-child-of-child-under-block "O84zhInr1" "Settings" "Open?: True")

(defn get-left-sidebar-section-uids-for-current-user []
  (let [username (get-current-user)]
    (println "username for user is" username)
    (->> (q '[:find ?section-uid ?order
              :in $ ?user-left-sidebar
              :where
              [?eid :node/title ?user-left-sidebar]
              [?eid :block/children ?section-eid]
              [?section-eid :block/string "Sections"]
              [?section-eid :block/children ?section-children-eid]
              [?section-children-eid :block/uid ?section-uid]
              [?section-children-eid :block/order ?order]]
            (str username "/left-sidebar"))
         (sort-by second)
         (mapv first))))

(defn get-children-for-eid [eid]
  (ffirst (q '[:find (pull ?e [:block/string :block/uid {:block/children ...}])
                     :in $ ?e]
                    eid)))
;(get-children-for-eid (first (get-left-sidebar-section-uids-for-current-user)))
(get-left-sidebar-section-uids-for-current-user)

(defn str-to-num [s]
  (let [n (js/parseInt s)]
    (if (js/isNaN n)
      nil
      n)))


(defn str-to-bool [s]
  (cond
    (= s "true") true
    (= s "false") false
    :else nil))
(defn parse-time [time-str]
  (let [time-regex #"(\d+)([smhd])"
        matches (re-seq time-regex time-str)]
    (reduce (fn [acc match]
              (let [value (js/parseInt (second match))
                    unit (last match)]
                (assoc acc unit value)))
            {}
            matches)))

(defn get-milliseconds-from [time-str]
  (let [time-map (parse-time time-str)
        seconds (get time-map "s" 0)
        minutes (get time-map "m" 0)
        hours (get time-map "h" 0)
        days (get time-map "d" 0)]
    (+ (* seconds 1000)
       (* minutes 60 1000)
       (* hours 60 60 1000)
       (* days 24 60 60 1000))))

(defn truncate-str
  [s n]
  (let [sl (count s)]
    (if (> sl n)
      (str (subs s 0  n) "...")
      s)))
(truncate-str "hello world" 53)

(defn pull-children [uid]
  (ffirst (q '[:find (pull ?e [:block/string {:block/children ...}])
                     :in $ ?uid
                     :where
                     [?e :block/uid ?uid]]
                   uid)))
(defn custom-keyword [key]
  (println "+++" key)
  (if (.startsWith (str key) ":")
    (keyword (subs (str key) 2))
    (keyword key)))

(defn query-builder-run-query [query-block]
  (-> (.runQuery (.-queryBuilder (.-extension (.-roamjs js/window)))
                 (str query-block))
      (.then (fn [res]
               (js->clj res :value-fn custom-keyword)))))


(defn get-block-string [uid]
  (ffirst (q '[:find ?string
                     :in $ ?uid
                     :where
                     [?e :block/uid ?uid]
                     [?e :block/string ?string]]
                   uid)))


(defn add-command-in-context-menu-for-section [section-uid label]
  (let [section-title   (get-block-string section-uid)
        child-block-uid (:uid (get-child-block-with-text section-uid "Children"))]
    (println "section-title" section-title child-block-uid section-title)
    (js/roamAlphaAPI.ui.blockContextMenu.addCommand
      #js {:label label
           :display-conditional (fn [block-context] true) ;; You can modify this function to determine when the command should be displayed
           :callback (fn [block-context]
                       (cljs.pprint/pprint block-context)
                       (let [block-context-clj (js->clj block-context :keywordize-keys true)
                             block-to-add-uid   (:block-uid block-context-clj)

                             title-of-page-to-add (ffirst (q '[:find ?page-title
                                                                :in $ ?page-uid
                                                                :where
                                                                [?page-eid :block/uid ?page-uid]
                                                                [?page-eid :block/string ?page-title]]
                                                              block-to-add-uid))]
                         (.createBlock (.-roamAlphaAPI js/window)
                                       (clj->js {:location
                                                 {:parent-uid child-block-uid
                                                  :order "last"}
                                                 :block {:string (str "((" block-to-add-uid "))") }}))
                         #_(js/console.log "Command added in context menu!" block-context)))})))
(defn get-current-page []
     (-> (js/roamAlphaAPI.ui.mainWindow.getOpenPageOrBlockUid)
         (.then (fn [uid]
                  (println "current page uid"uid )
                  (or {:page (ffirst (q '[:find ?page-title
                                          :in $ ?page-uid
                                          :where
                                          [?page-eid :block/uid ?page-uid]
                                          [?page-eid :node/title ?page-title]]
                                       uid))
                       :block-page uid})))))

(defn add-command-to-command-pallet [section-uid label]
  (let [section-title (get-block-string section-uid)
        child-block-uid (:uid (get-child-block-with-text section-uid "Children"))]
    (js/roamAlphaAPI.ui.commandPalette.addCommand
      #js {:label label
           :callback (fn []
                       (println "child block uid" child-block-uid)
                       (-> (get-current-page)
                           (.then (fn [current-page]
                                    (println "current page"  current-page)
                                    (.createBlock (.-roamAlphaAPI js/window)
                                                  (clj->js {:location
                                                            {:parent-uid child-block-uid
                                                             :order "last"}
                                                            :block {:string (cond
                                                                              (:page
                                                                                current-page) (str "[[" (:page current-page) "]]")
                                                                              :else           (str "((" (:block-page current-page) "))"))}}))))))})))


(defn get-global-left-sidebar-uids []
  (->> (q '[:find ?section-uid ?order
            :in $ ?user-left-sidebar
            :where
            [?eid :node/title ?user-left-sidebar]
            [?eid :block/children ?section-eid]
            [?section-eid :block/string "Global sections"]
            [?section-eid :block/children ?section-children-eid]
            [?section-children-eid :block/uid ?section-uid]
            [?section-children-eid :block/order ?order]]
          "roam/left-sidebar")
       (sort-by second)
       (mapv first)))

(comment

  ;; Functions below are not used anymore, but I'm keeping them here
  ;; because I like them.
  (defn get-starred-pages-as-hiccup []
    (let [starred-pages         (.querySelector js/document ".starred-pages")
          shortcut-links        (.-children starred-pages)
          parsed-shortcut-links (h/parse-fragment (apply str (for [link shortcut-links]
                                                               (.-outerHTML link))))
          shortcut-links-hiccup (r/as-element (map h/as-hiccup parsed-shortcut-links))]
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
