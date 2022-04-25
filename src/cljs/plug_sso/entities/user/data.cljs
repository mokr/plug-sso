(ns plug-sso.entities.user.data
  (:require [plug-fetch.core :as fetch]
            [plug-field.re-frame :as pfrf]
            [plug-field.ui.table :as pf-table]
            [plug-sso.entities.config :as config]
            [re-frame.core :as rf]
            [taoensso.timbre :as log]
            [clojure.string :as str]))


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
;| FILTER

(rf/reg-event-db
  :filter/update
  [rf/trim-v]
  (fn [db [key value]]
    (->> value
         (str/lower-case)
         (assoc-in db [::filter key]))))


(rf/reg-sub
  :filter/terms
  (fn [db [_ key]]
    (str
      (if key
        (get-in db [::filter key])
        (db key)))))


;|-------------------------------------------------
;| SUBSCRIPTIONS

(rf/reg-sub
  ::unfiltered-users
  (fn [db [_]]
    (get db USERS-KEY)))


(rf/reg-sub
  ::users
  :<- [::unfiltered-users]
  :<- [:filter/terms :user/email]
  (fn [[users filter-term]]
    (->> users
         (filter #(or (empty? filter-term)
                      (-> % :user/email (str/includes? filter-term))))
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

