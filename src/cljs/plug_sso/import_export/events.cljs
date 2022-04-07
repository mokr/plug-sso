(ns plug-sso.import-export.events
  (:require [plug-fetch.core :as fetch]
            [plug-sso.import-export.effects]
            [plug-sso.import-export.config :refer [IMPORT-KEY]]
            [re-frame.core :as rf]
            [taoensso.timbre :as log]))


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
  :discard/imported-data
  [rf/trim-v]
  (fn [db [_]]
    (dissoc db IMPORT-KEY)))


;|-------------------------------------------------
;| SEND IMPORTED DATA TO BACKEND

(rf/reg-event-fx
  :transact/imported-category
  [rf/trim-v]
  (fn [{:keys [db]} [category]]
    (when-let [transactions (get-in db [IMPORT-KEY :data category])]
      ;(log/debug "Transacting category" category)
      ;(js/console.info "Transactions" transactions)
      (fetch/make-fx-map-for-backend-event
        {:method      :post
         :uri         "/api/import/entities"
         :params      {:category     category
                       :transactions transactions}
         ;:result-fn
         :result-path [:transaction/result]
         ;:result-merge-in []
         ;:fx [[:dispatch [:]]]
         :ok-fx       [[:dispatch [:init/management]]]
         ;:nok-fx [[:dispatch [:]]]
         }))))


;|-------------------------------------------------
;| EXPORT

(rf/reg-event-fx
  :fetch/export-transactions
  [rf/trim-v]
  (fn [{:keys [db]} []]
    (fetch/make-fx-map-for-backend-event
      {:id          :fetch-users
       :uri         "/api/export/transactions"
       :result-path [:export/transactions]})))
