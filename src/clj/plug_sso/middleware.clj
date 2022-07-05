(ns plug-sso.middleware
  (:require
    [plug-sso.env :refer [defaults]]
    [plug-sso.lib.core :as sso-lib]
    [taoensso.timbre :as log]
    [plug-sso.layout :refer [error-page]]
    [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
    [plug-sso.middleware.formats :as formats]
    [muuntaja.middleware :refer [wrap-format wrap-params]]
    [plug-sso.config :refer [env]]
    [ring.middleware.flash :refer [wrap-flash]]
    [ring.adapter.undertow.middleware.session :refer [wrap-session]]
    [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
    [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
    [buddy.auth.accessrules :refer [restrict]]
    [buddy.auth :refer [authenticated?]]
    [buddy.auth.backends.session :refer [session-backend]])
  )

(defn wrap-internal-error [handler]
  (fn [req]
    (try
      (handler req)
      (catch Throwable t
        (log/error t (.getMessage t))
        (error-page {:status  500
                     :title   "Something very bad has happened!"
                     :message "We've dispatched a team of highly trained gnomes to take care of the problem."})))))

(defn wrap-csrf [handler]
  (wrap-anti-forgery
    handler
    {:error-response
     (error-page
       {:status 403
        :title  "Invalid anti-forgery token"})}))


(defn wrap-formats [handler]
  (let [wrapped (-> handler wrap-params (wrap-format formats/instance))]
    (fn [request]
      ;; disable wrap-formats for websockets
      ;; since they're not compatible with this middleware
      ((if (:websocket? request) handler wrapped) request))))

;(defn on-error [request response]
;  (error-page
;    {:status 403
;     :title  (str "Access to " (:uri request) " is not authorized")}))
;
;(defn wrap-restricted [handler]
;  (restrict handler {:handler  authenticated?
;                     :on-error on-error}))
;

(defn wrap-base [handler]
  (let [defaults-middleware (:middleware defaults)]
    (-> handler
        sso-lib/wrap-auth
        defaults-middleware                                 ;; After auth to avoid prone.middleware/wrap-exceptions in DEV interfering with Buddy unauthorized exceptions
        wrap-flash
        (wrap-session {:timeout      7200                   ; 7200 sec 2 hours (from old mTools)
                       :cookie-attrs {:http-only true}})
        (wrap-defaults
          (-> site-defaults
              (assoc-in [:security :anti-forgery] false)
              (dissoc :session)))
        wrap-internal-error)))
