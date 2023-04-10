(ns left-sidebar.namespaced-protocol.template)


(comment
  #_(ns developer-doc.rd.q
      (:require
        [roam.datascript.reactive :as rd]
        [roam.datascript :as d]
        [roam.main-window :as w]
        [roam.block :as block]
        [reagent.core :as r]
        [roam.util :as utils]
        [promesa.core :as p]
        [clojure.edn :as edn]))


  (defn set-up-reactive-query []
    (let [block-data (r/atom nil)
          callback (fn [custom-query-str new-block-uid query-results-block-uid query-result-block-children]
                     (reset! block-data {:custom-query-str custom-query-str
                                         :new-block-uid new-block-uid
                                         :query-results-block-uid query-results-block-uid
                                         :query-result-block-children query-result-block-children}))]
      (-> (w/get-open-page-or-block-uid)
          (p/then
            (fn [res]
              (let [custom-query-str            (ffirst (d/q '[:find ?custom-query-str
                                                               :in $ ?page-uid
                                                               :limit 1
                                                               :where [?page-eid :block/uid ?page-uid]
                                                                      [?page-eid :block/children ?child-eid]
                                                                      [?child-eid :block/string "Query Builder"]
                                                                      [?child-eid :block/children ?query-block-eid]
                                                                      [?query-block-eid :block/children ?scratch-eid]
                                                                      [?scratch-eid :block/string "scratch"]
                                                                      [?scratch-eid :block/children ?custom-block-eid]
                                                                      [?custom-block-eid :block/string "custom"]
                                                                      [?custom-block-eid :block/children ?custom-child-eid]
                                                                      [?custom-child-eid :block/string ?custom-query-str]]

                                                             res))
                    query-results-block-uid     (ffirst (d/q '[:find ?block-uid
                                                               :in $ ?block-text ?page-uid
                                                               :where [?page-eid :block/uid ?page-uid]
                                                                      [?page-eid :block/children ?child-eid]
                                                                      [?child-eid :block/string ?block-text]
                                                                      [?child-eid :block/uid ?block-uid]]
                                                             "Query Results" res))
                    query-result-block-children (ffirst (d/q '[:find ?child-block-uid
                                                               :in $ ?block-uid
                                                               :where [?e :block/uid ?block-uid]
                                                                      [?e :block/children ?children]
                                                                      [?children :block/uid ?child-block-uid]]
                                                             query-results-block-uid))
                    new-block-uid (utils/generate-uid)]
                (-> (block/create {:location {:parent-uid query-results-block-uid
                                              :order 0}
                                   :block {:string ""
                                           :uid new-block-uid}})
                    (p/then (fn [_]
                              (callback custom-query-str new-block-uid query-results-block-uid query-result-block-children))))))))

      (fn []
        (if @block-data
          (let [reactive-qry-result (rd/q (edn/read-string (:custom-query-str @block-data)))
                new-block-uid       (:new-block-uid @block-data)]

            (->(block/update {:block
                              {:uid new-block-uid
                               :string  (str "/`/`/`" @reactive-qry-result "/`/`/`")}})
               (p/then (fn []
                         (let [child-block (:query-result-block-children @block-data)]
                           (when child-block
                             (block/delete {:block {:uid child-block}}))))))
            [:div "reactive queries"])
          [:div "Loading..."])))))

