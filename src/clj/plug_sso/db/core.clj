(ns plug-sso.db.core
  "Schema and conn for DB"
  (:require [clojure.java.io :as io]
            [datalevin.core :as d]
            [plug-sso.config :refer [env]]
            [plug-sso.db.schema :refer [schema]]
            [mount.core :refer [defstate]]
            [taoensso.timbre :as log]))


(defstate conn
  :start (let [db-path (-> env :datalevin-db-path io/file .getAbsoluteFile str)] ;; Ensure it is an absolute path as Datalevin did not handle ~/ well and created a '~' sub dir inside project
           (log/info (format "Connecting to Datalevin DB %s" db-path))
           (d/get-conn db-path schema))
  :stop (d/close conn))
