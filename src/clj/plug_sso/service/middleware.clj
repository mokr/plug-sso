(ns plug-sso.service.middleware
  "Middleware for requests to the SSO service API")


(defn wrap-message-encryption
  "Decrypt incoming params.
  Encrypt response"
  [handler]
  ;;TODO: Implement before and after (in/out)
  ;(log/debug "Init encryption-middleware")
  (fn [request]
    ;(log/debug "Via encryption-middleware")
    (handler request)))

