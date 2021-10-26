(ns plug-sso.entities.app.management
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [plug-utils.re-frame :refer [<sub >evt]]
            [plug-utils.dom :refer [target-value]]
            [plug-utils.debug :as d]
            [plug-fetch.core :as fetch]
            [re-frame.core :as rf]))


(def ^:private EDIT-APP-KEY :edited-app)


;|-------------------------------------------------
;| EDIT

(rf/reg-sub
  :edited/app
  (fn [db [_ field]]
    (if (some? field)
      (get-in db [EDIT-APP-KEY field])
      (get db EDIT-APP-KEY {}))))


(rf/reg-event-db
  :edit/app
  [rf/trim-v]
  (fn [db [id]]
    (js/console.info "EDIT APP" id)
    (if-let [app (->> db
                      :apps
                      (filter #(-> % :db/id (= id)))
                      (first))]
      (assoc db EDIT-APP-KEY app)
      db)))


(rf/reg-event-db
  :set/app-field
  [rf/trim-v]
  (fn [db [field value]]
    (assoc-in db [EDIT-APP-KEY field] value)))


;|-------------------------------------------------
;| APP ACTIONS

(rf/reg-event-db
  :new/app
  [rf/trim-v]
  (fn [db [_]]
    (assoc db EDIT-APP-KEY {})))


(rf/reg-event-fx
  :delete/app
  [rf/trim-v]
  (fn [_ [id]]
    (fetch/make-fx-map-for-backend-event
      {:method :delete
       :uri    (str "/api/apps/" id)
       :ok-fx  [[:dispatch [:fetch/apps]]
                [:dispatch [:fetch/accesses]]]})))


(rf/reg-event-db
  :clear/app-form
  [rf/trim-v]
  (fn [db []]
    (dissoc db EDIT-APP-KEY)))


(rf/reg-event-fx
  ::save-edited-app
  [rf/trim-v]
  (fn [{:keys [db]} [_]]
    (when-let [app (some->
                     (get db EDIT-APP-KEY)
                     (update :access/roles #(some-> % (str/split #",\s*") vec)))]
      (fetch/make-fx-map-for-backend-event
        {:method :post
         :uri    "/api/apps"
         :params app
         :ok-fx  [[:dispatch [:fetch/apps]]
                  [:dispatch [:clear/app-form]]]}))))


;|-------------------------------------------------
;| PREDICATE SUBSCRIPTIONS

(rf/reg-sub
  :app/edit-in-progress?
  (fn [db]
    (boolean (get db EDIT-APP-KEY))))

(rf/reg-sub
  :existing/app-being-edited?
  :<- [:edited/app]
  (fn [app]
    (boolean (:db/id app))))


;|-------------------------------------------------
;| HELPERS

(defn update-app [field & {:keys [formatter]}]
  (fn [e]
    (let [value (cond-> e
                        :always (target-value)
                        (fn? formatter) (formatter))]
      (>evt [:set/app-field field value]))))


;|-------------------------------------------------
;| UI

(defn form []
  [:form
   [:div.field
    [:label.label "Name *"]
    [:div.control.is-expanded>input.input {:type        "text"
                                           :placeholder "e.g. Support portal"
                                           :value       (<sub [:edited/app :app/name])
                                           :on-change   (update-app :app/name)}]]
   [:div.field
    [:label.label "Description"]
    [:div.control.is-expanded>input.input {:type        "text"
                                           :placeholder "e.g. Handle support tickets"
                                           :value       (<sub [:edited/app :app/description])
                                           :on-change   (update-app :app/description)}]]
   [:div.field
    [:label.label "Roles (comma separated)"]
    [:div.control.is-expanded>input.input {:type        "text"
                                           :placeholder "e.g. admin, user, fut"
                                           :value       (<sub [:edited/app :access/roles])
                                           :on-change   (update-app :access/roles :formatter str/lower-case)}]]])


(defn edit-app-card []
  [:div.card.has-background-success-light
   [:header.card-header.has-background-success
    [:p.card-header-title (if (<sub [:existing/app-being-edited?])
                            "Edit existing app"
                            "Create new app")]]
   [:div.card-content>div.content
    [form]]
   [:footer.card-footer
    [:div.card-footer-item>a.has-text-success {:on-click #(>evt [::save-edited-app])}
     "Save"]
    [:div.card-footer-item>a.has-text-success {:on-click #(js/console.info "Save and focus app")}
     "Save and focus"]
    [:div.card-footer-item>a.has-text-danger {:on-click #(>evt [:clear/app-form])}
     "Cancel"]]])

