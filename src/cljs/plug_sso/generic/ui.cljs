(ns plug-sso.generic.ui
  (:require [plug-utils.re-frame :refer [<sub >evt]]
            [plug-utils.dom :as udom]))


;|-------------------------------------------------
;| FILTER

(defn- filter-term-input [key]
  [:input.input.ml-2 {:type        "text"
                      :style       {:max-width "25em"}
                      :value       (<sub [:filter/terms :user/email])
                      :placeholder "Filter on email"
                      :on-change   #(>evt [:filter/update key (udom/target-value %)])}])

