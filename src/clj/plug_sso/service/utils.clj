(ns plug-sso.service.utils
  (:require [plug-utils.time :as tu]
            [ring.util.http-response :as response]))


(defn respond-ok
  "Create an API response message containing:
   - a timestamp
   - any other data passed in"
  ([]
   (respond-ok nil))
  ([data]
   (response/ok
     (merge {:timestamp (tu/time-now-local-str)}
            data))))


; NOTE: Giving descriptive error messages leaks information from a security point-of-view, but for internal tools that seems like a preferable trade-off as users will save time.
(defn respond-with-issue
  "Create an API response message that explains why the operation could not be
  fulfilled. Eg. 'Wrong password' when trying to authenticate.

   Note: API request itself did not fail, but the outcome is considered negative."
  [error-message]
  (respond-ok {:issue?    true
               :timestamp (tu/time-now-local-str)
               :message   error-message}))
