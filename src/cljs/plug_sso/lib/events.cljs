(ns plug-sso.lib.events
  (:require [re-frame.core :as rf]
            [plug-fetch.core :as fetch]
            [plug-utils.http :refer [redirect]]))


(rf/reg-event-fx
  ::logout
  (fn [{:keys [db]} [_ opts]]
    (redirect "/logout")))
