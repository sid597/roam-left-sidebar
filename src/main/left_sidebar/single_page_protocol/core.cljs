(ns left-sidebar.single-page-protocol.core
  (:require
    [clojure.string :as str]
    [left-sidebar.utils :as utils]
    ["@blueprintjs/core" :refer [Collapse Icon]]
    [reagent.core :as r]))


(defn transform-data [input-data]
  (let [children (get input-data :children)
        key-value-pairs (map (fn [child]
                               (let [split-string (clojure.string/split (get child :string) #": ")]
                                 [(keyword (clojure.string/trim (first split-string))) (second split-string)]))
                             children)]

    (into {} key-value-pairs)))

(defn default-config []
  {:Re-organisable? "false"
   :Actions {}
   :Refresh-every "0"
   :Open? "false"
   :Show "5"
   :Type "blocks-and-pages"
   :Collapsable? "true"
   :Truncate-result? "75"})

(defn get-settings-for-section [section-uid]
  (let [settings-uid-data (utils/get-child-block-with-text section-uid "Settings")
        transformed-data (transform-data settings-uid-data)
        actions-to-take {:Actions (transform-data (utils/get-child-block-with-text (:uid settings-uid-data) "Actions"))}
        general-settings (transform-data (utils/get-block-uid-for-block-on-page
                                           "General settings"
                                           (str (utils/get-current-user) "/left-sidebar")))
        section-settings (merge transformed-data actions-to-take)
        all-settings     (merge (default-config) general-settings section-settings)]
    {:type           (:Type all-settings)
     :show           (when (:Show all-settings)
                       (or (utils/str-to-num (:Show all-settings))
                           1000))
     :truncate?      (when (:Truncate-result? all-settings)
                          (or (utils/str-to-num (:Truncate-result? all-settings))
                              1000))
     :refresh-time   (utils/get-milliseconds-from (:Refresh-every all-settings))
     :re-organisable? (utils/str-to-bool (:Re-organisable? all-settings))

     :collapsable?     (utils/str-to-bool (:Collapsable? all-settings))

     :actions        (:Actions all-settings)
     :open?          (r/atom (utils/str-to-bool (:Open? all-settings)))}))

(defn parse-str-for-refs [block-string]
  ;#_(println "LS: parse str for refs" block-string)
  (cond
    (re-matches #"\(\(\s*([^)]+)\s*\)\)" block-string) {:uid (second (re-find #"\(\(\s*([^)]+)\s*\)\)" block-string))}
    (re-matches #"\[\[(.*)\]\]" block-string) {:page (second (re-find #"\[\[(.*)\]\]" block-string))}
    :else {:string block-string}))

(defn section-child-item [block settings]
  (let [string (or(:string block)
                  (get block ":block/string")
                  (get block "text"))
        parsed-str (parse-str-for-refs string)
        uid    (cond
                 (get parsed-str :uid) (get parsed-str :uid)
                 (get parsed-str :page) (ffirst (utils/q '[:find ?uid
                                                           :in $ ?page
                                                           :where [?e :node/title ?page]
                                                           [?e :block/uid ?uid]]
                                                         (get parsed-str :page)))
                 :else (or (:uid block)
                           (get block ":block/uid")
                           (get block "uid")))
        render-str (cond (:uid parsed-str) (utils/get-block-string  (:uid parsed-str))
                         (:page parsed-str) (:page parsed-str)
                         :else string)
        todo-block? (utils/is-todo-block? string)
        truncate-length (:truncate? settings)]
    [:a {:href (str "/#/app/" (utils/get-graph-name) "/page/" uid)
         :style {:text-decoration "none"}
         :on-click (fn [event]
                     (when (.-shiftKey event)
                       (do
                         (.preventDefault event)
                         (.addWindow (.-rightSidebar (.-ui (.-roamAlphaAPI js/window))) (clj->js {:window {:type "outline"
                                                                                                           "block-uid" (str uid)}})))))}
     [:div {:class (str "section-child-item " uid)
            :style {:padding "4px 0 4px 4px"
                    :color "#495057"
                    :line-height "1.5"
                    :border-radius "3px"}}
      (if todo-block?
        [:<>
         [:input {:type "checkbox"
                  :style {:margin-right "8px"
                          :margin-left "4px"
                          :accent-color "black"}

                  :on-click #(.updateBlock (.-roamAlphaAPI js/window)
                                           (clj->js {:block
                                                     {:uid uid
                                                      :string (str/replace render-str"{{[[TODO]]}}" "{{[[DONE]]}}")}}))}]

         (-> render-str
             (str/replace "{{[[TODO]]}}" " ")
             (utils/truncate-str truncate-length))]
        (utils/truncate-str render-str truncate-length))]]))

(defn section-component [settings section-uid children]
  (let [is-open?     (:open? settings)
        is-open-uid  (utils/get-child-of-child-under-block section-uid "Settings" (str "Open?: " @is-open?))
        collapsable? (:collapsable? settings)
        section-title (ffirst (utils/q '[:find ?title
                                         :in $ ?section-uid
                                         :where
                                         [?section-eid :block/uid ?section-uid]
                                         [?section-eid :block/string ?title]]
                                       section-uid))
        parsed-title (parse-str-for-refs section-title)
        render-str (cond (:uid parsed-title) (utils/get-block-string  (:uid parsed-title))
                         (:page parsed-title) (:page parsed-title)
                         :else section-title)
        uid    (cond
                (get parsed-title :uid) (get parsed-title :uid)
                (get parsed-title :page) (ffirst (utils/q '[:find ?uid
                                                            :in $ ?page
                                                            :where [?e :node/title ?page]
                                                            [?e :block/uid ?uid]]
                                                          (get parsed-title :page)))
                :else section-uid)
        waiting? (r/atom false)
        click-count (r/atom 0)
        click-timeout (r/atom nil)]
    (fn [settings section-uid children]
      [:div {:class "collapsable-section"}
       [:div {:class "sidebar-title-button"
              :on-click (fn [e]
                          (when (.-shiftKey e)
                            (do
                              (.preventDefault e)
                              (.addWindow (.-rightSidebar (.-ui (.-roamAlphaAPI js/window))) (clj->js {:window {:type "outline"
                                                                                                                "block-uid" (str uid)}}))))
                          (swap! click-count inc)
                          (reset! waiting? true)
                          (when @click-timeout
                            (js/clearTimeout @click-timeout))
                          (reset! click-timeout
                                  (js/setTimeout
                                    (fn []
                                      (when @waiting?
                                        (if (= @click-count 1)
                                          ;; Single click handler
                                          (when (and collapsable? is-open-uid)
                                            #_(println "LS: single clicked")
                                            (.updateBlock (.-roamAlphaAPI js/window)
                                                          (clj->js {:block
                                                                    {:uid (:uid is-open-uid)
                                                                     :string (str "Open?: " (not @is-open?))}}))
                                            (reset! is-open? (not @is-open?)))

                                          ;; double click handler
                                          (do
                                            #_(println "LS: double click")
                                            (-> (.openBlock (.-mainWindow (.-ui (.-roamAlphaAPI js/window)))
                                                            (clj->js {:block
                                                                      {:uid uid}}))
                                                (.then (println "LS: double clicked left sidebar section")))))
                                        (reset! click-count 0)
                                        (reset! waiting? false)))
                                    250)))


               :style {:display "flex"
                       :align-items "center"
                       :background "transparent"
                       :border "none"
                       :cursor "pointer"
                       :font-size "14px"
                       :font-weight "600"
                       :padding "4px 0"
                       :width "100%"
                       :outline "none"
                       :transition "color 0.2s ease-in"}}

        [:div {:style {:display "flex"
                       :align-items "center"
                       :justify-content "space-between"
                       :width "100%"}}
         [:span render-str]
         (when (and collapsable?
                    (not-empty children))
          [:span (if @is-open? [:> Icon {:icon "chevron-down"}]
                              [:> Icon {:icon "chevron-right"}])])]]
       [:hr {:style {:margin-bottom "4px"
                     :margin-top "2px"
                     :border "1px solid #CED9E0"
                     :border-radius "5px"}}]
       (if collapsable?
         [:> Collapse {:is-open @is-open?}
          (take (:show settings) children)]
         (take (:show settings) children))])))

(defn get-children-for-section [section-uid section-settings callback]
  #_(println "LS: get-children-for-section" section-uid section-settings)
  (let [type (:type section-settings)
        child-block (utils/get-child-block-with-text section-uid
                                                     (if (= "query" type)
                                                       "Query block"
                                                       "Children"))]
    #_(println "LS: child block for section" section-uid child-block)
    (cond (= "query" type)
          (do
            (-> (utils/query-builder-run-query (-> child-block
                                                   :children
                                                   first
                                                   :uid))
                (.then callback)))
          :else (do
                  (callback (or (:children child-block)
                                []))))))

(defn- process-section-uids [section-uids section-children]
  (keep (fn [section-uid]
          (let [section-settings (get-settings-for-section section-uid)
                children-atom (get section-children section-uid)
                rt (:refresh-time section-settings)]
            (when (and rt (not= rt 0))
              (fn []
                (js/setInterval
                  (fn []
                    (get-children-for-section section-uid section-settings (fn [res]
                                                                             (reset! children-atom res))))
                  rt)))))
        section-uids))

(defn create-action-fns [section-uids]
  (keep (fn [section-uid]
          (let [section-settings (get-settings-for-section section-uid)
                actions (:actions section-settings)
                cmd-pallet (:command-pallet actions)
                context-menu (:context-menu actions)]

            (fn []
              (when (and (not-empty actions)
                         (not= (:type section-settings) "query"))
               (do
                 (when cmd-pallet
                   (utils/add-command-to-command-pallet section-uid cmd-pallet))
                 (when context-menu
                   (utils/add-command-in-context-menu-for-section section-uid context-menu)))))))
        section-uids))

(defn load-section-children [section-uids section-children]
  (doseq [section-uid section-uids]
    (let [section-settings (get-settings-for-section section-uid)
          children-atom (get section-children section-uid)]
      (get-children-for-section section-uid section-settings (fn [res]
                                                               (reset! children-atom res))))))

(defn left-sidebar-sections [section-uids]
  (let [section-children (->> section-uids
                              (map (fn [uid] [uid (r/atom nil)]))
                              (into {}))]
    (load-section-children section-uids section-children)

    (let [processed-fns (process-section-uids section-uids section-children)]
      (doseq [f processed-fns]
        (f)))

    (let [action-fns (create-action-fns section-uids)]
      #_(cljs.pprint/pprint action-fns)
      (doseq [f action-fns]
        (f)))

    (fn []
      [:div {:class "left-sidebar-sections"}
       (doall
         (for [section-uid section-uids
               :let [section-settings (get-settings-for-section section-uid)
                     children-atom (get section-children section-uid)
                     s-children @children-atom]]
           (if s-children
             (let [children-list (r/atom (for [child s-children]
                                           ^{:key (or (:block/uid child)
                                                      (get child ":block/uid")
                                                      (:uid child)
                                                      (get child "uid"))}
                                           [:div [section-child-item child section-settings]]))]
               [:div {:key section-uid}
                ;@children-list
                [section-component section-settings
                 section-uid
                 @children-list]])
             ;[section-child-item child section-settings]))]
             [:div {:key section-uid} "Loading..."])))])))
