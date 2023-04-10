(ns left-sidebar.core
  (:require
    [clojure.edn :as edn]
    [reagent.core :as r]
    [reagent.dom :as rd]
    [left-sidebar.utils :as utils]
    [left-sidebar.namespaced-protocol.personal-shortcuts :as personal-shortcuts]
    [left-sidebar.single-page-protocol.core :as single-page-protocol]))


(defn start []
  (let [sidebar-container  (js/document.querySelector ".roam-sidebar-container .starred-pages-wrapper")
        todo-container     (js/document.createElement "div")
        starred-pages-html (.-outerHTML (.querySelector js/document ".starred-pages"))
        user-section-uids (utils/get-left-sidebar-section-uids-for-current-user)
        global-sections   (utils/get-global-left-sidebar-uids)]
    (.setAttribute todo-container "class" "todos-sidebar-container")
    (personal-shortcuts/create-left-sidebar-page)
    (when sidebar-container
      (.remove (.querySelector sidebar-container ".starred-pages"))
      (rd/render  [:div
                         {:class "starred-pages "
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
                   [single-page-protocol/left-sidebar-sections global-sections]
                   [single-page-protocol/left-sidebar-sections user-section-uids]]
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
