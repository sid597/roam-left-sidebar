(ns left-sidebar.todos
  (:require [left-sidebar.utils :as utils]
            [clojure.string :as str]
            [clojure.edn :as edn]
            ["@blueprintjs/core" :refer [Collapse Icon]]
            [reagent.core :as r]))


(defn create-my-todos-page []
  (let [username               (utils/get-current-user)
        personal-shortcuts-uid (utils/get-personal-shortcut-page-uid username)]
    (when-not personal-shortcuts-uid
      (-> (.createPage (.-roamAlphaAPI js/window)
                       (clj->js {:page
                                 {:title (str username "/left-sidebar/my-todos")}}))
          (.then (fn []
                   (let [new-shortcut-page-uid (utils/get-personal-shortcut-page-uid username)]
                     (js/console.log "My todos page created" new-shortcut-page-uid)
                     (-> (.createBlock (.-roamAlphaAPI js/window)
                                       (clj->js {:block
                                                 {:string ""
                                                  :location {:parent-uid new-shortcut-page-uid
                                                             :order 0}}}))))))))))





(defn todo-item [block]
  [:a {:href (str "/#/app/" (utils/get-graph-name) "/page/" (:uid block))
       :style {:text-decoration "none"}}
   [:div {:class "todo-item"
          :style {:padding "4px 0 4px 4px"
                  :color "hsl(204,20%,45%)"
                  :font-family "Inter"
                  :line-height "1.5"
                  :font-size "15px"
                  :font-weight "500"
                  :border-radius "3px"}}
    [:input {:type "checkbox"
             :style {:margin-right "8px"
                     :margin-left "4px"
                     :accent-color "black"}

             :on-click #(.updateBlock (.-roamAlphaAPI js/window) (clj->js {:block
                                                                           {:uid (:uid block)
                                                                            :string (str/replace (:string block) "{{[[TODO]]}}" "{{[[DONE]]}}")}}))}]
    (-> (:string block)
        (str/replace "{{[[TODO]]}}" " "))]])


(defn todos-component [todos]
  [:div {:class "todos"}
   (when @todos
     (for [todo @todos]
       ^{:key (:uid todo)}
       [todo-item todo]))])


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


(defn get-custom-query [query-block-uid]
  (first (first (utils/q '[:find ?query-string
                           :in $ ?query-block-uid
                           :where
                           [?e :block/uid ?query-block-uid]
                           [?e :block/children ?scratch-child]
                           [?scratch-child :block/string "scratch"]
                           [?scratch-child :block/children ?custom-child]
                           [?custom-child :block/string "custom"]
                           [?custom-child :block/children ?query-child]
                           [?query-child :block/string ?query-string]]
                         query-block-uid))))

(defn get-todos-from-query-block []
  (let [username          (utils/get-current-user)
        query-page        (str username "/left-sidebar/my todos")
        query-block-uid   (utils/get-child-of-block-with-text-on-page "My todo query" query-page)
        custom-query      (get-custom-query query-block-uid)
        query-result      (->> (utils/q (edn/read-string custom-query))
                               (mapv first))]
    (if (empty? query-result)
      (get-todos-for-user username)
      query-result)))
