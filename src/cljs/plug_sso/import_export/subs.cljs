(ns plug-sso.import-export.subs
  (:require [cljs.spec.alpha :as s]
            [plug-sso.specs :as $]
            [plug-sso.config :refer [EXPORTED-DATA
                                     EXPORT-LINK-DATA
                                     IMPORTED-DATA]]
            [plug-utils.maps :as um]
            [plug-utils.time :as ut]
            [re-frame.core :as rf]))


(rf/reg-sub
  :export/transactions
  (fn [db]
    (:export/transactions db)))


(rf/reg-sub
  :transaction/stats
  (fn [db [_ db-key]]
    (some-> (get-in db [db-key :data])
            (um/some-updates
              :user-count #(-> % :users count)
              :access-count #(-> % :accesses count)
              :app-count #(-> % :apps count)
              :unknown-count #(-> % :apps count)))))


(rf/reg-sub
  :import/data-from-file
  (fn [db [_]]
    (when-let [imported-from-file (get db IMPORTED-DATA)]
      (let [valid? (s/valid? ::$/file-import imported-from-file)]
        (-> imported-from-file
            (assoc :valid? valid?))))))


(rf/reg-sub
  :import/display-prepped-data
  :<- [:import/data-from-file]
  (fn [imported-data]
    (some-> imported-data
            (update :exported ut/inst->str))))


(rf/reg-sub
  :import/data-is-invalid?
  :<- [:import/data-from-file]
  (fn [{:keys [valid?]}]
    valid?))


(rf/reg-sub
  :import/invalid-data-description
  :<- [:import/data-from-file]
  (fn [{:keys [valid?] :as file-data}]
    (when (and file-data (not valid?))
      (s/explain-str ::$/file-import file-data))))


;|-------------------------------------------------
;| EXPORT

(rf/reg-sub
  :export/link-data
  (fn [db []]
    (db EXPORT-LINK-DATA)))


(rf/reg-sub
  :export/have-data-locally?
  (fn [db [_]]
    (boolean (db EXPORTED-DATA))))
