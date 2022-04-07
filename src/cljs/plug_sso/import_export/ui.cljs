(ns plug-sso.import-export.ui
  (:require [plug-sso.import-export.config :refer [IMPORT-KEY]]
            [plug-sso.import-export.events]
            [plug-sso.import-export.subs]
            [plug-utils.re-frame :refer [<sub >evt]]
            [plug-utils.time :as ut]
            [re-frame.core :as rf]
            [taoensso.timbre :as log]))


;|-------------------------------------------------
;| HELPERS

(defn- dispatch-file-content-and-store-in [db-key]
  (fn [e]
    (let [file     (-> e .-target .-files (aget 0))
          filename (-> file .-name)
          reader   (js/FileReader.)]
      (log/debug (str "Opened file \"" filename "\""))
      (set! (.-onload reader) #(rf/dispatch [:store/edn-data-from-file db-key filename (-> % .-target .-result)]))
      (.readAsText reader file))))


;|-------------------------------------------------
;| ICON

(def ^:private icons
  {:upload "upload"})


(defn- icon [{:keys [icon-id on-click disabled]}]
  [:span.material-icons
   (cond
     disabled {:title "No data to upload"
               :class "has-text-grey-light"}
     on-click {:title    "Click to upload data to backend and transact into database"
               :class    "is-clickable"
               :on-click on-click}
     :else nil)
   (icons icon-id "(≥o≤)")])


;|-------------------------------------------------
;| FILE IMPORT AND INFO

(defn- file-picker []
  [:input.input
   {:type      "file"
    :accept    ".edn"
    :style     {:max-width "20em"}
    :on-change (dispatch-file-content-and-store-in IMPORT-KEY)}])


(defn- discard-file-data-button []
  [:button.button.is-danger
   {:title    "Discard imported data (that has not already been transacted)\nAfterwards you can open a new file"
    :on-click #(>evt [:discard/imported-data])}
   "Discard"])


(defn- file-detail [text value]
  [:div
   [:span.has-text-info.mr-2 text]
   [:em value]])


(defn- file-details [{:keys [source-info exported filename valid?]}]
  [:div
   [:div.level
    [:div.level-left
     [:div.level-item
      [file-detail "File" filename]]
     [:div.level-item
      [file-detail "Source" source-info]]
     [:div.level-item
      [file-detail "Exported" exported]]
     [:div.level-item
      [file-detail "Valid?" (if valid? "yes" "no")]
      ]]
    [:div.level-right
     [:div.level-item [discard-file-data-button]]]]])


(defn- file-open-or-info
  "Toggles between input to open a file or file info,
  depending on whether we already have a file imported or not"
  []
  (if-let [file-info (<sub [:display-prepped/imported-data])]
    [file-details file-info]
    [:div.level
     [:div.level-item [file-picker]]]))


;|-------------------------------------------------
;| FILE CONTENT

(defn- stats-cell [category num-of-entries]
  [:td.is-clickable
   {:on-double-click #(>evt [:console/print-path [IMPORT-KEY category]])}
   num-of-entries])


(defn- header-cell-with-upload [text id entry-count]
  [:th
   [:div.level {:title "Upload transactions to backend for transacting into DB"
                :style {:max-width "5em"}}
    [:div.level-left
     [:div.level-item
      text]]
    [:div.level-right
     [:div.level-item.has-text-success
      [icon {:icon-id  :upload
             :disabled (< entry-count 1)
             :on-click #(>evt [:transact/imported-category id])}]]]]])


(defn- transaction-stats [{:keys [user-count access-count app-count unknown-count] :as stats}]
  [:table.table.is-narrow
   [:thead
    [:tr
     [header-cell-with-upload "Users" :users user-count]
     [header-cell-with-upload "Accesses" :accesses access-count]
     [header-cell-with-upload "Apps" :apps app-count]
     ;[:th "Unknowns"]
     ]]
   [:tbody
    [:tr
     [stats-cell :users user-count]
     [stats-cell :accesses access-count]
     [stats-cell :apps app-count]
     ;[stats-cell :unknown unknown-count]
     ]]])


(defn- describe-invalid-data []
  [:pre (<sub [:invalid/data-description])]
  )


(defn- info-about-imported-data
  "Show either the imported data or a description about why it is invalid"
  []
  (let [{:keys [valid?]} (<sub [:display-prepped/imported-data])]
    (if valid?
      [transaction-stats (<sub [:transaction/stats IMPORT-KEY])]
      [describe-invalid-data])))

;|-------------------------------------------------
;| PAGE

(defn page []
  [:section.section>div.container>div.content
   [:div [:strong.has-text-danger "*alpha*"] [:em " Just a crude, first implementation"]]
   [:h3.title.is.3 "Import from file"]
   [:div.box
    [file-open-or-info]
    [:br]
    [info-about-imported-data]]])