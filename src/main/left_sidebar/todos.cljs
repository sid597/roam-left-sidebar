(ns left-sidebar.todos
  (:require [left-sidebar.utils :as utils]
            [clojure.string :as str]
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
                     :accent-color "black"
                     }
             :on-click #(.updateBlock (.-roamAlphaAPI js/window) (clj->js {:block
                                                                           {:uid (:uid block)
                                                                            :string (str/replace (:string block) "{{[[TODO]]}}" "{{[[DONE]]}}")}}))}]
    (-> (:string block)
        (str/replace "{{[[TODO]]}}" " "))]])


(defn todos-component [todos]
  [:div {:class "todos"}
   (for [todo @todos]
     ^{:key (:uid todo)}
     [todo-item todo])])
