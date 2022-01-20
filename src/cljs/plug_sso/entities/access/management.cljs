(ns plug-sso.entities.access.management
  (:require [cljs.spec.alpha :as s]
            [plug-fetch.core :as fetch]
            [plug-sso.specs :as $]
            [re-frame.core :as rf]))



(rf/reg-event-fx
  :upsert/access
  [rf/trim-v]
  (fn [_ [[user-id role app-id :as new-access]]]
    {:pre [s/valid? ::$/new-access new-access]}
    (js/console.info "UPSERT ACCESS" new-access)
    (fetch/make-fx-map-for-backend-event
      {:method :post
       :uri    "/api/accesses"
       :params {:new-access new-access}
       :ok-fx  [[:dispatch [:fetch/accesses]]]})))


(rf/reg-event-fx
  :delete/access
  [rf/trim-v]
  (fn [_ [id]]
    (fetch/make-fx-map-for-backend-event
      {:method :delete
       :uri    (str "/api/accesses/" id)
       :ok-fx  [[:dispatch [:fetch/accesses]]]})))
