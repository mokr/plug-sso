(ns plug-sso.db.entities.app
  (:require [taoensso.timbre :as log]
            [datalevin.core :as d]
            [clojure.spec.alpha :as s]
            [plug-utils.spec :refer [valid?]]
            [plug-sso.specs :as $]
            [plug-sso.db.entities.access :as access]
            [plug-sso.db.core :as db]
            [plug-sso.db.utils :refer [delete-entity
                                       delete-entities
                                       get-entity
                                       maybe]]))


;|-------------------------------------------------
;| HELPERS

(defn ->eid
  "Try to convert value to a DB entity ID (eid)"
  [app-or-app-name]
  {:pre [(valid? ::$/app-or-app-name app-or-app-name)]}
  (when-let [app-name (maybe :app/name app-or-app-name)]
    (:db/id (d/entity (d/db db/conn) [:app/name app-name]))))


;|-------------------------------------------------
;| QUERIES

(def ^:private q-list-of-apps
  '[:find [(pull ?e [*]) ...]                               ;; Return collection of app maps
    :where
    [?e :app/name]])


;|-------------------------------------------------
;| LISTINGS

(defn list-of-apps
  "Get list of all existing apps."
  []
  {:post [(valid? ::$/apps %)]}
  (d/q q-list-of-apps
       (d/db db/conn)))


(defn available-roles
  "Get all access roles applicable for app"
  [app-name]
  (or (some-> app-name
              (->eid)
              (get-entity)                                  ;; A lazy map-like entity ..
              :access/roles)                                ;; .. fetches keys on demand
      #{}))


;|-------------------------------------------------
;| PREDICATES

(defn exists?
  "Is there an app with this app name?

  Takes either a plain app name as a string or
  a map containing such value in a :app/name key.

  Valid examples:
    (exists? \"app1\")
    (exists? {:app/name \"app1\"}"
  [app-or-app-name]
  {:pre [(valid? ::$/app-or-app-name app-or-app-name)]}
  (boolean
    (->eid app-or-app-name)))                               ;; Leave for ->eid to figure out the value details


(defn valid-role? [app role]
  (contains? (available-roles app) role))


;|-------------------------------------------------
;| MANAGEMENT

(defn upsert
  "Update a app with specified fields only."
  [app]
  {:pre [(valid? ::$/app app)]}
  (d/transact! db/conn [app]))


(defn delete-app [app-name]
  (try
    (let [id (->eid app-name)]
      (delete-entity id)
      (log/info (format "Deleted app %s (ID: %s)" app-name id)))
    (catch Exception e
      (log/error (format "App delete failed with '%s'" (.getMessage e)))
      (throw (InternalError. (format "Failed deleting app '%s'" app-name))))))


(defn delete-by-id
  "Delete an app by id"
  [id]
  (try                                                      ;; TODO: get info about app for logging
    (let [accesses-to-app (access/accesses-to-app-id id)]
      (log/info (format "Deleting all app ID %s's accesses: %s" id accesses-to-app))
      (delete-entities accesses-to-app)
      (log/info (format "Deleting app ID %s" id))
      (delete-entity id))
    (catch Exception e
      (log/error (format "Deleting app failed with '%s'" (.getMessage e)))
      (throw (InternalError. (format "Failed deleting app with ID %s)" id))))))
