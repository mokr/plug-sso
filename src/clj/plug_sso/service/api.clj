(ns plug-sso.service.api
  (:require [buddy.hashers :as hashers]
            [clojure.spec.alpha :as s]
            [plug-sso.config :refer [env]]
            [plug-sso.specs :as $]
            [plug-utils.spec :refer [valid?]]
            [plug-utils.debug :as d]
            [plug-utils.string :as us]
            [plug-sso.db.entities.user :as user]
            [plug-sso.db.entities.access :as access]
            [plug-sso.service.email :as email]
            [plug-sso.service.utils :refer [respond-with-issue respond-ok]]
            [ring.util.http-response :as response]
            [taoensso.timbre :as log]))


;|-------------------------------------------------
;| API HANDLERS

(defn- type-dispatcher
  "Dispatching fn for API handler"
  [req]
  (-> req :params :type))


(defmulti handle
          "Handle query to service api"
          type-dispatcher)


;|-------------------------------------------------
;| API: LOGIN

(defmethod handle :login [{:keys [params]}]
  (let [{:keys [app email password]} params]
    (log/debug (format "Handling login attempt by %s to \"%s\"" email app))
    (try
      (let [[hash role :as result] (user/get-users-hash-and-role-for-app email app)
            password-match? (hashers/check password hash)]
        (cond
          (nil? result)                                     ;;TODO: Reg failed login? Or just below?
          (do
            (log/debug (format "NOT FOUND! For %s logging in to \"%s\"" email app))
            (if (user/user-has-access-to-app? email app)
              (do
                (access/register-failed-login email app)
                (respond-with-issue "Looks like you haven't defined a password"))
              (respond-with-issue "You don't have access to this application")))

          password-match?
          (do
            (access/register-successful-login email app)
            (respond-ok {:access/role role}))

          (not password-match?)
          (do                                               ;;TODO: + Lock if too many failed
            (log/warn (format "Wrong password for user %s (accessing '%s')" email app))
            (access/register-failed-login email app)
            (respond-with-issue "Wrong password"))

          :else
          (do (log/error "Unhandled auth issue" result)
              (response/internal-server-error "Unhandled auth outcome"))))

      (catch Exception e
        (log/error (format "Exception while trying to authenticate %s for %s. Err: %s" email app (.getMessage e)))
        (response/internal-server-error (.getMessage e))))))


;|-------------------------------------------------
;| API: PASSWORD RESET EMAIL

(defn- gen-reset-token
  "Create a one-time token for password reset.
  A stringified UUID as it simplifies handling compared to real UUID when getting token back from emailed link"
  []
  (us/gen-uuid-str))


(defmethod handle :password-reset-email [{:keys [params]}]
  (let [{:keys [email app]} params]
    (if-not (user/user-has-access-to-app? email app)
      (do (log/warn (format "%s requested reset email for \"%s\", but don't have access to that app." email app))
          (respond-with-issue "You don't have access to this application"))
      (try
        (let [reset-token (gen-reset-token)
              _           (user/assign-reset-token email reset-token)
              smtp-config (email/smtp-config-from-env env)
              smtp-result (email/send-reset-email (assoc params :reset-token reset-token) smtp-config)]
          (if (-> smtp-result :code (= 0))
            (respond-ok)
            (do
              (log/error (format "Sending email to %s failed with error: %s" email (:message smtp-result)))
              (response/internal-server-error "Sending email failed"))))
        (catch Exception e
          (log/error (format "Sending reset email for %s failed with exception: %s" email (.getMessage e)))
          (response/internal-server-error "Unable to send email"))))))


;|-------------------------------------------------
;| API: VALIDATE TOKEN

(defmethod handle :validate-token [{:keys [params]}]
  (let [token (:token params)]
    (log/debug (format "Validate token: %s" params))
    (try
      (if (user/valid-token? token)
        (respond-ok)
        (respond-with-issue "Invalid token"))
      (catch Exception e
        (log/error (format "Token verification threw: %s" (.getMessage e)))
        (response/internal-server-error "Token validation failed")))))


;|-------------------------------------------------
;| API: PASSWORD RESET

(defmethod handle :password-reset [{:keys [params]}]
  (let [{:keys [password token]} params
        {:keys [user/email]} (user/get-user-by-token token)
        password-hash (hashers/derive password)]            ;;TODO: Derive in lib?
    (try
      (log/info (format "About to assign %s hash '%s'" email password-hash))
      (user/assign-password-hash email password-hash)
      (log/info (format "About to remove %s's token '%s'" email token))
      (user/remove-reset-token-from-user email)             ;;DEBUG: disabled removal
      (log/info (format "Password update for %s completed" email))
      (response/ok)
      (catch Exception e
        (log/error (format "Failed assigning new password to %s. Error: %s" email (.getMessage e)))
        (response/internal-server-error (.getMessage e))))))



;|-------------------------------------------------
;| API: DEFAULT / BAD REQUEST

(defmethod handle :default [request]
  (log/error "Don't know how to handle request of type" (-> request :params :type))
  (response/bad-request))


;|-------------------------------------------------
;| PUBLIC

(defn handle-request
  "Handle request to SSO service API.
  Dispatches to multifn for actual handling and response"
  [request]
  (try
    (handle request)
    (catch Exception e
      (response/internal-server-error))))                   ;; Details are logged at the point of failure