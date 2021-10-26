(ns plug-sso.core
  (:require
    [day8.re-frame.http-fx]
    [reagent.dom :as rdom]
    [reagent.core :as r]
    [re-frame.core :as rf]
    [goog.events :as events]
    [goog.history.EventType :as HistoryEventType]
    [plug-sso.ajax :as ajax]
    [plug-sso.events]
    [plug-sso.generic.icon :as icon]
    [plug-sso.lib.events :as sso-event]
    [plug-sso.pages.about :as about]
    [plug-sso.pages.home :as home]
    [plug-sso.pages.management :as management]
    [plug-sso.entities.config :as entities-config]
    [plug-sso.entities.subs]
    [plug-utils.re-frame :refer [>evt]]
    [reitit.core :as reitit]
    [reitit.frontend.easy :as rfe])
  (:import goog.History))


(defn nav-link [uri title page]
  [:a.navbar-item
   {:href  uri
    :class (when (= page @(rf/subscribe [:common/page-id])) :is-active)}
   title])


(defn- top-right-icons-menu []
  [:div
   [icon/button {:name     "logout"
                 :class    "has-text-grey"
                 :tooltip  "Log out (not implemented yet)"
                 :on-click #(>evt [::sso-event/logout])}]])


(defn navbar []
  (r/with-let [expanded? (r/atom false)]
    [:nav.navbar.is-dark>div.container
     [:div.navbar-brand
      [:a.navbar-item {:href "/" :style {:font-weight :bold}} "SSO admin"]
      [:span.navbar-burger.burger
       {:data-target :nav-menu
        :on-click    #(swap! expanded? not)
        :class       (when @expanded? :is-active)}
       [:span] [:span] [:span]]]
     [:div#nav-menu.navbar-menu
      {:class (when @expanded? :is-active)}
      [:div.navbar-start
       [nav-link "#/" "Home" :home]
       [nav-link "#/management" "Management" :management]
       [nav-link "#/about" "About" :about]]
      [:div.navbar-end
       [:div.navbar-item
        [top-right-icons-menu]]]]]))


(defn page []
  (if-let [page @(rf/subscribe [:common/page])]
    [:div
     [navbar]
     [page]]))


(defn navigate! [match _]
  (rf/dispatch [:common/navigate match]))


(def router
  (reitit/router
    [["/" {:name        :home
           :view        #'home/page
           :controllers [{:start (fn [_] (rf/dispatch [:page/init-home]))}]}]
     ["/management" {:name        :management
                     :view        #'management/page
                     :controllers [{:start (fn [_] (rf/dispatch [:init/management]))}]}]
     ["/about" {:name :about
                :view #'about/page}]]))


(defn start-router! []
  (rfe/start!
    router
    navigate!
    {}))


;|-------------------------------------------------
;| INITIALIZE APP

(defn ^:dev/after-load mount-components []
  (rf/clear-subscription-cache!)
  (rdom/render [#'page] (.getElementById js/document "app")))


(defn init! []
  (start-router!)
  (ajax/load-interceptors!)
  (entities-config/register)
  (mount-components))
