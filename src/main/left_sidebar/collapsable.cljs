(ns left-sidebar.collapsable
  (:require [reagent.core :as r]
            ["@blueprintjs/core" :refer [Collapse Icon]]))

(defn collapsable-section [title children]
  (let [is-open (r/atom true)]
    (fn []
      [:div {:class "collapsable-section"
             :style {:margin-bottom "15px"}}
       [:button {:on-click #(reset! is-open (not @is-open))
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

        [:div {::style {:display "flex"
                        :align-items "center"
                        :justify-content "space-between"
                        :width "100%"}}
          [:span title]
          [:span (if @is-open [:> Icon {:icon "chevron-down"}]
                              [:> Icon {:icon "chevron-right"}])]]]
       [:hr {:style {:margin-bottom "4px"
                     :margin-top "2px"
                     :border "1px solid #CED9E0"
                     :border-radius "5px"}}]
       [:> Collapse {:is-open @is-open}
        children]])))

