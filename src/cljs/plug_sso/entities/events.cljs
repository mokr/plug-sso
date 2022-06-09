(ns plug-sso.entities.events
  (:require [re-frame.core :as rf]
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
