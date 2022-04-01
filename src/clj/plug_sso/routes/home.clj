(ns plug-sso.routes.home
  (:require
    [clojure.string :as str]
    [taoensso.timbre :as log]
    [plug-sso.layout :as layout]
    [plug-sso.lib.core :as sso-lib]
    ;[clojure.java.io :as io]
    [plug-sso.middleware :as middleware]
    [ring.util.response]
    [ring.util.http-response :as response]
    [plug-sso.db.entities.access :as access]
    [plug-sso.db.entities.app :as app]
    [plug-sso.db.entities.user :as user]
    [plug-sso.db.utils :refer [add-creation-keys
                               add-modification-keys]]))


;|-------------------------------------------------
;| HELPERS

(defn- id-param-as-int
  "Extract id (string) from path-params and cast to int"
  [path-params]
  (some-> path-params :id Integer/parseInt))


(defn delete-by-id-handler
  "Create a generic handler for deleting something by ID (where :id is part of path-params).

  args:
  - type:   The type/kind of entity we want to delete in lowercase (for logging)
  - action: Function to be called with id to execute deletion"
  [type action]
  (fn [{:keys [identity path-params] :as req}]
    (if-let [id (id-param-as-int path-params)]
      (try
        (log/info (format "Deletion of %s ID %s requested by %s" type id (or identity "<unknown user>")))
        (action id)
        (response/ok (format "%s deleted" (str/capitalize type)))
        (catch Exception e
          (response/internal-server-error (.getMessage e))))
      (response/bad-request "'id' is a mandatory parameter!"))))


;|-------------------------------------------------
;| PAGES

(defn home-page [request]
  (layout/render request "home.html"))


;|-------------------------------------------------
;| ROUTES

(defn home-routes
  "Routes for the admin interface.
  Refer to plug-sso.handler/app-routes for the other routes"
  []
  [""
   {:middleware [sso-lib/wrap-protected
                 middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]
   ;; ***** API *****
   ["/api"
    ["/users" {:get  (fn [{:keys [identity] :as req}]
                       (println "identity:" identity)
                       (try
                         (some->
                           (user/list-of-users)
                           (response/ok))
                         (catch Exception e
                           (log/debug (format "USERS MESSAGE: %s -- full err: %s" (.getMessage e) (str e)))
                           (println "USERS ERROR" (str e))
                           (response/internal-server-error "Unable to provide users list at the moment"))))
               :post (fn [{user     :params
                           identity :identity
                           :or      {identity "admin@example.com"}}] ;; DEBUG (remove default identity when auth is in place)
                       (log/info (format "Adding user '%s' requested by %s" (str user) identity))
                       (try
                         ;;TODO: Assert identity or throw unauthorized
                         (if (user/exists? user)
                           (-> user
                               (add-modification-keys identity)
                               (user/upsert))
                           (-> user
                               (add-creation-keys identity)
                               (user/upsert)))
                         (response/ok)
                         (catch Exception e
                           (log/warn (format "Unable to create/modify user at the moment. Request from %s" identity))
                           (response/internal-server-error "Unable to create/modify user at the moment"))))}]
    ["/users/:id" {:delete (delete-by-id-handler "user" user/delete-by-id)}]
    ["/accesses" {:get  (fn [req]
                          (try
                            (some->
                              (access/list-of-accesses)
                              (response/ok))
                            (catch Exception e
                              (response/internal-server-error "Unable to provide accesses list at the moment"))))
                  :post (fn [{:keys [params] :as req}]
                          (try
                            (let [new-access (:new-access params)]
                              (access/upsert new-access)
                              (response/ok))
                            (catch Exception e
                              (response/internal-server-error "Unable save new access at the moment")
                              )))}]
    ["/accesses/:id" {:delete (delete-by-id-handler "access" access/delete-by-id)}]
    ["/apps" {:get  (fn [req]
                      (try
                        (some->
                          (app/list-of-apps)
                          (response/ok))
                        (catch Exception e
                          (response/internal-server-error "Unable to provide apps list at the moment"))))
              :post (fn [{app      :params
                          identity :identity
                          :or      {identity "admin@example.com"}}] ;; DEBUG (remove default identity when auth is in place)
                      (log/info (format "Adding app '%s' requested by %s" (str app) identity))
                      (try
                        ;;TODO: Assert identity or throw unauthorized
                        (if (:db/id app)
                          (-> app
                              (add-modification-keys identity)
                              (app/upsert))
                          (-> app
                              (add-creation-keys identity)
                              (app/upsert)))
                        (response/ok)
                        (catch Exception e
                          (log/warn (format "Unable to create/modify app at the moment. Request from %s" identity))
                          (response/internal-server-error "Unable to create/modify app at the moment")))
                      )}]

    ["/apps/:id" {:delete (delete-by-id-handler "access" app/delete-by-id)}]]
   ])

