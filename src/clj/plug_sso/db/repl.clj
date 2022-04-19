(ns plug-sso.db.repl
  (:require [buddy.hashers :as hashers]
            [datalevin.core :as d]
            [plug-sso.db.core :as db]
            [plug-sso.db.entities.user :as user]
            [plug-sso.db.entities.app :as app]
            [plug-sso.db.entities.access :as access]
            [plug-sso.db.queries :as q]
            [plug-utils.string :as us]
            [plug-sso.db.utils :as du]
            [plug-sso.specs :as $]
            [clojure.spec.alpha :as s]))


;|-------------------------------------------------
;| DUMMY DATA

(def dummy-users
  [{:user/email  "bob@example.com"
    :user/name   "Bob Doe"
    :user/info   "Experienced"
    :reset/token (us/gen-uuid-str)
    :created/by  "admin@example.com"
    :modified/at (java.util.Date.)}
   {:user/email "alice@example.com"
    :user/name  "Alice"}
   {:user/email "chuck@example.com"
    :user/name  "Chuck"
    :user/info  "Rookie"}])

(def dummy-apps
  [{:app/name        "SSO admin"
    :app/description "Authentication and authorization for apps"
    :access/roles    ["admin"]}
   {:app/name        "app1"
    :app/description "Just some app"
    :access/roles    ["admin"]}
   {:app/name        "app2"
    :app/description "Log tool"
    :access/roles    ["admin" "user"]}
   {:app/name     "app3"
    :access/roles ["admin" "user" "fut"]}]
  )

(def dummy-relations
  [{:access/for  {:user/email "bob@example.com"}
    :access/to   {:app/name "app1"}
    :access/role "user"}
   {:access/for  {:user/email "alice@example.com"}
    :access/to   {:app/name "SSO admin"}
    :access/role "admin"}
   {:access/for  {:user/email "alice@example.com"}
    :access/to   {:app/name "app3"}
    :access/role "fut"}
   {:access/for  {:user/email "bob@example.com"}
    :access/to   {:app/name "app2"}
    :access/role "admin"}
   ])
;|-------------------------------------------------
;| POPULATE DB

(comment
  ;; Return e.g {:datoms-transacted 4}
  (do
    (d/transact! db/conn dummy-users)
    (d/transact! db/conn dummy-apps)
    (d/transact! db/conn dummy-relations)
    (->> (hashers/derive "Monkey-123")
         (user/assign-password-hash "alice@example.com"))
    )
  )

;|-------------------------------------------------
;| INSPECT DB

(comment
  (d/conn? db/conn)
  (d/schema db/conn)
  (d/datoms (d/db db/conn) :eav)
  )


;|-------------------------------------------------
;| CLEAR

(comment
  (d/clear db/conn)
  )


;|-------------------------------------------------
;| QUERY

(comment
  ;;USERS
  (user/list-of-users)
  (user/get-user-by-email "bob@example.com")
  (user/get-user-by-email "alice@example.com")
  (user/get-users-hash-and-role-for-app "bob@example.com" "SSO admin")
  (user/get-users-hash-and-role-for-app "bob@example.com" "invalid-app")
  (user/->eid "bob@example.com")
  (user/->eid {:user/email "bob@example.com"})
  (user/->eid "foo")
  (d/entity (d/db db/conn) [:user/email "bob@example.com"])
  (user/exists? "bob@example.com")
  (user/exists? {:user/email "bob@example.com"})
  (user/exists? "chuck@example.com")
  (user/get-user-by-email "chuck@example.com")
  (user/get-user-by-email "bob@example.com")
  (user/add-reset-token-to-user "bob@example.com")
  (user/add-reset-token-to-user "chuck@example.com")
  (user/remove-reset-token-from-user "bob@example.com")
  (user/delete-by-email "alice@example.com")
  (user/get-user-by-id 1)
  (user/get-user-by-id "1")
  (user/assign-reset-token "alice@example.com" (us/gen-uuid-str))
  (user/assign-reset-token "alice@example.com" "b82d7c8b-0647-476c-b033-ea954813d8f9")
  (user/valid-token? (java.util.UUID/fromString "16a9dd04-c9f0-4bd1-88d7-17276930c513"))
  (->> (hashers/derive "Monkey-123")
       (user/assign-password-hash "alice@example.com"))

  ;;APPS
  (app/list-of-apps)
  (app/->eid "app1")
  (app/->eid "app2")
  (app/->eid "app3")
  (app/exists? "app1")
  (app/exists? "app4")
  (app/available-roles "app1")
  (app/available-roles "app2")
  (app/available-roles "app3")
  (app/valid-role? "app2" "admin")
  (app/valid-role? "app2" :foo)

  ;;ACCESSES
  (access/list-of-accesses)
  (access/list-of-accesses-as-texts)
  (access/accesses-for-user "bob@example.com")
  (access/accesses-for-user "alice@example.com")
  (access/accesses-to-app "app2")
  (access/accesses-to-app "app1")
  (access/accesses-for-user-id 12)
  (access/accesses-for-user-id 59)
  (access/register-failed-login "alice@example.com" "app3")
  (access/register-successful-login "alice@example.com" "app3")
  (du/get-entity-as-map 59)
  (d/transact! db/conn [[:db/retract 59 :last/successful]])

  ;;ENTITIES
  (d/entity (d/db db/conn) [:app/name "app1"])
  (du/delete-entity 6)
  (du/get-entity 1)
  (-> (du/get-entity 1) :user/name)
  (du/get-entity-as-map 5)
  (du/get-entity 10)

  (->> (d/entity (d/db db/conn) 1)
       :user/name)

  (-> (du/get-entity 1)
      (select-keys [:user/name :user/email]))
  )

;|-------------------------------------------------
;| PULL

(comment
  (d/pull (d/db db/conn) '[*] 5)
  (d/pull (d/db db/conn) '[* {:access/for [*]} {:access/to [*]}] 5)
  (d/pull (d/db db/conn) '[* {:access/for [*]} {:access/to [*]}] 5)
  )

;|-------------------------------------------------
;| EXPERIMENTS

(comment

  (du/maybe :app/name "app1")
  (du/maybe :app/name {:app/name "app1"})
  (du/maybe :app/name :foo)

  (s/conform ::$/email "bob@example.com")
  (s/conform ::$/email "bob@example")
  (s/unform ::$/email "bob@example")
  (s/unform ::$/email "bob@example.com")

  ;PERF
  (time (app/available-roles "app2"))

  (-> (d/entity (d/db db/conn) [:user/email "alice@example.com"])
      (d/touch)
      ;(update :login/failed-count inc)
      )

  )