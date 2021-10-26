(ns plug-sso.repl
  "REPL code for plug-sso"
  (:require [plug-sso.service.email :as email]
            [plug-sso.config :refer [env]]))


;|-------------------------------------------------
;| HELPERS

(defn test-email-to
  "Send an email from REPL"
  [email]
  (email/send-reset-email
    {:email       email
     :reset-token "4a1e987b-f8ee-40f3-9f33-c2eb258d2180"
     :app-host    "Testapp"}
    (email/smtp-config-from-env env)))


;|-------------------------------------------------
;| REPL

(comment
  (email/smtp-config-from-env env)

  )