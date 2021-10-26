(ns plug-sso.db.schema)


(def schema {:user/email   {:db/valueType :db.type/string
                            :db/unique    :db.unique/identity}
             :reset/token  {:db/valueType :db.type/string
                            :db/unique    :db.unique/value}
             ;;ACCOUNT
             :access/for   {:valueType :db.type/ref}        ;; Ref to a user
             :access/to    {:valueType :db.type/ref}        ;; Ref to an app
             ;;APP
             :app/name     {:db/valueType :db.type/string
                            :db/unique    :db.unique/identity}
             :access/roles {:db/cardinality :db.cardinality/many}
             })
