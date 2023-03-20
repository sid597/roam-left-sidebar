(ns left-sidebar.core
  (:require [reagent.core :as r]
            [reagent.dom :as rd]
            [left-sidebar.utils :as utils]
            [left-sidebar.shortcuts :as shortcuts]
            [left-sidebar.collapsable :as collapsable]
            [left-sidebar.todos :as todos]))


(defn start []
  (let [user              (utils/get-current-user)
        sidebar-container (js/document.querySelector ".roam-sidebar-container .starred-pages-wrapper")
        todo-container    (js/document.createElement "div")
        todos-list        (r/atom [])
        starred-pages-html (.-outerHTML (.querySelector js/document ".starred-pages"))]
    (.setAttribute todo-container "class" "todos-sidebar-container")
    (when sidebar-container
      (.remove (.querySelector sidebar-container ".starred-pages"))
      (rd/render [:<>
                  [collapsable/collapsable-section "SHORTCUTS" "star"
                   [shortcuts/starred-pages-component starred-pages-html]]
                  [collapsable/collapsable-section "TODOS" "tick"
                   [todos/todos-component todos-list]]]
                 sidebar-container)
      ;; This is a hack to get the todos to update, we can use
      ;; mutation observers to do this better but it also complicates
      ;; things.
      (js/setInterval (fn []
                        (reset! todos-list (todos/get-todos-for-user user))) 1000))))

(defn stop []
  (let [sidebar-container (js/document.querySelector ".roam-sidebar-container .starred-pages-wrapper")
        todos-sidebar-container (js/document.querySelector ".todos-sidebar-container")]
    (when sidebar-container
      (rd/unmount-component-at-node todos-sidebar-container)
      (.removeChild sidebar-container todos-sidebar-container))))

(defn init []
  (println "Hello from core!")
  (start))
