(ns plug-sso.service.routes
  (:require [plug-sso.middleware :as middleware]
            [plug-sso.service.api :as api]
            [plug-sso.service.middleware :refer [wrap-message-encryption]]))


(defn api-endpoint
  "Routes for the auth service receiving requests from apps (clients) using plug-sso as a lib"
  []
  ["" {:middleware [wrap-message-encryption
                    ;middleware/wrap-csrf
                    middleware/wrap-formats]}
   ["/service/api"
    {:post api/handle-request}]])                           ;; NOTE: Using a single handler to make it easy to transition to websocket
