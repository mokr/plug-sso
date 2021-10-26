(ns plug-sso.lib.repl
  "Just some lib related REPL code"
  (:require [clojure.java.io :as io]
            [plug-sso.lib.pages :as pages]
            [selmer.parser :as parser]))


;|-------------------------------------------------
;| DEFINITIONS

(def ^:private resource-path
  (io/resource "auth"))


;|-------------------------------------------------
;| REPL

(comment
  (pages/login-page {:app            "Foo"
                     :error          "No access!"
                     :reset-capable? true})
  (parser/render-file "login.html" {:app "Yours"} {:custom-resource-path resource-path})
  (parser/render-file "login.html" {:app "Mine"} {:custom-resource-path "auth"})
  (io/resource "auth")
  )