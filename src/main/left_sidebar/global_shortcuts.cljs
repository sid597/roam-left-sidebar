(ns left-sidebar.global-shortcuts)
(defn starred-pages-component [html]
  [:div {:dangerouslySetInnerHTML {:__html html}}])
