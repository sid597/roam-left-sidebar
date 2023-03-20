;; namespace for this

(ns left-sidebar.roam
  (:require [reagent.core :as r]
            [reagent.dom :as rd]
            [cljs.pprint :as pp]
            [clojure.string :as str]
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
  ;; Passing starred pages html because we have to replace it with a new one.
  (let [starred-pages-html (.-outerHTML (.querySelector js/document ".starred-pages"))
        sidebar-container (js/document.querySelector ".roam-sidebar-container .starred-pages-wrapper")]
    (when sidebar-container
      (.remove (.querySelector sidebar-container ".starred-pages"))
      (rd/render [dropdown-menu starred-pages-html] (.querySelector sidebar-container ".title")))))



;; ========== My todos ==========


(defn get-todos-for-user [username]
    (->> (utils/q '[:find
                    (pull ?node [:block/uid :block/string])
                    :in $ ?username
                    :where
                    [?todo-eid :node/title "TODO"]
                    [?user-eid :node/title ?username]
                    [?node :block/refs ?todo-eid]
                    [?node :create/user ?node-creator]
                    [?node-creator :user/display-page ?user-eid]]
                  username)
         (mapv first)))


(defn todo-item [block]
  (let [graph-name (utils/get-graph-name)]
    [:li
     [:a {:href (str "/#/app/" graph-name "/page/" (:uid block))
          :style {:text-decoration "none"}}
      (-> block :string  str/trim)]]))


(defn todos-list [todos]
    (fn []
      [:ul
       (for [todo @todos]
         ^{:key (:uid todo)} [todo-item todo])]))

(defn todo-section [todos]
  [:div.todos-sidebar-section
   [:div.flex-h-box.title
    [:span.bp3-icon-small.bp3-icon-tick]
    [:div {:style {:flex "0 0 8px"}}]
    [:span "TODOS"]]
   [todos-list todos]])


(defn start []
  (let [user              (utils/get-current-user)
        sidebar-container (js/document.querySelector ".roam-sidebar-container .starred-pages-wrapper")
        todo-container    (js/document.createElement "div")
        todos             (r/atom [])]
    (.setAttribute todo-container "class" "todos-sidebar-container")
    (when sidebar-container
      (rd/render [todo-section todos] todo-container)
      (.appendChild sidebar-container todo-container)
      ;; This is a hack to get the todos to update, we can use
      ;; mutation observers to do this better but it also complicates
      ;; things.
      (js/setInterval (fn []
                        (reset! todos (get-todos-for-user user))) 1000))))

(defn stop []
  (let [sidebar-container (js/document.querySelector ".roam-sidebar-container .starred-pages-wrapper")
        todo-container (js/document.querySelector ".todos-sidebar-container")]
    (when sidebar-container
      (rd/unmount-component-at-node todo-container)
      (.removeChild sidebar-container todo-container))))
