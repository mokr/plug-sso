(ns plug-sso.import-export.effects
  (:require [re-frame.core :as rf]))



(rf/reg-fx
  :console
  (fn [text]
    (js/console.info text)))
