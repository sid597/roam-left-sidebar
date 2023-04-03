(ns left-sidebar.personal-shortcuts
  (:require [left-sidebar.utils :as utils]
            ["@blueprintjs/core" :refer [Collapse Icon]]))


(defn create-personal-shortcuts-page []
  (let [username               (utils/get-current-user)
        personal-shortcuts-uid (utils/get-personal-shortcut-page-uid username)]
       (when-not personal-shortcuts-uid
         (-> (.createPage (.-roamAlphaAPI js/window)
                          (clj->js {:page
                                    {:title (str username "/left-sidebar/personal-shortcuts")}}))
             (.then (fn []
                      (let [new-shortcut-page-uid (utils/get-personal-shortcut-page-uid username)]
                        (js/console.log "Personal shortcuts page created" new-shortcut-page-uid)
                        (-> (.createBlock (.-roamAlphaAPI js/window)
                                          (clj->js {:block
                                                     {:string ""}
                                                    :location {:parent-uid new-shortcut-page-uid
                                                               :order 0}}))))))))))



(defn get-personal-shortcuts-for-user [username]
  (->> (utils/q '[:find (pull ?shortcut-page-eid [:block/uid :node/title])
                  :in $ ?page-name
                  :where
                  [?user-page-uid :node/title ?page-name]
                  [?user-page-uid :block/children ?block-uid]
                  [?block-uid :block/string ?shortcut-page-name]
                  [?shortcut-page-eid :node/title ?shortcut-page-name]]
                (str username "/left-sidebar/personal-shortcuts"))
       (mapv first)))

(defn personal-shortcut-item [shortcut]
  [:a {:href (str "/#/app/" (utils/get-graph-name) "/page/" (:uid shortcut))
       :style {:text-decoration "none"}}
   [:div {:class "personal-shortcut-item"
          :style {:padding "4px 0 4px 4px "
                  :color "hsl(204,20%,45%)"
                  :font-family "Inter"
                  :line-height "1.5"
                  :font-size "15px"
                  :font-weight "500"
                  :border-radius "3px"}}
    (:title shortcut)]])

(defn personal-shortcuts-component [shortcuts]
  [:div {:class "personal-shortcuts"}
   (for [shortcut @shortcuts]
     ^{:key (:uid shortcut)}
     [personal-shortcut-item shortcut])])

(defn add-personal-shortcut-command-in-menu []
  (let [username                         (utils/get-current-user)
        personal-shortcuts-page-uid (utils/get-personal-shortcut-page-uid username)]
   (js/Promise.
     (fn [resolve _]
       (resolve
         (-> (js/roamAlphaAPI.ui.blockContextMenu.addCommand
               #js {:label "Add current page to personal shortcut"
                    :display-conditional (fn [block-context] true) ;; You can modify this function to determine when the command should be displayed
                    :callback (fn [block-context]
                                (let [block-context-clj (js->clj block-context :keywordize-keys true)
                                      page-to-add-uid   (:page-uid block-context-clj)
                                      [page-to-add-title] (first (utils/q '[:find ?page-title
                                                                            :in $ ?page-uid
                                                                            :where
                                                                            [?page-eid :block/uid ?page-uid]
                                                                            [?page-eid :node/title ?page-title]]
                                                                          page-to-add-uid))]
                                  (.createBlock (.-roamAlphaAPI js/window)
                                                (clj->js {:location
                                                          {:parent-uid personal-shortcuts-page-uid
                                                           :order "last"}
                                                          :block {:string page-to-add-title}}))
                                  (js/console.log "Personal shortcut added!" block-context)))})
             (.then (fn [_])
                    (js/console.log "Command added successfully!"))))))))