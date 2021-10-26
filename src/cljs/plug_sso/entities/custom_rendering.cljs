(ns plug-sso.entities.custom-rendering
  "Render functions for plug-field Field records, or higher other function that create such functions."
  (:require [plug-sso.generic.icon :as icon]
            [plug-sso.entities.ui :as ui]
            [plug-utils.re-frame :refer [>evt]]))


;|-------------------------------------------------
;| HIGHER ORDER (FACTORIES)

(defn modification-icons-for
  "Create a Field render function that produces a row of icons for modifying the given type.

  ARGS:
  -----
  type         - e.g. \"user\"
  opts-map:
     edit-event   - e.g. :edit/user
     delete-event - e.g. :delete/user "
  [type {:keys [edit-event delete-event]}]
  {:pre  [(string? type) (or (keyword? edit-event) (nil? edit-event)) (or (keyword? delete-event) (nil? edit-event))]
   :post [(fn? %)]}
  (fn render-modification-icons [{:keys [id] :as this}]
    [:td.has-text-right.has-text-grey
     [:div.level
      [:div.level-item
       (when (some? edit-event)
         [icon/button {:name     "edit"
                       :tooltip  (str "Modify " type)
                       :on-click #(>evt [edit-event id])
                       :cursor   "pointer"}])]
      [:div.level-item
       (when (some? delete-event)
         [icon/button {:name     "clear"
                       :tooltip  (str "Delete " type)
                       :on-click #(>evt [:with/confirmation delete-event id])
                       :cursor   "pointer"}])]]]))


(defn ref-renderer-presenting
  "Create a component that looks up the ref in a Field's :v key and presents the desired key from that entity.

   The ref in :v has the form {:db/id _}"
  [key-to-present]
  {:pre [(keyword? key-to-present)]}                        ;; App uses keyword keys exclusively
  (fn
    [{:keys [v] :as this}]
    [ui/ref-lookup v key-to-present]))


;|-------------------------------------------------
;| FIELD RENDERING FUNCTIONS

(defn roles-as-tags
  "Field render function that takes coll of string from :v and renders them as tags
  assuming they represent access roles for this app."
  [{app-id :id
    roles  :v
    :as    this}]
  [:td
   [:div.level
    [:div.level-left
     (for [role roles]
       ^{:key role}
       [:div.level-item
        [:span.tag.is-info {:style         {:cursor "grab"}
                            :draggable     true
                            :on-drag-start (fn [e]
                                             (-> e .-dataTransfer (.setData "text/plain" [app-id role])))}
         role]])]]])