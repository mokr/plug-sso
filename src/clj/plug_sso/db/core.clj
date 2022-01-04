(ns plug-sso.db.core
  "Schema and conn for DB"
  (:require [datalevin.core :as d]
            [plug-sso.config :refer [env]]
            [plug-sso.db.schema :refer [schema]]
            [me.raynes.fs :as fs]
            [mount.core :refer [defstate]]
            [taoensso.timbre :as log]))


(defstate conn
  :start (let [db-path (-> env :datalevin-db-path fs/expand-home str)]
           (log/info (format "Connecting to Datalevin DB %s" db-path))
           (d/get-conn db-path schema))
  :stop (d/close conn))
