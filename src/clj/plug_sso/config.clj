(ns plug-sso.config
  (:require
    [cprop.core :refer [load-config]]
    [cprop.source :as source]
    [mount.core :refer [args defstate]]))

(defstate env
  :start
  (load-config
    :merge
    [(args)
     (source/from-system-props)
     (source/from-env-file ".env")                          ;; To be changed to yml file from Ansible
     (load-config)                                          ;; Allow project config.edn, dev-config and so on to override config from Ansible. NOTE: This will actually be the second call to load-config
     (source/from-env)]))
