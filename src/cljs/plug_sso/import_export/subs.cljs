(ns plug-sso.import-export.subs
  (:require [cljs.spec.alpha :as s]
            [plug-sso.specs :as $]
            [plug-sso.import-export.config :refer [IMPORT-KEY]]
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
  :imported/from-file
  (fn [db [_]]
    (when-let [imported-from-file (get db IMPORT-KEY)]
      (let [valid? (s/valid? ::$/file-import imported-from-file)]
        (-> imported-from-file
            (assoc :valid? valid?))))))


(rf/reg-sub
  :display-prepped/imported-data
  :<- [:imported/from-file]
  (fn [imported-data]
    (some-> imported-data
            (update :exported ut/inst->str))))


(rf/reg-sub
  :imported/data-is-invalid?
  :<- [:imported/from-file]
  (fn [{:keys [valid?]}]
    valid?))


(rf/reg-sub
  :invalid/data-description
  :<- [:imported/from-file]
  (fn [{:keys [valid?] :as file-data}]
    (when (and file-data (not valid?))
      (s/explain-str ::$/file-import file-data))))