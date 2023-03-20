(ns left-sidebar.collapsable
  (:require [reagent.core :as r]
            ["@blueprintjs/core" :refer [Collapse Icon]]))

(defn collapsable-section [title icon children]
  (let [is-open (r/atom true)]
    (fn []
      [:<>
       [:button {:on-click #(reset! is-open (not @is-open))
                 :style {:display "flex"
                         :align-items "center"
                         :background "transparent"
                         :border "none"
                         :cursor "pointer"
                         :color "#fff"
                         :font-size "14px"
                         :font-weight "bold"
                         :padding "4px 0"
                         :outline "none"
                         :transition "color 0.2s ease-in"}}
        [:> Icon {:icon icon
                  :size "12px"
                  :style {:margin-right "4px"}}]
        [:span title]
        [:span (if @is-open [:> Icon {:icon "chevron-down"}]
                            [:> Icon {:icon "chevron-right"}])]]
       [:> Collapse {:is-open @is-open}
        children]])))





