(ns plug-sso.db.entities.app
  (:require [taoensso.timbre :as log]
            [datalevin.core :as d]
            [clojure.data :as data]
            [clojure.spec.alpha :as s]
            [plug-utils.spec :refer [valid?]]
            [plug-sso.specs :as $]
            [plug-sso.db.core :as db]
            [plug-sso.db.entities.access :as access]
            [plug-sso.db.import :as import]
            [plug-sso.db.queries :as q]
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
;| LISTINGS

(defn list-of-apps
  "Get list of all existing apps."
  []
  {:post [(valid? ::$/apps %)]}
  (d/q q/list-of-apps
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

(defn- decide-roles-to-be-retracted
  "Compare stored roles for an app with the ones in app to be saved.
  Return roles that should be removed from DB"
  [{:keys [app/name access/roles] :as new-app-data}]
  {:pre  [(string? name) (sequential? roles)]
   :post [(or (nil? %) (coll? %))]}
  (let [stored-roles (available-roles name)
        fresh-roles  (into #{} roles)                       ;; Make sure we pass a set to 'diff'
        [roles-to-be-removed _ _] (data/diff stored-roles fresh-roles)] ;; First element in 3-tuple is the entries only in DB. See diff doc
    roles-to-be-removed))


(defn upsert
  "Update an app with specified fields only."
  [{:keys [app/name] :as app}]
  {:pre [(valid? ::$/app app)]}
  ;;TODO: Remove accesses using a role that is no longer valid. With confirmation?
  (let [roles-to-retract (decide-roles-to-be-retracted app)
        retractions      (map (fn [role]
                                [:db/retract [:app/name name] :access/roles role])
                              roles-to-retract)]
    (d/transact! db/conn (concat retractions [app]))))


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


;|-------------------------------------------------
;| IMPORT

(defn- upsert-multi
  "Upsert based on a collection of apps as maps.
  Typically, for importing from a data based export.
  Note: A \"data based export\" means data without DB IDs."
  [apps]
  {:pre [(valid? ::$/apps apps)]}
  (d/transact! db/conn apps))


(defmethod import/category-based-import :apps [{:keys [transactions]}]
  (upsert-multi transactions))
