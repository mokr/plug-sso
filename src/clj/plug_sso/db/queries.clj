(ns plug-sso.db.queries
  "All Datalevin queries utilized in application")


;|-------------------------------------------------
;| USERS

(def list-of-users
  '[:find [(pull ?e [*]) ...]
    :where
    [?e :user/email]])


(def user-hash-and-role
  '[:find ?hash ?role                                       ;; [(pull ?usr [*]) ...]
    :in $ ?user-email ?app-name
    :where
    [?usr :user/email ?user-email]                          ;; Find user with given email ..
    [?acc :access/for ?usr]                                 ;; .. and user's accesses ..
    [?acc :access/to ?app]                                  ;; .. to some app ..
    [?app :app/name ?app-name]                              ;; .. where that app has given name. Then:
    [?usr :password/hash ?hash]                             ;; Get password hash for this user ..
    [?acc :access/role ?role]])                             ;; .. and the access role for the app


(def user-has-access-to-app?
  '[:find ?usr                                              ;; What we return is not of importance here.
    :in $ ?user-email ?app-name
    :where
    [?usr :user/email ?user-email]                          ;; Find user with given email ..
    [?acc :access/for ?usr]                                 ;; .. and user's accesses ..
    [?acc :access/to ?app]                                  ;; .. to some app ..
    [?app :app/name ?app-name]])                            ;; .. where that app has given name. Then:


;|-------------------------------------------------
;| APPS

(def list-of-apps
  '[:find [(pull ?e [*]) ...]                               ;; Return collection of app maps
    :where
    [?e :app/name]])


;|-------------------------------------------------
;| ACCESSES

(def list-of-accesses
  '[:find [(pull ?e [*]) ...]                               ;; Return collection of app maps
    :where
    [?e :access/for]
    [?e :access/to]])


(def list-of-accesses-as-text
  '[:find ?app-name ?email ?role                            ;; Return collection of app maps
    :where
    [?e :access/for ?user]
    [?e :access/to ?app]
    [?e :access/role ?role]
    [?user :user/email ?email]
    [?app :app/name ?app-name]])


(def accesses-for-user-email
  '[:find [(pull ?e [*]) ...]                               ;; Return collection of app maps
    :in $ ?user-email
    :where
    [?u :user/email ?user-email]
    [?e :access/for ?u]])


(def access-ids-for-user-id
  '[:find [?e ...]                                          ;; Return collection of app maps
    :in $ ?id
    :where
    [?e :access/for ?id]])


(def access-ids-for-app-id
  '[:find [?e ...]                                          ;; Return collection of app maps
    :in $ ?id
    :where
    [?e :access/to ?id]])


(def existing-access-for-user-to-app
  "Get the eID of an existing access for a given user to a specific app.
  As there can only exist _one_ access from a user to an app."
  '[:find [?e]
    :in $ ?user ?app
    :where
    [?e :access/for ?user]
    [?e :access/to ?app]])


(def accesses-to-app
  '[:find [(pull ?e [*]) ...]                               ;; Return collection of app maps
    :in $ ?app-name
    :where
    [?a :app/name ?app-name]
    [?e :access/to ?a]])


(def access-for-user-to-app
  '[:find ?acc .                                            ;; Find one (.)
    :in $ ?user-email ?app-name
    :where
    [?usr :user/email ?user-email]
    [?acc :access/for ?usr]
    [?acc :access/to ?app]
    [?app :app/name ?app-name]
    ;[?acc :failed/logins ?failed-count]
    ])


;|-------------------------------------------------
;| DATA EXPORT

(def export-users-data
  '[:find [(pull ?e [*]) ...]                               ;; Return collection of app maps
    :where
    [?e :user/email]])


(def export-apps-data
  "Export apps without :db/id"
  '[:find [(pull ?e [*]) ...]                               ;; Return collection of app maps
    :where
    [?e :app/name]])


(def export-accesses-data
  '[:find [(pull ?e [* {:access/for [:user/email]} {:access/to [:app/name]}]) ...] ;; Return collection of app maps
    :where
    [?e :access/for]
    [?e :access/to]])
