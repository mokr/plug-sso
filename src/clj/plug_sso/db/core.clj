(ns plug-sso.db.core
  "Schema and conn for DB"
  (:require [datalevin.core :as d]
            [plug-sso.config :refer [env]]
            [plug-sso.db.schema :refer [schema]]
            [me.raynes.fs :as fs]
            [mount.core :refer [defstate]]
            [taoensso.timbre :as log]
            [buddy.hashers :as hashers]))


(defn make-defaults-transaction
  "Create admin user to be used initially"
  [email password]
  {:pre  [(string? email) (string? password)]
   :post [(vector? %)]}
  [;; Create "SSO admin" app
   {:db/id        -1
    :app/name     "SSO admin"
    :access/roles "admin"}
   ;; Create default admin user
   {:db/id         -2
    :user/name     "default admin"
    :user/email    email
    :password/hash (hashers/derive password)
    :user/info     "First time user. Delete when another admin has been created."}
   ;; Give admin access to app
   {:access/to   [:app/name "SSO admin"]
    :access/for  [:user/email email]
    :access/role "admin"}])


(defn empty-db? [conn]
  (empty? (d/datoms @conn :eav)))


(defn inject-defaults-if-db-is-empty
  "Takes a DB connection and injects some default data if DB is empty.
  Returns connection"
  [conn {:keys [first-time-email first-time-password]}]
  (when (empty-db? conn)
    (log/warn (format "Injecting default admin user %s into empty DB" first-time-email))
    (d/transact! conn (make-defaults-transaction first-time-email first-time-password)))
  conn)


(defstate conn
  :start (let [db-path (-> env :datalevin-db-path fs/expand-home str)]
           (log/info (format "Connecting to Datalevin DB %s" db-path))
           (-> (d/get-conn db-path schema)
               (inject-defaults-if-db-is-empty env)))
  :stop (d/close conn))
