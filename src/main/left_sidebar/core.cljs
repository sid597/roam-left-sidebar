(ns left-sidebar.core
  (:require
    [clojure.edn :as edn]
    [reagent.core :as r]
    [reagent.dom :as rd]
    [left-sidebar.utils :as utils]
    [left-sidebar.global-shortcuts :as global-shortcuts]
    [left-sidebar.personal-shortcuts :as personal-shortcuts]
    [left-sidebar.single-page-protocol.core :as single-page-protocol]
    [left-sidebar.user-left-sidebar :as user-left-sidebar]
    [left-sidebar.collapsable :as collapsable]
    [left-sidebar.todos :as todos]))


(defn start []
  (let [sidebar-container  (js/document.querySelector ".roam-sidebar-container .starred-pages-wrapper")
        todo-container     (js/document.createElement "div")]
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
                   [single-page-protocol/left-sidebar-sections]]
                 sidebar-container))))

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
