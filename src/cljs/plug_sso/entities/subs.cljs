(ns plug-sso.entities.subs
  "Subscriptions that are not specific to one type of entities"
  (:require [plug-sso.entities.user.data :as user]
            [plug-sso.entities.app.data :as app]
            [plug-sso.entities.access.data :as access]
            [re-frame.core :as rf]))


;|-------------------------------------------------
;| LOOKUP OF ENTITIES OF ANY KIND

(rf/reg-sub
  ::entity-lookup
  :<- [::user/users]
  :<- [::app/apps]
  :<- [::access/accesses]
  (fn [[users apps accesses]]
    (->> (concat users apps accesses)
         (into {} (map (juxt :db/id identity))))))


(rf/reg-sub
  :lookup/entity
  :<- [::entity-lookup]
  (fn [entity-lookup [_ id]]
    (get entity-lookup id)))
