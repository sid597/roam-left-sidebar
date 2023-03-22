(ns left-sidebar.personal-shortcuts
  (:require [left-sidebar.utils :as utils]
            ["@blueprintjs/core" :refer [Collapse Icon]]))


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
  [:a {:href (str "/#/app/resultsgraph/page/" (:uid shortcut))
       :style {:text-decoration "none"}}
   [:div {:class "personal-shortcut-item"
          :style {:padding "4px 0 4px 4px 4px"
                  :color "hsl(204,20%,45%)"
                  :font-family "Inter"
                  :font-size "14px"}}
    (-> (:title shortcut))]])

(defn personal-shortcuts-component [shortcuts]
  [:div {:class "personal-shortcuts"}
   (for [shortcut @shortcuts]
     ^{:key (:uid shortcut)}
     [personal-shortcut-item shortcut])])
