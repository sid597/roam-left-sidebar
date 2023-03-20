(ns left-sidebar.shortcuts)
(defn starred-pages-component [html]
  [:div {:dangerouslySetInnerHTML {:__html html}}])
