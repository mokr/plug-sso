(ns plug-sso.generic.icon
  (:require [plug-utils.re-frame :refer [<sub >evt]]))


(defn regular
  "A regular material-icon"
  [{:keys [name class tooltip cursor]}]
  [:span.icon.material-icons
   {:title tooltip
    :class class
    :style {:cursor cursor}}
   name])


(defn button
  "An material-icon intended to be used with on-click"
  [{:keys [name
           class
           tooltip
           cursor
           on-click]}]
  [:span.icon
   {:title    tooltip
    :class    class
    :style    {:cursor (or cursor :pointer)}
    :on-click on-click}
   [:i.material-icons name]])