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

(defn get-settings-for-section [section-uid]
  (let [settings-uid-data (utils/get-child-block-with-text section-uid "Settings")
        transformed-data (transform-data settings-uid-data)
        actions-to-take  {:Actions (transform-data (utils/get-child-block-with-text (:uid settings-uid-data) "Actions"))}
        general-settings (transform-data (utils/get-block-uid-for-block-on-page
                                           "General settings"
                                           (str (utils/get-current-user) "/left-sidebar")))
        section-settings (merge transformed-data actions-to-take)
        all-settings     (merge general-settings section-settings)]
    (println "all settings -------------------")
    (cljs.pprint/pprint all-settings)
    {:type           (or (:Type all-settings)
                         "blocks-and-pages")
     :show           (or (when (:Show all-settings)
                           (or (utils/str-to-num (:Show all-settings))
                               1000)
                           5))
     :truncate?      (or (when (:Truncate-result? all-settings)
                           (or (utils/str-to-num (:Truncate-result? all-settings))
                               1000))
                         75)
     :refresh-time   (or (utils/get-milliseconds-from (:Refresh-every all-settings))
                         0)
     :re-organisable? (utils/str-to-bool (:Re-organisable? all-settings))

     :collapsable?     (utils/str-to-bool (:Collapsable? all-settings))

     :actions        (or (:Actions all-settings)
                       {})
     :open?          (r/atom (utils/str-to-bool (:Open? all-settings)))}))



(get-settings-for-section "eAbCXyZDp")

(defn section-child-item [block settings]
  (let [string (or(:string block) (get block ":block/string"))

        uid    (or (:uid block)(get block ":block/uid"))
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

                  :on-click #(.updateBlock (.-roamAlphaAPI js/window)
                                           (clj->js {:block
                                                     {:uid uid
                                                      :string (str/replace string "{{[[TODO]]}}" "{{[[DONE]]}}")}}))}]

         (-> string
             (str/replace "{{[[TODO]]}}" " ")
             (utils/truncate-str truncate-length))]
        (utils/truncate-str string truncate-length))]]))

(defn section-component [settings section-uid children]
  (let [is-open?     (:open? settings)
        is-open-uid  (utils/get-child-of-child-under-block section-uid "Settings" (str "Open?: " @is-open?))
        collapsable? (:collapsable? settings)
        show         (:show settings)
        section-title (first (first (utils/q '[:find ?title
                                               :in $ ?section-uid
                                               :where
                                               [?section-eid :block/uid ?section-uid]
                                               [?section-eid :block/string ?title]]
                                             section-uid)))
        waiting? (r/atom false)
        click-count (r/atom 0)
        click-timeout (r/atom nil)]
    (fn [settings section-uid children]
      (println "open " is-open-uid "==="(str "Open?: " @is-open?))
      [:div {:class "collapsable-section"
             :style {:margin-bottom "15px"}}
       [:div {:class "but"
              :on-click (fn []
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
                                            (println "single clicked")
                                            (.updateBlock (.-roamAlphaAPI js/window)
                                                          (clj->js {:block
                                                                    {:uid (:uid is-open-uid)
                                                                     :string (str "Open?: " (not @is-open?))}}))
                                            (reset! is-open? (not @is-open?)))

                                          ;; double click handler
                                          (do
                                            (println "double click")
                                            (-> (.openBlock (.-mainWindow (.-ui (.-roamAlphaAPI js/window)))
                                                            (clj->js {:block
                                                                      {:uid section-uid}}))
                                                (.then (println "double clicked left sidebar section")))))
                                        (reset! click-count 0)
                                        (reset! waiting? false)))
                                    250)))


               :style {:display "flex"
                       :align-items "center"
                       :background "transparent"
                       :border "none"
                       :cursor "pointer"
                       :color "#CED9E0"
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
         [:span section-title]
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
          children]
         children)])))



(defn get-children-for-section [section-uid section-settings callback]
  (let [type (:type section-settings)
        child-block (utils/get-child-block-with-text section-uid
                                                     (if (= "query" type)
                                                       "Query block"
                                                       "Children"))]
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



(defn left-sidebar-sections []
  (let [section-uids (utils/get-left-sidebar-section-uids-for-current-user)
        section-children (->> section-uids
                              (map (fn [uid] [uid (r/atom nil)]))
                              (into {}))]
    (doseq [section-uid section-uids]
      (let [section-settings (get-settings-for-section section-uid)
            children-atom (get section-children section-uid)]
        (get-children-for-section section-uid section-settings (fn [res]
                                                                 (println "section uid " section-uid " ---res---" res)
                                                                 (reset! children-atom res)))))

    (js/setInterval (fn []
                      #_(println "*****section uid***** ")
                      (let [section-uid "O84zhInr1"
                            section-settings (get-settings-for-section "O84zhInr1")]
                        (get-children-for-section section-uid section-settings (fn [res]
                                                                                 (reset! (get section-children "O84zhInr1") res)))))
                                                                                 ;(println "*****section uid***** " (get section-children "O84zhInr1") " ---res---" res)))))

                   2000)
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
                                                      (:uid child))}
                                           [:div [section-child-item child section-settings]]))]
                 [:div {:key section-uid}
                  ;@children-list
                  [section-component section-settings
                   section-uid
                   @children-list]])
             ;[section-child-item child section-settings]))]
             [:div {:key section-uid} "Loading..."])))])))


(defn left-sidebar []
  (let [atoms {:a (r/atom "q")
               :b (r/atom "gg")}
        kw [:a :b]]
    (js/setInterval (fn []
                      (reset! (get atoms :a) (str "a" (rand-int 1000)))))
    (fn []
      [:<>
       (doall
         (for [k kw
               :let [atom @(get atoms k)]]
           [:div (str "hello" atom)]))])))


(left-sidebar-sections)