(ns plug-sso.entities.access.data
  (:require [plug-fetch.core :as fetch]
            [plug-field.re-frame :as pfrf]
            [plug-field.ui.table :as pf-table]
            [plug-sso.entities.config :as config]
            [re-frame.core :as rf]))

;|-------------------------------------------------
;| DEFINITIONS

(def ACCESSES-KEY :accesses)


;|-------------------------------------------------
;| FETCH

(rf/reg-event-fx
  :fetch/accesses
  (fn []
    (fetch/make-fx-map-for-backend-event
      {:id          :fetch-accesses
       :uri         "/api/accesses"
       :result-path [ACCESSES-KEY]})))


;|-------------------------------------------------
;| SUBSCRIPTIONS

(rf/reg-sub
  ::accesses
  (fn [db [_]]
    (some->>
      (get db ACCESSES-KEY)
      (sort-by (comp :db/id :access/for)))))                ;; Temp sorting. Need functionality to lookup refs for proper sorting.


;|-------------------------------------------------
;| TABLE

(rf/reg-sub
  ::target-fields
  (constantly config/accesses-target-fields))


(rf/reg-sub
  ::header-factories
  :<- [::pfrf/key-config]
  :<- [::pfrf/common-header-config]
  :<- [::pfrf/field-defaults]
  :<- [::target-fields]
  pfrf/create-field-factories)


(rf/reg-sub
  ::table-headers
  :<- [::header-factories]
  :<- [::pfrf/no-entities]
  :<- [::pfrf/row-config]
  pfrf/produce-field-entities-with-factories)


(rf/reg-sub
  ::field-value-factories
  :<- [::pfrf/value-config]
  :<- [::pfrf/common-content-config {:id-key :db/id}]
  :<- [::pfrf/field-defaults]
  :<- [::target-fields]
  pfrf/create-field-factories)


(rf/reg-sub
  ::table-contents
  :<- [::field-value-factories]
  :<- [::accesses]
  :<- [::pfrf/row-config {:id-key :db/id}]
  pfrf/produce-field-entities-with-factories)


(rf/reg-sub
  :accesses/table-data
  :<- [::table-headers]
  :<- [::table-contents]
  :<- [::pf-table/default-config]
  pfrf/as-table-data)

