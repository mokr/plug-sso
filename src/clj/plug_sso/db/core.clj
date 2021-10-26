(ns plug-sso.db.core
  "Schema and conn for DB"
  (:require [datalevin.core :as d]
            [plug-sso.config :refer [env]]
            [plug-sso.db.schema :refer [schema]]
            [mount.core :refer [defstate]]))


(defstate conn
  :start (d/get-conn (:datalevin-db-path env) schema)
  :stop (d/close conn))
