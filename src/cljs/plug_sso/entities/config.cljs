(ns plug-sso.entities.config
  (:require [cljs.reader :refer [read-string]]
            [plug-sso.entities.events]
            [plug-sso.entities.custom-rendering :as render]
            [plug-field.re-frame :as pfrf]
            [plug-utils.re-frame :refer [>evt]]
            [plug-utils.time :refer [inst->str]]
            [plug-utils.debug :as d]
            [re-frame.core :as rf]
            [clojure.string :as str]))


(def user-target-fields
  [:db/id
   :user/email
   :user/name
   :user/info
   :company/name
   :company/department
   :password/hash
   :reset/token
   :_modify/user])


(def app-target-fields
  [:db/id
   :app/name
   :access/roles
   :app/description
   :app/url
   :_modify/app])


(def accesses-target-fields
  [:db/id
   :access/for
   :access/to
   :access/role
   :last/successful-login
   :failed/logins
   :last/failed-login
   :_modify/access])


(defn- key-as-tooltip [_ {:keys [k description]}]
  (str "Field  " k
       (when description (str "\n" description))))


(def field-config
  {:db/id                 {:display     "ID"
                           :tag         :td>span.tag.has-text-dark
                           :tooltip     key-as-tooltip
                           :description "DB entity ID"}
   :user/name             {:display "Name" :tooltip key-as-tooltip}
   :user/info             {:display "Info" :tooltip key-as-tooltip}
   :password/hash         {:display "Hash" :tooltip key-as-tooltip}
   :reset/token           {:display     "Token" :tooltip key-as-tooltip
                           :description "On-time token to be used for password reset"}
   :company/name          {:display "Company" :tooltip key-as-tooltip}
   :company/department    {:display "Department" :tooltip key-as-tooltip}
   :user/email            {:display     "Email" :tooltip key-as-tooltip
                           :description "Email represents a unique ID in DB"}
   :app/name              {:display     "App"
                           :description "App name represents a unique ID in DB"
                           :tooltip     key-as-tooltip}
   :app/description       {:display "Description" :tooltip key-as-tooltip}
   :app/url               {:display "URL" :tooltip key-as-tooltip}
   :access/roles          {:display "Roles" :tooltip key-as-tooltip}
   :access/for            {:display "User" :tooltip key-as-tooltip}
   :access/role           {:display "Role" :tooltip key-as-tooltip}
   :access/to             {:display "App" :tooltip key-as-tooltip}
   :last/successful-login {:display "Last login" :tooltip key-as-tooltip}
   :failed/logins         {:display "Failed count" :tooltip key-as-tooltip}
   :last/failed-login     {:display "Last failed" :tooltip key-as-tooltip}
   :_modify/user          {:display ""}
   :_modify/app           {:display ""}
   :_modify/access        {:display ""}})


;|-------------------------------------------------
;| FIELD VALUE HELPERS

(defn- extract-transferred-data
  "Extract data from a drag event"
  [e]
  (some-> e
          .-dataTransfer
          (.getData "text/plain")
          (read-string)))
;
(defn- pretty-timestamp
  "Convert inst to formatted string"
  [this]
  (some-> this :v inst->str))


(defn- yes-if-set
  "Display \"yes\" if this field has a value"
  [{:keys [v]}]
  (when (some? v)
    "yes"))

;|-------------------------------------------------
;| FIELD VALUE CONFIG

(def field-value-config
  {:db/id                 {:class ["has-text-grey"]
                           :tag   :td>span.tag.is-light}
   :user/name             {:tooltip "Users name"}
   :user/email            {:tooltip "Email is a unique identifier in DB"
                           :render  (fn [{user-id :id
                                          display :display
                                          :as     this}]
                                      [:td {:on-drop      (fn [e]
                                                            (let [[app-id role] (extract-transferred-data e)
                                                                  new-access [user-id role app-id]]
                                                              (>evt [:upsert/access new-access])))
                                            :on-drag-over (fn [e]
                                                            (.preventDefault e))}
                                       display])}
   :password/hash         {:display yes-if-set
                           :class   ["has-text-primary-dark"]
                           :tooltip (fn [{:keys [password/hash]}]
                                      hash)}
   :reset/token           {:display yes-if-set
                           :class   ["has-text-warning-dark"]
                           :tooltip (fn [{:keys [reset/token]}]
                                      token)}
   :app/url               {:render (fn [{:keys [v] :as this}]
                                     [:td
                                      [:a {:href  v :target "_blank" :rel "noreferrer"
                                           :style {:cursor "pointer"}}
                                       v]])}
   :access/roles          {
                           ;:value-formatter str
                           :render render/roles-as-tags}
   :access/for            {
                           ;:lookup-as       :user
                           ;:value-formatter #(:db/id %)
                           :render (render/ref-renderer-presenting :user/name)
                           ;:lookup          user-lookup'
                           }
   :last/successful-login {:tag     :td>small.has-text-success-dark
                           :display pretty-timestamp}
   :last/failed-login     {:tag     :td>small.has-text-danger-dark
                           :display pretty-timestamp}
   :failed/logins         {:class "has-text-danger has-text-centered"}
   :access/to             {
                           ;:value-formatter #(:db/id %)
                           :render (render/ref-renderer-presenting :app/name)
                           }
   :_modify/user          {:render (render/modification-icons-for "user" {:edit-event   :edit/user
                                                                          :delete-event :delete/user})}
   :_modify/app           {:render (render/modification-icons-for "app" {:edit-event   :edit/app
                                                                         :delete-event :delete/app})}
   :_modify/access        {:render (render/modification-icons-for "access" {:delete-event :delete/access})}})


;|-------------------------------------------------
;| REGISTER CONFIG

;;TODO: Handle init and reloading. Use mount?
(rf/dispatch-sync [::pfrf/add-key-config field-config])
(rf/dispatch-sync [::pfrf/add-value-config field-value-config])

(defn register []
  ;(rf/dispatch-sync [::pfrf/add-key-config field-config])
  ;(rf/dispatch-sync [::pfrf/add-value-config field-value-config])
  )