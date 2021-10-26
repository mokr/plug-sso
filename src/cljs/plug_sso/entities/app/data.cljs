(ns plug-sso.entities.app.data
  (:require [plug-fetch.core :as fetch]
            [plug-field.re-frame :as pfrf]
            [plug-field.ui.table :as pf-table]
            [plug-sso.entities.config :as config]
            [re-frame.core :as rf]
            [clojure.string :as str]))

;|-------------------------------------------------
;| DEFINITIONS

(def APPS-KEY :apps)


;|-------------------------------------------------
;| FETCH

(rf/reg-event-fx
  :fetch/apps
  (fn []
    (fetch/make-fx-map-for-backend-event
      {:id          :fetch-apps
       :uri         "/api/apps"
       :result-path [APPS-KEY]})))


;|-------------------------------------------------
;| SUBSCRIPTIONS

(rf/reg-sub
  ::apps
  (fn [db [_]]
    (some->>
      (get db APPS-KEY)
      (sort-by #(-> % :app/name (str/lower-case))))))


;|-------------------------------------------------
;| TABLE

(rf/reg-sub
  ::target-fields
  (constantly config/app-target-fields))


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
  :<- [::apps]
  :<- [::pfrf/row-config {:id-key :db/id}]
  pfrf/produce-field-entities-with-factories)


(rf/reg-sub
  :apps/table-data
  :<- [::table-headers]
  :<- [::table-contents]
  :<- [::pf-table/default-config]
  pfrf/as-table-data)

