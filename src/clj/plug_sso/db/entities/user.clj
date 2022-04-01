(ns plug-sso.db.entities.user
  (:require [taoensso.timbre :as log]
            [datalevin.core :as d]
            [plug-utils.spec :refer [valid?]]
            [plug-sso.specs :as $]
            [plug-sso.db.entities.access :as access]
            [plug-sso.db.utils :refer [delete-entity
                                       delete-entities
                                       get-entity
                                       maybe]]
            [plug-sso.db.core :as db]
            [plug-sso.db.queries :as q]
            [clojure.spec.alpha :as s])
  (:import [java.util UUID]))


;|-------------------------------------------------
;| HELPERS

(defn ->eid
  "Try to convert value to a DB entity ID (eid)"
  [user-or-email]
  {:pre  [(valid? ::$/user-or-email user-or-email)]
   :post [(valid? ::$/maybe-eid %)]}
  (when-let [email (maybe :user/email user-or-email)]       ;; Short circuits if value looks off
    (:db/id (d/entity (d/db db/conn) [:user/email email]))))


(defn exists?
  "Is there a user with this email?

  Takes either a plain email address string or
  a map containing such value in a :user/email key.

  Valid examples:
    (exists? \"bob@example.com\")
    (exists? {:user/email \"bob@example.com\"}"
  [user-or-email]
  {:pre  [(or (s/valid? :user/email user-or-email)
              (s/valid? ::$/user user-or-email))]
   :post [(boolean? %)]}
  (boolean
    (->eid user-or-email)))                                 ;; Leave for ->eid to figure out the value details


(defn- assert-email
  "Throw if there is no existing user entity with this email"
  [email]
  (when-not (exists? email)
    (throw (IllegalArgumentException. (format "Unknown user '%s'" email))))) ;; Throw something more "UserNotFound" like?


;|-------------------------------------------------
;| RETRIEVE

(defn get-users-hash-and-role-for-app
  "Get password hash and access role for specified app provided that:
   - There is a user with given email
   - That can access the given app
   - And the user has a :password/hash assigned"
  [email app-name]
  ;{:post [(valid? ::$/users %)]}
  (first
    (d/q q/user-hash-and-role
         (d/db db/conn)
         email app-name)))


(defn user-has-access-to-app? [email app-name]
  (boolean
    (first
      (d/q q/user-has-access-to-app?
           (d/db db/conn)
           email app-name))))


(defn get-user-by-email
  "Retrieve user details"
  [email]
  (some->
    (d/entity (d/db db/conn) [:user/email email])
    (d/touch)))


(defn get-user-by-id
  "Retrieve user details"
  [id]
  (some->
    ;(d/entity (d/db db/conn) [:db/id id])
    (d/entity (d/db db/conn) id)
    (d/touch)))


(defn user-id->email
  "Get email for a given user id"
  [id]
  (-> (get-entity id)
      :user/email))


;|-------------------------------------------------
;| PASSWORD RESET

(defn assign-password-hash
  "Assign password hash to user identified by email"
  [email password-hash]
  {:pre [(valid? :user/email email)
         (valid? :password/hash password-hash)]}
  (try
    (d/transact! db/conn [{:user/email    email
                           :password/hash password-hash}])
    (catch Exception e
      (log/warn (format "Assigning new password (hash) to user %s failed with: %s" email (.getMessage e)))
      (throw (RuntimeException. "Failed assigning new password (hash) to user")))))


(defn assign-reset-token
  "Assign password reset token to user identified by email"
  [email reset-token]
  {:pre [(valid? :user/email email)
         (valid? :reset/token reset-token)]}
  (log/info (format "Assigning reset token to %s" email))
  (try
    (d/transact! db/conn [{:user/email  email
                           :reset/token reset-token}])
    (catch Exception e
      (log/warn (format "Assigning reset token to user %s failed with: %s" email (.getMessage e)))
      (throw (RuntimeException. "Failed assigning reset token to user")))))



;|-------------------------------------------------
;| TOKEN BASED PASSWORD RESET

(defn valid-token? [token]
  {:pre [(valid? :reset/token token)]}
  (boolean
    (d/entity (d/db db/conn) [:reset/token token])))


(defn get-user-by-token
  "Retrieve user details by reset token"
  [token]
  (some->
    (d/entity (d/db db/conn) [:reset/token token])
    (d/touch)))


(defn add-reset-token-to-user
  "Adds a token to a given user if user exists.
  Returning the token if successful"
  [email]
  (assert-email email)
  (try
    (let [token (UUID/randomUUID)]
      (d/transact! db/conn [{:user/email  email
                             :reset/token token}])
      token)
    (catch Exception e
      (log/warn (format "Failed adding token to user. Error: %s" (.getMessage e)))
      (throw (RuntimeException. "Failed adding token to user")))))


(defn remove-reset-token-from-user
  "Remove any existing reset token from user.
  Typically after it has been used or needs to be invalidated for some other reason"
  [user-or-email]
  (try
    (when-let [eid (->eid user-or-email)]
      (d/transact! db/conn [[:db/retract eid :reset/token]]))
    (catch Exception e
      (log/warn (format "Failed removing token. Error: %s" (.getMessage e)))
      (throw (RuntimeException. "Failed removing token from user")))))


;|-------------------------------------------------
;| USER MANAGEMENT

(defn upsert
  "Update a user with specified fields only."
  [user]
  {:pre [(valid? ::$/user user)]}
  (d/transact! db/conn [user]))


(defn listing
  "Get list of all existing users.
  E.g. for listing users in admin GUI"
  []
  {:post [(valid? ::$/users %)]}
  (d/q q/list-of-users
       (d/db db/conn)))


(defn delete-by-id
  "Two arities as we delete by ID (eid), but log email for readability"
  ([id]
   (if-let [email (user-id->email id)]
     (delete-by-id id email)
     (delete-by-id id "<unknown email>")))                  ;; Unknown email, but we assume ID is actually a user and proceed with deletion. Ensures we can delete entities with bad data.
  ([id email]                                               ;; We delete by ID, email is just for logging.
   (try
     (let [this-users-accesses (access/accesses-for-user-id id)]
       (log/info (format "Deleting %s's accesses: %s" email this-users-accesses))
       (delete-entities this-users-accesses)
       (log/info (format "Deleting user %s (ID %s)" email id))
       (delete-entity id))
     (catch Exception e
       (log/error (format "User delete failed with '%s'" (.getMessage e)))
       (throw (InternalError. (format "Failed deleting user '%s' (ID %s)" email id)))))))


(defn delete-by-email
  [email]
  (assert-email email)
  (let [id (->eid email)]
    (delete-by-id id email)))


;(defn get-essential-auth-data
;  "Retrieve map of auth data the client typically needs to render authentication related information."
;  [user app]
;
;  )
