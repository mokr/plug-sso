(ns plug-sso.lib.routes
  "Provides routes used by an app to handle auth related tasks.
  Communicates with SSO Service API according to sso-opts map (passed to routes function)"
  (:require [plug-sso.lib.pages :as pages]
            [plug-sso.validation :refer [validate-password]]
            [clj-http.client :as http]
            [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [plug-sso.specs :as $]
            [plug-utils.spec :refer [valid?]]
            [plug-utils.string :as us]
            [plug-utils.time :as tu]
            [taoensso.timbre :as log]
            [clojure.walk :refer [keywordize-keys]]
            [ring.util.http-predicates :as predicates]
            [ring.util.http-response :as response :refer [content-type]]
            [ring.util.response :refer [redirect]]))


;|-------------------------------------------------
;| CONTACT SERVICE API

(defn- encrypt-data
  "Encrypt data that is to be sent to plug-sso service"
  [data]
  data)


(defn- sso-service-url
  "Assemble sso service url bases on sso-opts passed in when creating the auth routes"
  [{:keys [sso-host sso-port] :as sso-opts
    :or   {sso-host "localhost"
           sso-port 3300}}]
  (format "http://%s:%s/service/api" sso-host sso-port))


(defn- query-sso-service
  "Perform the actual query to SSO Service API"
  [type params {:keys [app] :as sso-opts}]
  (let [params (-> (merge params
                          {:type    type
                           :app     app
                           :version 1})
                   (encrypt-data))
        url    (sso-service-url sso-opts)]
    (http/post url
               {
                :form-params        params
                :content-type       :transit+json           ;; Sending
                :as                 :transit+json           ;; Receiving
                ;:throw-exceptions   false
                :connection-timeout 5000})))


;|-------------------------------------------------
;| LOGIN

(defn- login
  "Contact SSO Service API to login/authenticate user"
  [{:keys [params] :as req} sso-opts]                       ;; TODO: use env port as default service-port
  {:pre [(valid? ::$/login-params params)]}
  ;(log/debug (format "PARAMS: %s" params))
  (let [user-data    (-> params
                         (select-keys [:email :password])
                         (update :email str/lower-case))
        email        (:email user-data)
        api-response (query-sso-service :login user-data sso-opts)
        {:keys [timestamp issue? message role] :as result} (-> api-response :body keywordize-keys)] ;; TODO: clj-http can probably handle keywordize-keys with the right config
    (cond

      (not (predicates/success? api-response))              ;;HTTP request to SSO service API failed
      (do
        (log/warn "Unsuccessful authentication for " email)
        (pages/login-page (assoc sso-opts
                            :error (str (tu/time-now-local-str) ": Unable to handle request. You might try again later."))))

      (some? issue?)                                        ;;HTTP request ok, but indicate an issue
      (do
        (log/warn (format "Failed login for %s. \"%s\"" email message))
        (pages/login-page (assoc sso-opts :error (str timestamp ": " message))))

      :else-successful-login                                ;;Successful authentication
      (let [next-url (get-in req [:query-params "next"] "/")]
        (log/info email "logged in")
        (->
          (redirect next-url)                               ;; Note: ring.util.http-response/temporary-redirect causes "405 - Not allowed" issue
          (assoc :session {:identity email
                           :role     role}))))))


;|-------------------------------------------------
;| LOGOUT

(defn- logout
  "Contact SSO Service API for logout.
  Provides client with redirect url in response and clears the server side session."
  [{:keys [identity] :as request}]
  (log/info identity "logged out")
  (->
    (response/temporary-redirect "/login")                  ;; Create response with redirect to /login ..
    (assoc :session {})))                                   ;; .. and clear session


;|-------------------------------------------------
;| PASSWORD RESET

(defn- send-reset-email
  "Contact SSO Service API to send email to user"
  [params sso-opts]
  (let [api-response (query-sso-service :password-reset-email params sso-opts)
        {:keys [timestamp issue? message role] :as result} (-> api-response :body keywordize-keys)]
    (cond
      (not (predicates/success? api-response))              ;; API responded with e.g. HTTP 500
      (pages/feedback-page (assoc sso-opts
                             :error "Sorry, sending email failed!"
                             :message "Probably a backend issue and it might be permanent."
                             :timestamp (tu/time-now-local-str) ;; Note: This timestamp will update if user refreshes the feedback page. Not perfect, but ok for now.
                             :redirect-url "/orderreset"
                             :redirect-text "Click here to try again"))

      (some? issue?)                                        ;; API request did not fail, but there was an issue preventing service endpoint from fulfilling the request
      (pages/feedback-page (assoc sso-opts
                             :error message
                             ;:message message
                             :timestamp timestamp
                             :redirect-url "/orderreset"
                             :redirect-text "Click here to try again"))

      :else-emailing-successful
      (pages/feedback-page (assoc sso-opts
                             :feedback "Check your email"
                             :message "You can close this page and follow link in email."
                             :timestamp timestamp
                             :redirect-url "/orderreset"
                             :redirect-text "No email? Click here to try again")))))


(defn- valid-reset-token?
  "Returns identity"
  [token sso-opts]
  (let [params       {:token token}
        api-response (query-sso-service :validate-token params sso-opts)
        {:keys [issue?]} (-> api-response :body keywordize-keys)]
    (boolean
      (and (predicates/success? api-response)               ;; API request was processed..
           (not issue?)))))                                 ;; .. and no issue was discovered


(defn- password-issues
  "Verify that password fulfills requirements.
  Returns a map of issues if any, nil if ok"
  [password]
  (println "TODO: Implement validaten of new password")
  nil)



(defn- password-reset
  "Relay new password to SSO Service API"
  [token password sso-opts]
  {:pre [(valid? :reset/token token)]}
  (log/debug (format "Reset password with token token '%s'" token))
  (let [params       {:password password
                      :token    token}
        api-response (query-sso-service :password-reset params sso-opts)
        {:keys [timestamp issue? message role] :as result} (-> api-response :body keywordize-keys)]

    (if (and (predicates/success? api-response)             ;; API request was processed..
             (not issue?))
      (pages/feedback-page (assoc sso-opts
                             :feedback "New password assigned"
                             ;:message "You can close this page and follow link in email."
                             :timestamp (or timestamp (tu/time-now-local-str))
                             :redirect-url "/login"
                             :redirect-text "Click here to log in"))
      (pages/feedback-page (assoc sso-opts
                             :error "Assigning new password failed"
                             :message "Probably a backend issue and it might be permanent."
                             :timestamp (or timestamp (tu/time-now-local-str))
                             :redirect-url "/orderreset"
                             :redirect-text "Click here to try again"))
      )))




;|-------------------------------------------------
;| ROUTES

(defn auth-routes
  "Routes used for:
   - authentication in apps using plug-sso as a library
   - authentication for the built in admin UI of plug-sso when used as a service.

   sso-opts should be a map containing the following keys:
   :app - Name of the app. Presented at login and other html pages.
   :reset-capable? - If capability to send reset token by email is available" ;; TODO: Consider having clients (lib users) query this via API during init.
  [{:keys [reset-capable?] :as sso-opts}]
  {:pre [(valid? ::$/sso-opts sso-opts)]}
  (log/debug "sso-opts:" sso-opts)
  ["" #_{:middleware []}                                    ;;TODO: csrf
   ["/login" {:get  (fn [req]
                      (pages/login-page sso-opts))
              :post (fn [{:keys [params] :as req}]
                      ;(log/debug (format ">>>>!!!>>> PARAMS: %s" params))
                      (try
                        (login req sso-opts)
                        (catch Exception e
                          (log/warn "Login failed for " (:email params) "error:" (.getMessage e))
                          (redirect "/login"))))}]          ;; TODO: Add error message
   ["/logout" {:get logout}]
   (when reset-capable?
     ["/orderreset" {:get  (fn [req]
                             (pages/orderreset-page sso-opts))
                     :post (fn [{:keys [params] :as req}]
                             (let [host   (get-in req [:headers "origin"])
                                   params (assoc params :app-host host)]
                               (try
                                 (send-reset-email params sso-opts)
                                 (catch Exception e
                                   (log/error (format "Sending reset email failed with: %s" (.getMessage e)))
                                   (pages/feedback-page (assoc sso-opts
                                                          :error "Sorry, sending email failed!"
                                                          :message "Issue might very well be permanent"
                                                          :timestamp (tu/time-now-local-str) ;; Note: This timestamp will update if user refreshes the feedback page. Not perfect, but ok for now.
                                                          :redirect-url "/orderreset"
                                                          :redirect-text "Click here to try again"))))))}])
   (when reset-capable?
     ["/reset/:token" {:get  (fn [{:keys [path-params] :as req}]
                               (let [token (:token path-params)]
                                 (try
                                   (if (valid-reset-token? token sso-opts)
                                     (pages/reset-page sso-opts)
                                     (throw (Exception. "Some issue with token"))) ;; Just to handle all issues and errors in catch below.
                                   (catch Exception e
                                     (log/warn (format "Attempted access to reset form with invalid token '%s'" token))
                                     (pages/feedback-page (assoc sso-opts
                                                            :error "Problem with reset token"
                                                            :message "Make sure to use link in latest email if you did multiple reset requests."
                                                            :redirect-url "/orderreset"
                                                            :redirect-text "Click here to order a new one"))))))
                       :post (fn [{:keys [params path-params] :as req}]
                               (let [password (:new-password params)
                                     token    (:token path-params)]
                                 (try
                                   (let [[validation-issues _] (validate-password password)]
                                     (if-not (nil? validation-issues)
                                       (pages/reset-page (assoc sso-opts
                                                           :validation-error (-> validation-issues :password first)))
                                       (do
                                         (password-reset token password sso-opts)
                                         (pages/feedback-page (assoc sso-opts
                                                                :feedback "New password assigned"
                                                                :redirect-url "/login"
                                                                :redirect-text "Click here to login")))))
                                   (catch Exception e
                                     (log/error (format "Password reset using token '%s' failed with: %s" token (.getMessage e)))
                                     ))))}])
   ;["/invalidate" {:get (fn [request]                       ;;DEBUG
   ;                       (-> (response/ok {:session "invalidated"})
   ;                           (assoc :session {})))}]
   ])