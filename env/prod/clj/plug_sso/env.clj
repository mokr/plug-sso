(ns plug-sso.env
  (:require [taoensso.timbre :as log]))

(def defaults
  {:init
               (fn []
                 (log/info "\n-=[plug-sso started successfully]=-"))
   :stop
               (fn []
                 (log/info "\n-=[plug-sso has shut down successfully]=-"))
   :middleware identity})
