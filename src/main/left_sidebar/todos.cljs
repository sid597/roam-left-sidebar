(ns left-sidebar.todos
  (:require [left-sidebar.utils :as utils]
            [clojure.string :as str]
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
  (println "todos component" @todos)
  [:div {:class "todos"}
   (for [todo @todos]
     ^{:key (:uid todo)}
     [todo-item todo])])


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


(defn get-todos-from-query-block []
  (let [username          (utils/get-current-user)
        query-page        (str username "/left-sidebar/my todos")
        query-block-uid   (utils/get-child-of-block-with-text-on-page "My todo query" query-page)]
    (-> js/window
        .-roamjs
        .-extension
        .-queryBuilder
        (.runQuery query-block-uid))))



(defn get-todos-list [username]
  (let [todos-list-atom (r/atom nil)]
    (.then (get-todos-from-query-block) (fn [todos]
                                          (reset! todos-list-atom
                                                  (js->clj todos :keywordize-keys true))))
    (println "----" @todos-list-atom)
    (println "====" @(r/atom (get-todos-for-user username)))
    (if todos-list-atom
        todos-list-atom
        (r/atom (get-todos-for-user username)))))

