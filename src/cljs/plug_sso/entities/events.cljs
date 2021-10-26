(ns plug-sso.entities.events
  (:require [re-frame.core :as rf]
            [clojure.spec.alpha :as s]
            [plug-sso.specs :as $]
            [plug-fetch.core :as fetch]
            [plug-utils.re-frame :refer [>evt]]))


;|-------------------------------------------------
;| CONFIRMATION

(defn confirmation-text
  "Text for a confirmation dialog for a given action and ID it will be performed on.s"
  [action id]
  (case action
    :delete/user (str "Delete user " id "?")
    :delete/app (str "Delete app " id "?")
    :delete/access (str "Delete access " id "?")
    (str "Confirm " action)))


;; Perform some action on a given ID (:db/id) with confirmation
(rf/reg-event-fx
  :with/confirmation
  [rf/trim-v]
  (fn [_ [action id]]
    (when (js/confirm (confirmation-text action id))
      (>evt [action id]))))                                 ;; Eg. [:delete/user 2]


;|-------------------------------------------------
;| ERRORS

(rf/reg-event-db
  :reg/error
  [rf/trim-v]
  (fn [db [err]]
    (js/console.error err)
    (assoc db :latest/error err)
    ))




;|-------------------------------------------------
;| ACCESS ACTIONS

(rf/reg-event-fx
  :delete/access
  [rf/trim-v]
  (fn [_ [id]]
    (fetch/make-fx-map-for-backend-event
      {:method :delete
       :uri    (str "/accesses/" id)
       :ok-fx  [[:dispatch [:fetch/accesses]]]})))


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
