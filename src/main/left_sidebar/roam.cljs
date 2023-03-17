;; namespace for this

(ns left-sidebar.roam
  (:require [reagent.core :as r]
            [reagent.dom :as rd]
            [cljs.pprint :as pp]
            [hickory.core :as h]
            ["@blueprintjs/core" :refer [Collapse Icon]]))

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

(defn append-node []
  (let [starred-pages-html (.-outerHTML (.querySelector js/document ".starred-pages"))]
    (.remove (.querySelector js/document ".starred-pages"))
    (rd/render [dropdown-menu starred-pages-html]
               (.querySelector js/document ".title"))))


(comment
   (defn get-starred-pages-as-hiccup []
     (let [starred-pages         (.querySelector js/document ".starred-pages")
           shortcut-links        (.-children starred-pages)
           parsed-shortcut-links (h/parse-fragment (apply str (for [link shortcut-links]
                                                                (.-outerHTML link))))
           shortcut-links-hiccup (r/as-element (map h/as-hiccup parsed-shortcut-links))]
       (pp/pprint shortcut-links-hiccup))))