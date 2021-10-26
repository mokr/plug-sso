(ns plug-sso.service.email
  (:require [clojure.spec.alpha :as s]
            [plug-sso.specs :as $]
            [plug-utils.spec :refer [valid?]]
            [postal.smtp :as smtp]
            [postal.core :as postal]
            [plug-sso.db.entities.user :as user]
            [taoensso.timbre :as log]))



(defn smtp-config-from-env
  "Extract and validate SMTP config from ENV"
  [environment]
  {:pre [(map? environment)]}
  (let [smtp-config (select-keys environment $/smtp-keys)]
    (assert (valid? :smtp/config smtp-config) "Server config for sending email is invalid")
    smtp-config))


(defn- email-content
  "Body content for reset email.

  ARGS:
  host  = Host where application is running that link should point to
  token = The reset token that should be part of the URL"
  [app host token]
  (format "Click <a href=\"%s/reset/%s\">this link</a> to set a new password for %s" host token app)) ;;TODO: Separation between host and app-name


(defn send-reset-email [{:keys [app email reset-token app-host]} smtp-config]
  {:pre [(valid? :user/email email)
         (valid? :reset/token reset-token)
         (string? app-host)]}
  (log/info (format "About to send reset mail to %s with token '%s' and host '%s'" email reset-token app-host))
  (log/debug (format "SMTP CONF: %s" smtp-config))
  (let [{:keys [smtp/host smtp/port smtp/user smtp/pass]} smtp-config
        args   {:host   host
                :port   port
                :tls    :y
                ;:debug  true                                ;;=> ...Broken pipe (Write failed)...
                :sender user
                :user   user
                :pass   pass}
        msg    {:from    user
                :to      email
                :subject (format "%s password reset" app)
                :body    [{:type    "text/html"
                           :content (email-content app app-host reset-token)}]}
        result (smtp/smtp-send args msg)]
    (log/info (format "Reset email sent to %s" email))
    (log/debug "SMTP result" result)
    result))
