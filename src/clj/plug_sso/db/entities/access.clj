(ns plug-sso.db.entities.access
  (:require [taoensso.timbre :as log]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [datalevin.core :as d]
            [plug-utils.spec :refer [valid?]]
            [plug-sso.db.import :as import]
            [plug-sso.specs :as $]
            [plug-sso.db.queries :as q]
            [plug-sso.db.utils :refer [delete-entity
                                       get-entity-as-map
                                       get-entity]]
            [plug-sso.db.core :as db]))


;|-------------------------------------------------
;| LOGGING / LOCKING

(defn register-successful-login
  "Register successful login and clear failed count"
  [email app]
  (when-let [access (d/q q/access-for-user-to-app
                         (d/db db/conn)
                         email app)]
    (d/transact! db/conn [[:db/retract access :failed/logins]
                          [:db/add access :last/successful-login (java.util.Date.)]])))


(defn register-failed-login
  "Register failed login attempt by known user"
  [email app]
  (when-let [access (d/q q/access-for-user-to-app
                         (d/db db/conn)
                         email app)]
    (let [entity       (d/entity (d/db db/conn) access)
          failed-count (inc (:failed/logins entity 0))]
      (d/transact! db/conn [[:db/add access :failed/logins failed-count]
                            [:db/add access :last/failed-login (java.util.Date.)]]))))


;|-------------------------------------------------
;| LISTINGS

(defn list-of-accesses
  "Collection of maps describing accesses (with eIDs)"
  []
  {:post [(valid? ::$/accesses %)]}
  (d/q q/list-of-accesses
       (d/db db/conn)))


(defn list-of-accesses-as-texts
  "Convenience for quick overview of who can access what and with which role"
  []
  {:post [(every? string? %)]}
  (->>
    (d/q q/list-of-accesses-as-text
         (d/db db/conn))
    (map (partial str/join ","))))


(defn accesses-for-user
  "Collection of accesses for user with given email"
  [email]
  {:post [(valid? ::$/accesses %)]}
  (d/q q/accesses-for-user-email
       (d/db db/conn)
       email))


(defn accesses-to-app
  "Collection of accesses to given app"
  [app-name]
  {:post [(valid? ::$/accesses %)]}
  (d/q q/accesses-to-app
       (d/db db/conn)
       app-name))


;|-------------------------------------------------
;| FIND RELATED

(defn accesses-for-user-id
  "Find all accesses for a sigle user identified by ID (:db/id)"
  [id]
  {:pre  [(pos-int? id)]
   :post [(every? pos-int? %)]}
  (d/q q/access-ids-for-user-id
       (d/db db/conn)
       id))


(defn accesses-to-app-id
  "Find all accesses to a specific app.
  E.g. for the purpose of deleting all such accesses when the app itself is deleted"
  [id]
  {:pre  [(pos-int? id)]
   :post [(every? pos-int? %)]}
  ;; TODO: Write query
  (d/q q/access-ids-for-app-id
       (d/db db/conn)
       id))


;|-------------------------------------------------
;| MANAGEMENT

(defn delete-by-id
  "Delete access by id"
  [id]
  (try                                                      ;; TODO: get info about access for logging
    (delete-entity id)
    (log/info (format "DELETE: access ID %s" id))
    (catch Exception e
      (log/error (format "Deleting access failed with '%s'" (.getMessage e)))
      (throw (InternalError. (format "Failed deleting access with ID %s)" id))))))


(defn upsert
  "Create or update a users access to an app"
  [[user-id role app-id :as new-access]]
  {:pre [s/valid? ::$/new-access new-access]}
  (let [existing-access (first
                          (d/q q/existing-access-for-user-to-app
                               (d/db db/conn)
                               user-id app-id))
        user-email      (:user/email (get-entity user-id))
        app-name        (:app/name (get-entity app-id))]
    ;; Update existing if any ..
    (if (some? existing-access)
      (let [existing-role (:access/role (get-entity existing-access))]
        (if (= role existing-role)
          (log/info (format "NO-OP: %s already has '%s' access to '%s'" user-email role app-name))
          (do
            (log/info (format "UPDATE: %s is now '%s' in '%s' (was '%s')" user-email role app-name existing-role))
            (d/transact! db/conn [[:db/add existing-access :access/role role]]))))
      ;; .. or add the new access
      (do
        (log/info (format "NEW: %s got '%s' access to '%s'" user-email role app-name))
        (d/transact! db/conn [{:access/for  {:db/id user-id}
                               :access/to   {:db/id app-id}
                               :access/role role}])))))


;|-------------------------------------------------
;| IMPORT

(defn- upsert-based-on-data-import
  "Upsert access based on a map from a data based export.
  Note: A \"data based export\" means data without DB IDs."
  [{{user-email :user/email} :access/for
    {app-name :app/name}     :access/to
    role                     :access/role
    :as                      access}]
  {:pre [(map? access)]}
  (let [[access-id user-id app-id existing-role] (d/q q/access-for-user-to-app-as-ids
                                                      (d/db db/conn) user-email app-name)
        access-exists? (pos-int? access-id)]

    ;; Correct role for an existing access if needed
    (when (and access-exists?
               (not= role existing-role))
      (log/info (format "Access %s updated from role %s" [user-email role app-name] existing-role))
      (d/transact! db/conn [[:db/add access-id :access/role role]]))

    ;; Add non-existing access (will also create missing users and apps)
    (when-not access-exists?
      (log/info (format "Creating missing access %s" [user-email role app-name]))
      (d/transact! db/conn [access]))))


(defn- multi-upsert-from-map-transactions
  "Collection of maps"
  [accesses]
  (doseq [access accesses]
    (upsert-based-on-data-import access)))


(defmethod import/category-based-import :accesses [{:keys [transactions]}]
  (multi-upsert-from-map-transactions transactions))
