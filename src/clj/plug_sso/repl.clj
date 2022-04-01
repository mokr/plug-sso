(ns plug-sso.repl
  "REPL code for plug-sso"
  (:require [plug-sso.service.email :as email]
            [plug-sso.config :refer [env]]
            [plug-sso.db.entities.app :as app]
            [plug-sso.db.entities.user :as user]
            [plug-sso.db.entities.access :as access]
            [plug-sso.db.export :as export]))


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
  (set! *print-namespace-maps* false)

  (email/smtp-config-from-env env)

  (app/list-of-apps)
  (user/list-of-users)
  (access/list-of-accesses)

  (export/db-data-as-map)
  (export/db-data-as-transaction-data)
  )