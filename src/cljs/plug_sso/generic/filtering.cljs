(ns plug-sso.generic.filtering
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [taoensso.timbre :as log]))


;|-------------------------------------------------
;| EVENTS

(rf/reg-event-db
  :filter/update
  [rf/trim-v]
  (fn [db [key value]]
    (->> value
         (str/trim)
         (str/lower-case)
         (assoc-in db [::filter key]))))


(rf/reg-event-db
  :filter/clear
  [rf/trim-v]
  (fn [db [key]]
    (if key
      (update db ::filter dissoc key)
      (dissoc db ::filter))))

;|-------------------------------------------------
;| SUBSCRIPTIONS

(rf/reg-sub
  :filter/terms
  (fn [db [_ key]]
    (str
      (if key
        (get-in db [::filter key])
        (db key)))))
