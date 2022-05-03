(ns plug-sso.generic.filtering
  (:require [re-frame.core :as rf]
            [clojure.string :as str]))


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
