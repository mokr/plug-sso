(ns plug-sso.lib.core
  (:require [buddy.auth :refer [authenticated? throw-unauthorized]]
            [buddy.auth.backends.session :refer [session-backend]]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [buddy.hashers :as hashers]
    ;[buddy.core.crypto :as crypto]
    ;[buddy.core.nonce :as nonce]
            [clojure.spec.alpha :as s]
            [plug-sso.specs :as $]
            [plug-sso.lib.routes :as lib-routes]
            [plug-utils.spec :refer [valid?]]
            [clojure.string :as str]
            [taoensso.timbre :as log]
            [clojure.walk :refer [keywordize-keys]]
            [plug-utils.debug :as d]

            [ring.util.http-response :as response]
            [ring.util.response :refer [redirect]]
            [clojure.string :as str]))


;|-------------------------------------------------
;| MIDDLEWARE

(defn- api-route?
  "Inspect :uri to decide if this is an API URI."
  [request]
  {:pre [(map? request)]}
  (some-> request :uri (str/includes? "/api")))


(defn- unauthorized-handler
  [request metadata]
  ;(log/debug (format "unauthorized uri %s session: %s" (:uri request) (:session request)))
  ;(log/debug (format "Metadata: %s" metadata))
  (cond
    ;; If request is authenticated, raise 403 instead
    ;; of 401 (because user is authenticated but permission
    ;; denied is raised).
    (authenticated? request)
    (println "LATER: AUTHENTICATED BUT NOT AUTHORIZED" request)
    ;(-> (render (slurp (io/resource "error.html")) request)
    ;    (assoc :status 403))

    ;; If it is an API/AJAX route, be return a response client can use to redirect to /login
    (api-route? request)
    (response/unauthorized)                                 ;; Note: response/unauthorized! throws.

    ;; In other cases, redirect the user to login page.
    :else
    (let [current-url (:uri request)]
      (response/temporary-redirect (format "/login?next=%s" current-url)))))


;(defn on-error [request response]
;  (error-page
;    {:status 403
;     :title  (str "Access to " (:uri request) " is not authorized")}))

;(defn wrap-restricted [handler]
;  (restrict handler {:handler  authenticated?
;                     :on-error on-error}))



(def ^:private auth-backend
  (session-backend {:unauthorized-handler unauthorized-handler}))


(defn wrap-protected
  "Add this as middleware to the routes that require authentication.

  Luminus tip:
  - Typically added to 'yourproject.routes.home/home-routes'"
  [handler]
  (fn [request]
    (if (authenticated? request)
      (handler request)
      (throw-unauthorized))))


(defn wrap-auth
  "Usage: Add as middleware in app.

   Luminus tip:
    - Typically first middleware in 'yourproject.middleware/wrap-base'
    - OBS: Before (:middleware defaults) where Luminus adds prone.middleware/wrap-exceptions that will interfere with Buddy's exception handling."
  [handler]
  (-> handler
      (wrap-authorization auth-backend)                     ;; Note: authorization before authentication, even though some examples does the opposite
      (wrap-authentication auth-backend)))




(defn logout-button
  "UI button for logout"
  [{:keys [class text] :as opts
    :or   {text "logout"}}]
  [:button {:class class}
   text])


;|-------------------------------------------------
;| FOR CONVENIENCE

(def auth-routes lib-routes/auth-routes)