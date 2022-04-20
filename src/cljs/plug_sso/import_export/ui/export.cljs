(ns plug-sso.import-export.ui.export
  (:require [plug-utils.re-frame :refer [<sub >evt]]
            [reagent.core :as r]
            [reagent.dom :as rdom]
            [plug-sso.import-export.utils :as export]))


(defn- fetch-data-button []
  [:button.button.is-info {:on-click #(>evt [:export/fetch-data])}
   "Fetch export data"])


(defn- discard-data-button []
  [:button.button.is-danger
   {:title    "Discard export data fetched from server"
    :on-click #(>evt [:export/discard-existing-data])}
   "Discard"])


;;TODO: See if it works just as well without atom. Just manipulating dom-node upon mount
(defn- download-link [{:keys [filename blob]}]
  (let [dom-node (r/atom nil)]
    (when filename
      (r/create-class
        {:component-did-mount (fn [this]
                                (reset! dom-node
                                        (some-> (rdom/dom-node this)
                                                (export/add-link-data-for blob filename))))
         :display-name        "File download link"
         :reagent-render      (fn []
                                [:a filename])}))))


(defn export-features []
  [:div
   [:div.level
    [:div.level-left
     [:div.level-item [fetch-data-button]]
     (when (<sub [:export/have-data-locally?])
       [:div.level-item [download-link (<sub [:export/link-data])]])]
    [:div.level-right
     [:div.level-item [discard-data-button]]]]])
