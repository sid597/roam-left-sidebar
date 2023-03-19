;; namespace for this

(ns left-sidebar.roam
  (:require [reagent.core :as r]
            [reagent.dom :as rd]
            [cljs.pprint :as pp]
            [hickory.core :as h]
            [left-sidebar.utils :as utils]
            ["@blueprintjs/core" :refer [Collapse Icon]]))

;; ========== Roam shortcuts ==========

(defn dropdown-menu [html]
  (let [is-open (r/atom false)]
    (fn []
      [:div {:class "title"
             :style {:display "flex"
                     :flex "0 0 auto"
                     :flex-direction "column"
                     :width "100%"}}
       [:button {:on-click #(reset! is-open (not @is-open))
                 :style {:display "flex"
                         :background "black"}}

        [:div {:style {:display "flex"
                       :align-items "center"

                       :width "90%"}}
         [:span [:> Icon {:icon "star"
                          :size "12px"
                          :style {:margin-bottom "2px"
                                  :margin-right "4px"}}]]
         [:span "SHORTCUTS"]]
        [:div {:style {:display "flex"
                       :align-items "center"}}

         [:span (if @is-open [:> Icon {:icon "chevron-down"}]
                             [:> Icon {:icon "chevron-right"}])]]]

       [:div {:class "starred-pages"}
        [:> Collapse {:is-open @is-open}
         [:div {:dangerouslySetInnerHTML {:__html html}}]]]])))

(defn append-shortcuts-node []
  (let [starred-pages-html (.-outerHTML (.querySelector js/document ".starred-pages"))]
    (.remove (.querySelector js/document ".starred-pages"))
    (rd/render [dropdown-menu starred-pages-html]
               (.querySelector js/document ".title"))))



;; ========== My todos ==========


(defn get-todos-for-user []
  (let [user-uid (utils/get-uid-from-localstorage)
        username (first (flatten (utils/q '[:find ?username
                                            :in $ ?user-uid
                                            :where
                                            [?eid :user/uid ?user-uid]
                                            [?eid :user/display-page ?duid]
                                            [?duid :node/title ?username]]
                                          user-uid)))]
    (utils/q '[:find
               (pull ?node [:block/string :node/title :block/uid])
               (pull ?node [:block/uid])
               (pull ?page [:node/title :block/uid :block/string])
               :in $ ?username
               :where
               [?TODO-Ref :node/title "TODO"]
               [?node-User-Display :node/title ?username]
               [?node :block/refs ?TODO-Ref]
               [?node :create/user ?node-User]
               [?node-User :user/display-page ?node-User-Display]
               [?node :block/page ?page]]
             username)))
