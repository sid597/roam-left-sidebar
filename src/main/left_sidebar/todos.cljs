(ns left-sidebar.todos
  (:require [left-sidebar.utils :as utils]
            ["@blueprintjs/core" :refer [Collapse Icon]]))
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
  [:a {:href (str "/#/app/resultsgraph/page/" (:uid block))
       :style {:text-decoration "none"}}
   [:div
    (:string block)]])

(defn todos-component [todos]
  [:div {:class "todos"}
   (for [todo @todos]
     ^{:key (:uid todo)}
     [todo-item todo])])
