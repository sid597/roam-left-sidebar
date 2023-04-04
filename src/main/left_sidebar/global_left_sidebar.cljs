(ns left-sidebar.global-left-sidebar
  (:require [left-sidebar.utils :as utils]))


(defn get-global-left-sidebar-list []
  (let [sidebar-section-titles (utils/get-page-name-from-ref (map first (utils/q '[:find ?children
                                                                                   :in $ ?username
                                                                                   :where [?e :node/title ?username]
                                                                                   [?e :block/children ?e-settings]
                                                                                   [?e-settings :block/string "Sections"]
                                                                                   [?e-settings :block/children ?e-children]
                                                                                   [?e-children :block/string ?children]
                                                                                   [(not= "" (clojure.string/trim ?children))]]
                                                                                 "roam/left-sidebar")))]))
