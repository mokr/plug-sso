(ns plug-sso.config
  (:require
    [cprop.core :refer [load-config]]
    [cprop.source :as source]
    [mount.core :refer [args defstate]]
    [plug-utils.yaml :as uy]))


(defstate env
  :start
  (merge
    (:plug-sso (uy/config-from-yml-file "ansible_config.yml")) ;; NOTE: :plug-sso not :plug_sso
    (load-config
      :merge
      [(args)
       ;(source/from-system-props)
       (source/from-env)])))
