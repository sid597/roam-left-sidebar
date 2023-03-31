(ns left-sidebar.template)


(comment
  (defn setup-reactive-query [custom-query-str new-block-uid]
    (let [reactive-qry-result (rd/q (edn/read-string custom-query-str))]
      (fn [reactive-qry-result new-block-uid]
        (block/update {:block
                       {:uid new-block-uid
                        :string (str @reactive-qry-result)}}))))

  (defn  get-result-blocks-uid []
    (-> (w/get-open-page-or-block-uid)
        (.then
          (fn [res]
            (let [custom-query-str (first (first(d/q '[:find ?custom-query-str
                                                        :in $ ?page-uid
                                                        :limit 1
                                                        :where
                                                        [?page-eid :block/uid ?page-uid]
                                                        [?page-eid :block/children ?child-eid]
                                                        [?child-eid :block/string "Query Builder"]
                                                        [?child-eid :block/children ?query-block-eid]
                                                        [?query-block-eid :block/children ?scratch-eid]
                                                        [?scratch-eid :block/string "scratch"]
                                                        [?scratch-eid :block/children ?custom-block-eid]
                                                        [?custom-block-eid :block/string "custom"]
                                                        [?custom-block-eid :block/children ?custom-child-eid]
                                                        [?custom-child-eid :block/string ?custom-query-str]]

                                                      res)))
                  block-uid (first (first (d/q '[:find ?block-uid
                                                  :in $ ?block-text ?page-uid
                                                  :where [?page-eid :block/uid ?page-uid]
                                                  [?page-eid :block/children ?child-eid]
                                                  [?child-eid :block/string ?block-text]
                                                  [?child-eid :block/uid ?block-uid]]
                                                "Query Results" res)))
                  new-block-uid (utils/generate-uid)]
              (println "block uid" block-uid "new block uid" new-block-uid "res" res (d/pull '[*] [:block/uid "Dg8HGePQx"]))
              (println custom-query-str)
              (println (d/q (edn/read-string custom-query-str)))

              #_(-> (block/create {:location {:parent-uid block-uid
                                                :order 0}
                                     :block {:string "Query Results"
                                                 :uid new-block-uid}})
                    #_(.then (fn [block-uid new-block-uid]
                                 (setup-reactive-query new-block-uid custom-query-str))))))))))