(ns plug-sso.lib.events
  (:require [re-frame.core :as rf]
            [plug-fetch.core :as fetch]
            [plug-utils.http :as http]))


(rf/reg-event-fx
  ::logout
  (fn [{:keys [db]} [_ opts]]
    (http/client-redirect "/logout")))
