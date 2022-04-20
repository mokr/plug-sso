(ns plug-sso.import-export.events
  (:require [plug-fetch.core :as fetch]
            [plug-sso.effects]
            [plug-sso.config :refer [EXPORTED-DATA
                                     EXPORT-LINK-DATA
                                     IMPORTED-DATA]]
            [plug-sso.import-export.utils :as export]
            [re-frame.core :as rf]
            [plug-utils.time :as ut]
            [taoensso.timbre :as log]
            [clojure.string :as str]))


;|-------------------------------------------------
;| HELPERS

(rf/reg-event-fx
  :console/print-path
  [rf/trim-v]
  (fn [{:keys [db]} [path]]
    {:console (get-in db path)}))


;|-------------------------------------------------
;| IMPORT FILE TO APP DB

(rf/reg-event-db
  :store/edn-data-from-file
  [rf/trim-v]
  (fn [db [db-key filename file-content]]
    (as-> file-content <>
          (cljs.reader/read-string <>)
          (assoc <> :filename filename)
          (assoc db db-key <>))))


(rf/reg-event-db
  :import/discard-current-data
  [rf/trim-v]
  (fn [db [_]]
    (dissoc db IMPORTED-DATA)))


;|-------------------------------------------------
;| SEND IMPORTED DATA TO BACKEND

(rf/reg-event-fx
  :import/transact-category
  [rf/trim-v]
  (fn [{:keys [db]} [category]]
    (when-let [transactions (get-in db [IMPORTED-DATA :data category])]
      (fetch/make-fx-map-for-backend-event
        {:method      :post
         :uri         "/api/import/entities"
         :params      {:category     category
                       :transactions transactions}
         :result-path [:transaction/result]
         :ok-fx       [[:dispatch [:init/management]]]}))))


;|-------------------------------------------------
;| EXPORT

(rf/reg-event-fx
  :export/fetch-data
  [rf/trim-v]
  (fn [_ _]
    (fetch/make-fx-map-for-backend-event
      {:id          :fetch-users
       :uri         "/api/export/data"
       :result-path [EXPORTED-DATA]
       :fx          [[:dispatch [:export/discard-existing-data]]] ;; Clear any existing data
       :ok-fx       [[:dispatch [:export/create-link-data]]]})))


(rf/reg-event-db
  :export/create-link-data
  [rf/trim-v]
  (fn [db []]
    (let [{:keys [exported] :as exported-data} (db EXPORTED-DATA)
          timestamp (some-> exported
                            (ut/inst->str :millis? false)
                            (str/replace #"[:]" "")
                            (str/replace #"[.]" "")
                            (str/replace #"[ ]" "_"))
          filename  (str "plug-sso_export_" timestamp ".edn")
          blob      (export/create-file-blob exported-data "application/edn")]
      (log/debug "Export file:" filename)
      (assoc db EXPORT-LINK-DATA {:filename filename
                                  :blob     blob}))))


(rf/reg-event-db
  :export/discard-existing-data
  [rf/trim-v]
  (fn [db []]
    (-> db
        (dissoc EXPORTED-DATA)
        (dissoc EXPORT-LINK-DATA))))