(ns left-sidebar.core
  (:require
    [clojure.edn :as edn]
    [reagent.core :as r]
    [reagent.dom :as rd]
    [left-sidebar.utils :as utils]
    [left-sidebar.global-shortcuts :as global-shortcuts]
    [left-sidebar.personal-shortcuts :as personal-shortcuts]
    [left-sidebar.user-left-sidebar :as user-left-sidebar]
    [left-sidebar.collapsable :as collapsable]
    [left-sidebar.todos :as todos]))


(defn start []
  (let [user               (utils/get-current-user)
        sidebar-container  (js/document.querySelector ".roam-sidebar-container .starred-pages-wrapper")
        todo-container     (js/document.createElement "div")
        ;todos-list         (r/atom (todos/get-todos-from-query-block))
        personal-shortcuts (r/atom (personal-shortcuts/get-personal-shortcuts-for-user user))
        starred-pages-html (.-outerHTML (.querySelector js/document ".starred-pages"))]
    (.setAttribute todo-container "class" "todos-sidebar-container")
    (personal-shortcuts/add-personal-shortcut-command-in-menu)
    (personal-shortcuts/create-personal-shortcuts-page)
    (when sidebar-container
      (.remove (.querySelector sidebar-container ".starred-pages"))
      (rd/render  [:div
                         {:class "collapsable-component-container"
                          :style {:overflow "scroll"
                                  :padding "15px"}}
                         [:style
                          (str ".personal-shortcut-item:hover{
                              color: #F5F8FA !important;
                              background-color: #10161A;
                              }
                              .page:hover{
                              color: #F5F8FA;
                              background-color: #10161A;
                              }
                              .todo-item:hover {
                              color: #F5F8FA !important;
                              background-color: #10161A;
                              }")]
                         #_[collapsable/collapsable-section "Global Shortcuts" "globe"
                            [global-shortcuts/starred-pages-component starred-pages-html]]
                         #_[collapsable/collapsable-section "Personal Shortcuts" "person"
                             [personal-shortcuts/personal-shortcuts-component personal-shortcuts]]
                         #_[collapsable/collapsable-section "My Todos" "tick"
                            [todos/todos-component "sync query" "sid/left-sidebar/my todos"]]
                    (user-left-sidebar/get-user-left-sidebar-list "sid")]


                 sidebar-container)
      ;; This is a hack to get the todos to update, we can use
      ;; mutation observers to do this better but it also complicates
      ;; things.
      #_(js/setInterval (fn []
                            (reset! todos-list (todos/get-todos-from-query-block))
                            (reset! personal-shortcuts (personal-shortcuts/get-personal-shortcuts-for-user user))) 1000))))

(defn stop []
  (let [sidebar-container (js/document.querySelector ".roam-sidebar-container .starred-pages-wrapper")
        todos-sidebar-container (js/document.querySelector ".todos-sidebar-container")]
    (when sidebar-container
      (rd/unmount-component-at-node todos-sidebar-container)
      (.removeChild sidebar-container todos-sidebar-container))))


(defn init []
  (println "Hello from left-sidebar plugin!")
  ;(pw)
 (start))
