(ns plug-sso.env
  (:require
    [selmer.parser :as parser]
    [taoensso.timbre :as log]
    [plug-sso.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
               (fn []
                 (parser/cache-off!)
                 (log/info "\n-=[plug-sso started successfully using the development profile]=-"))
   :stop
               (fn []
                 (log/info "\n-=[plug-sso has shut down successfully]=-"))
   :middleware wrap-dev})
