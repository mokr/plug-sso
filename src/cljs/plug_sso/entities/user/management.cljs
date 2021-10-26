(ns plug-sso.entities.user.management
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [plug-utils.re-frame :refer [<sub >evt]]
            [plug-utils.dom :refer [target-value]]
            [plug-fetch.core :as fetch]
            [re-frame.core :as rf]))

(def ^:private EDIT-USER-KEY :edited-user)

;|-------------------------------------------------
;| EDIT

(rf/reg-sub
  :edited/user
  (fn [db [_ field]]
    (if (some? field)
      (get-in db [EDIT-USER-KEY field])
      (get db EDIT-USER-KEY {}))))


(rf/reg-event-db
  :edit/user
  [rf/trim-v]
  (fn [db [id]]
    (js/console.info "EDIT USER" id)
    (if-let [user (->> db
                       :users
                       (filter #(-> % :db/id (= id)))
                       (first))]
      (assoc db EDIT-USER-KEY user)
      db)))


(rf/reg-event-db
  ::set-user-field
  [rf/trim-v]
  (fn [db [field value]]
    (assoc-in db [EDIT-USER-KEY field] value)))


;|-------------------------------------------------
;| USER ACTIONS

(rf/reg-event-db
  :new/user
  [rf/trim-v]
  (fn [db [_]]
    (assoc db EDIT-USER-KEY {})
    ))


(rf/reg-event-fx
  :delete/user
  [rf/trim-v]
  (fn [_ [id]]
    (fetch/make-fx-map-for-backend-event
      {:method :delete
       :uri    (str "/api/users/" id)
       :ok-fx  [[:dispatch [:fetch/users]]
                [:dispatch [:fetch/accesses]]]})))


(rf/reg-event-db
  :clear/user-form
  [rf/trim-v]
  (fn [db []]
    (dissoc db EDIT-USER-KEY)))


(rf/reg-event-fx
  :save/edited-user
  [rf/trim-v]
  (fn [{:keys [db]} [_]]
    (when-let [user (get db EDIT-USER-KEY)]
      (js/console.info "SAVE/UPSERT USER" user)
      (fetch/make-fx-map-for-backend-event
        {:method :post
         :uri    "/api/users"
         :params user
         :ok-fx  [[:dispatch [:fetch/users]]
                  [:dispatch [:clear/user-form]]]}))))


;|-------------------------------------------------
;| PREDICATE SUBSCRIPTIONS

(rf/reg-sub
  :user/edit-in-progress?
  (fn [db]
    (boolean (get db EDIT-USER-KEY))))

(rf/reg-sub
  :existing/user-being-edited?
  :<- [:edited/user]
  (fn [user]
    (boolean (:db/id user))))


;|-------------------------------------------------
;| HELPERS

(defn update-user [field & {:keys [formatter]}]
  (fn [e]
    (let [value (cond-> e
                        :always (target-value)
                        (fn? formatter) (formatter))]
      (>evt [::set-user-field field value]))))


;|-------------------------------------------------
;| UI

(defn form []
  [:form
   [:div.field
    [:label.label "Email *"]
    [:div.control.is-expanded>input.input {:type        "email"
                                           :placeholder "e.g. john.doe@example.com"
                                           :value       (<sub [:edited/user :user/email])
                                           :on-change   (update-user :user/email :formatter str/lower-case)}]]
   [:div.field
    [:label.label "Name"]
    [:div.control.is-expanded>input.input {:type        "text"
                                           :placeholder "e.g. John Doe"
                                           :value       (<sub [:edited/user :user/name])
                                           :on-change   (update-user :user/name)}]]
   [:div.field
    [:label.label "Company"]
    [:div.control.is-expanded>input.input {:type        "text"
                                           :placeholder "e.g. Acme"
                                           :value       (<sub [:edited/user :company/name])
                                           :on-change   (update-user :company/name)}]]
   [:div.field
    [:label.label "Department"]
    [:div.control.is-expanded>input.input {:type        "text"
                                           :placeholder "e.g. IT"
                                           :value       (<sub [:edited/user :company/department])
                                           :on-change   (update-user :company/department)}]]
   [:div.field
    [:label.label "Info"]
    [:div.control>input.input {:type        "text"
                               :placeholder "e.g. Tech lead"
                               :value       (<sub [:edited/user :user/info])
                               :on-change   (update-user :user/info)}]]
   [:div.field
    [:label.label "Password hash"]
    [:div.control>input.input {:type        "text"
                               :disabled    true
                               :placeholder "Password hash for user"
                               :value       (or (<sub [:edited/user :password/hash]) "")}]]
   [:div.field
    [:label.label "Reset token"]
    [:div.control>input.input {:type        "text"
                               :disabled    true
                               :placeholder "Reset token currently set for this user"
                               :value       (or (<sub [:edited/user :reset/token]) "")}]]
   ])


(defn edit-user-card []
  [:div.card.has-background-success-light.fade-in
   [:header.card-header.has-background-success
    [:p.card-header-title (if (<sub [:existing/user-being-edited?])
                            "Edit existing user"
                            "Create new user")]]
   [:div.card-content>div.content
    [form]]
   [:footer.card-footer
    [:div.card-footer-item>a.has-text-success {:on-click #(>evt [:save/edited-user])}
     "Save"]
    [:div.card-footer-item>a.has-text-success {:on-click #(js/console.info "Save and focus user")}
     "Save and focus"]
    [:div.card-footer-item>a.has-text-danger {:on-click #(>evt [:clear/user-form])}
     "Cancel"]]])