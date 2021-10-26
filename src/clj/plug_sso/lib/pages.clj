(ns plug-sso.lib.pages
  (:require [selmer.parser :as parser]
            [clojure.java.io :as io]))

;;https://stackoverflow.com/questions/37337470/how-can-i-handle-html-files-in-luminus-which-arent-in-resources

;|-------------------------------------------------
;| DEFINITIONS

(def ^:private resource-path
  (io/resource "auth"))


;|-------------------------------------------------
;| PAGES

(defn login-page
  "opts should be a map containing the following keys:
   :app - Name of the app to log in to
   :reset-capable? - `true` if there is email capability in place to support it

   returns a response map with the login page as the body"
  [opts]
  {:headers {"Content-Type" "text/html; charset=utf-8"}
   :body    (parser/render-file "login.html" opts {:custom-resource-path resource-path})})


(defn orderreset-page
  "opts should be a map containing the following keys:
   :app - Name of the app to log in to

   returns a response map with the orderreset page as the body"
  [opts]
  {:headers {"Content-Type" "text/html; charset=utf-8"}
   :body    (parser/render-file "orderreset.html" opts {:custom-resource-path resource-path})})


(defn reset-page
  "Page where user defines a new password.
  Requires valid token in url
  opts should be a map containing the following keys:
   :app - Name of the app to log in to

   returns a response map with the orderreset page as the body"
  [opts]
  {:headers {"Content-Type" "text/html; charset=utf-8"}
   :body    (parser/render-file "reset.html" opts {:custom-resource-path resource-path})})


(defn feedback-page
  "Page that confirms that a new password has been assigned and provides like to login page"
  [opts]
  {:headers {"Content-Type" "text/html; charset=utf-8"}
   :body    (parser/render-file "feedback.html" opts {:custom-resource-path resource-path})})
