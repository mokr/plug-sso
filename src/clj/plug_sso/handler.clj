(ns plug-sso.handler
  (:require
    [plug-sso.config :refer [env]]
    [plug-sso.middleware :as middleware]
    [plug-sso.layout :refer [error-page]]
    [plug-sso.routes.home :refer [home-routes]]
    [plug-sso.lib.routes :as sso-lib]
    [plug-sso.service.routes :as sso-service]
    [reitit.ring :as ring]
    [ring.middleware.content-type :refer [wrap-content-type]]
    [ring.middleware.webjars :refer [wrap-webjars]]
    [plug-sso.env :refer [defaults]]
    [mount.core :as mount]))


(mount/defstate init-app
  :start ((or (:init defaults) (fn [])))
  :stop ((or (:stop defaults) (fn []))))


(mount/defstate app-routes
  :start
  (ring/ring-handler
    (ring/router
      [(home-routes)                                        ;; (service)   UI routes used by admin UI
       (sso-service/api-endpoint)                           ;; (service)   API for client lib to communicated with service
       (sso-lib/auth-routes {:app            "SSO admin"    ;; (lib)       Auth routes handling auth (both in app utilizing the lib and to authenticate for admin UI)
                             :reset-capable? true
                             :sso-host       (:sso-host env)
                             :sso-port       (:sso-port env)})])
    (ring/routes
      (ring/create-resource-handler
        {:path "/"})
      (wrap-content-type
        (wrap-webjars (constantly nil)))
      (ring/create-default-handler
        {:not-found
         (constantly (error-page {:status 404, :title "404 - Page not found"}))
         :method-not-allowed
         (constantly (error-page {:status 405, :title "405 - Not allowed"}))
         :not-acceptable
         (constantly (error-page {:status 406, :title "406 - Not acceptable"}))}))))


(defn app []
  (middleware/wrap-base #'app-routes))
