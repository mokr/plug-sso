(ns plug-sso.specs
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]))


;|-------------------------------------------------
;| REGEXES

(def email-regex #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$")


;|-------------------------------------------------
;| SSO CONFIG AND SERVICE API

(s/def :sso/app string?)                                    ;; :sso/app to avoid conflict with ::app used for app entities below. TODO: Look into removing the one for entity.
(s/def ::reset-capable? boolean?)
(s/def ::sso-host string?)
(s/def ::sso-port pos-int?)
(s/def ::sso-port pos-int?)
(s/def ::sso-opts (s/keys :req-un [:sso/app ::sso-host ::sso-port]
                          :opt-un [::reset-capable?]))
(s/def ::sub-path (s/and string? #(str/starts-with? % "/")))


;|-------------------------------------------------
;| COMMON - used in multiple specs or entities

(s/def ::email (s/and string? #(re-matches email-regex %)))
(s/def ::password string?)                                  ;;TODO: Improve and use in form validation
(s/def ::identity ::email)                                  ;; Identity used in server sessions
(s/def :created/by ::email)                                 ;; For: user, access, app
(s/def :created/at inst?)
(s/def :modified/by ::email)
(s/def :modified/at inst?)
(s/def ::access-role string?)


;|-------------------------------------------------
;| FORM DATA

(s/def ::login-params (s/keys :req-un [::email ::password]))
(s/def ::password-reset-params (s/keys :req-un [::email ::password]))

;|-------------------------------------------------
;| ENTITY

(s/def :db/id pos-int?)
(s/def ::entity (s/keys :req [:db/id]))
(s/def ::eid pos-int?)
(s/def ::maybe-eid (s/nilable pos-int?))


;|-------------------------------------------------
;| USER

(s/def :user/email ::email)
(s/def :user/name string?)
(s/def :user/info string?)
(s/def :password/hash (s/and string? #(str/starts-with? % "bcrypt")))
(s/def :company/name string?)
(s/def :company/department string?)
(s/def :reset/url string?)
(s/def :reset/token string?)                                ;; Later: Verify that it is a stringified UUID

(s/def ::user (s/keys :req [:user/email]
                      :opt [:user/name                      ;; Optional to make it easy to have refs from simply {:user/email} in e.g. access entities. Can still enforce :user/name in creation form...
                            :user/info
                            :password/hash
                            :company/name
                            :company/department
                            :reset/url
                            :reset/token]))

(s/def ::users (s/coll-of ::user))

(s/def ::user-or-email (s/or :email ::email
                             :user ::user))

;|-------------------------------------------------
;| APPS

(s/def :app/name string?)
(s/def :app/description string?)
(s/def :app/url string?)
(s/def :access/roles (s/coll-of ::access-role :distinct true :into #{} :max-count 10)) ;; Not likely to have more than 10 available roles, so might want to take a look if we pass that limit

(s/def ::app (s/keys :req [:app/name]
                     :opt [:access/roles
                           :app/description
                           :app/url]))
(s/def ::apps (s/coll-of ::app))

(s/def ::app-or-app-name (s/or ::app-name :app/name
                               ::app ::app))

;|-------------------------------------------------
;| ACCESS

(s/def :access/to (s/or :ref ::eid                          ;; Ex: 3
                        :app ::app                          ;; Ex: {:app/name "app1"}
                        :entity ::entity))                  ;; Ex: {:db/id 3}
(s/def :access/for (s/or :ref ::eid
                         :user ::user                       ;; Ex: {:user/email "bob@example.com"}
                         :entity ::entity))
(s/def :access/role ::access-role)                          ;;NOTE: Actual values are app specific and managed in GUI
(s/def :last/login inst?)
(s/def :last/failed inst?)
(s/def :failed/logins pos-int?)

(s/def ::new-access (s/cat :user-id pos-int? :role string? :app-id pos-int?))

(s/def ::access (s/keys :req [:access/for
                              :access/to
                              :access/role]                 ;; Note: Other code need to verify that role is valid for given app
                        :opt [:last/login
                              :last/failed
                              :failed/logins]))
(s/def ::accesses (s/coll-of ::access))


;|-------------------------------------------------
;| SMTP

(def smtp-keys
  "Define required SMTP keys separately for use elsewhere in e.g. (select-keys env ...)"
  [:smtp-host :smtp-port :smtp-user :smtp-pass])

(s/def ::smtp-host (s/and string? #(str/starts-with? % "smtp.")))
(s/def ::smtp-port pos-int?)
(s/def ::smtp-user (s/and string? #(str/includes? % "@")))
(s/def ::smtp-pass string?)

(s/def :smtp/config (s/keys :req-un [::smtp-host ::smtp-port ::smtp-user ::smtp-pass]))