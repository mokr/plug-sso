(ns plug-sso.generic.ui
  (:require [plug-sso.generic.filtering]
            [plug-utils.re-frame :refer [<sub >evt]]
            [plug-utils.dom :as udom]))


;|-------------------------------------------------
;| FILTER

(def ^:private key->placeholder-text
  "Placeholder texts for different filter keys"
  {:user/email "Filter on user email"})


(defn- filter-term-input
  "Internal: Just the input field for filter."
  [key]
  [:input.input.ml-2 {:type        "text"
                      :value       (<sub [:filter/terms :user/email])
                      :placeholder (key->placeholder-text key)
                      :on-change   #(>evt [:filter/update key (udom/target-value %)])}])


(defn filter-component
  "Filter input with clear button"
  [key]
  [:div.field.has-addons
   [:div.control.is-expanded
    [filter-term-input key]]
   [:div.control
    [:button.button.is-danger {:title    "Click to clear filter"
                               :on-click #(>evt [:filter/clear :user/email])}
     [:strong "X"]]]])