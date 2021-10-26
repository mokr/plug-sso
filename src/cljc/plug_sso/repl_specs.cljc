(ns plug-sso.repl-specs
  "Just for experimenting with specs from REPL"
  (:require [clojure.spec.alpha :as s]
            [plug-sso.specs :as specs]))



(comment
  (s/conform ::specs/app {:app/name     "app1"
                          :access/roles #{"admin" :user "fut"}})
  (s/explain ::specs/app {:app/name     "app1"
                          :access/roles #{"admin" "user"}})

  (s/conform :app/name :foo)

  )