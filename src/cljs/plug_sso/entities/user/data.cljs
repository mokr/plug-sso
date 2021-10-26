(ns plug-sso.entities.user.data
  (:require [plug-fetch.core :as fetch]
            [plug-field.re-frame :as pfrf]
            [plug-field.ui.table :as pf-table]
            [plug-sso.entities.config :as config]
            [plug-utils.log :as log]
            [plug-utils.debug :as d]
            [re-frame.core :as rf]))

;|-------------------------------------------------
;| DEFINITIONS

(def USERS-KEY :users)


;|-------------------------------------------------
;| FETCH

(rf/reg-event-fx
  :fetch/users
  (fn []
    (fetch/make-fx-map-for-backend-event
      {:id          :fetch-users
       :uri         "/api/users"
       :result-path [USERS-KEY]})))


;|-------------------------------------------------
;| SUBSCRIPTIONS

(rf/reg-sub
  ::users
  (fn [db [_]]
    (some->>
      (get db USERS-KEY)
      (sort-by :user/email))))


;|-------------------------------------------------
;| TABLE

(rf/reg-sub
  ::target-fields
  (constantly config/user-target-fields))


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
  :<- [::users]
  :<- [::pfrf/row-config {:id-key :db/id}]
  pfrf/produce-field-entities-with-factories)


(rf/reg-sub
  :users/table-data
  :<- [::table-headers]
  :<- [::table-contents]
  :<- [::pf-table/default-config]
  pfrf/as-table-data)

