(ns plug-sso.pages.management
  (:require
    [plug-field.ui.table :as table]
    [plug-sso.entities.user.data]
    [plug-sso.entities.user.management :as user-management]
    [plug-sso.entities.app.management :as app-management]
    [plug-sso.entities.access.management]
    [plug-sso.entities.access.data]
    [plug-sso.entities.app.data]
    [plug-sso.generic.icon :as icon]
    [plug-sso.generic.ui :as gui]
    [plug-utils.re-frame :refer [<sub >evt]]
    [plug-utils.reagent :refer [err-boundary]]
    [re-frame.core :as rf]))


(rf/reg-event-fx
  :init/management
  [rf/trim-v]
  (fn [{:keys [db]}]
    {:fx [[:dispatch [:fetch/users]]
          [:dispatch [:fetch/accesses]]
          [:dispatch [:fetch/apps]]]}))


;|-------------------------------------------------
;| CARDS

(defn apps-card []
  [:div.card
   [:header.card-header.has-background-info-light
    [:h4.card-header-title "Apps"]
    [:div.card-header-icon
     [icon/button {:name     "add"
                   :tooltip  "Click to create a new app"
                   :on-click #(>evt [:new/app])}]]]
   [:div.card-content>div.content
    [table/component (<sub [:apps/table-data])]]])


(defn users-card []
  [:div.card
   [:header.card-header.has-background-info-light
    [:h4.card-header-title "Users"]
    [:div.card-header-icon
     [icon/button {:name     "add"
                   :tooltip  "Click to create a new user"
                   :on-click #(>evt [:new/user])}]]]
   [:div.card-content>div.content
    [table/component (<sub [:users/table-data])]]])


(defn accesses-card []
  [:div.card
   [:header.card-header.has-background-info-light
    [:h4.card-header-title "Accesses"]
    [:div.card-header-icon
     [icon/regular {:name   "help"
                    :title  "Drag roles from app to user to create/change accesses"
                    :cursor "help"}]]]
   [:div.card-content>div.content
    [table/component (<sub [:accesses/table-data])]]])


(defn email-filtering []
  [:div.columns
   [:div.column.is-4]
   [:div.column.is-4
    [:div.box
     [gui/filter-term-input :user/email]]]
   [:div.column.is-4]])


;|-------------------------------------------------
;| PAGE

(defn page []
  [:section.section>div.container>div.content
   [:nav.level
    [:div.level-left
     [:h1 "Access management"]]
    [:div.level-right
     [:div.level-item.has-text-warning
      [icon/button {:name     "refresh"
                    :tooltip  "Click to re-fetch data from server"
                    :on-click #(>evt [:init/management])}]]]]
   [:br]
   [err-boundary
    (when (<sub [:user/edit-in-progress?])
      [user-management/edit-user-card])]
   [err-boundary
    (when (<sub [:app/edit-in-progress?])
      [app-management/edit-app-card])]
   [:br]
   [err-boundary
    [apps-card]]
   [:br]
   [email-filtering]
   [err-boundary
    [users-card]]
   [:br]
   [err-boundary
    [accesses-card]]])