(ns plug-sso.entities.ui
  (:require [plug-utils.re-frame :refer [<sub]]))

(defn value-or-errror-message
  "Present the given key or present a helpful error message if missing."
  [{:keys [db/id] :as entity} key-to-present]
  [:span
   (or (some-> entity (get key-to-present) str)
       [:small [:em "Key " [:strong.has-text-danger (str key-to-present)] " undefined for ID " [:strong id]]])])


(defn ref-lookup
  "Use the provided re-frame sub 'query-id' to retrieve the referenced entity.
  Return a cell (td) with the value that entity has for 'key-to-present'

  Side note:
  At the point where this is used in config, we know what kind of entity is referenced, and hence also
  the keys that should be available for it."
  [ref key-to-present]
  {:pre  [(keyword? key-to-present)]
   :post [(vector? %)]}                                     ;; Returns hiccup
  (let [id     (:db/id ref)
        entity (<sub [:lookup/entity id])
        value  (if (not (some? entity))
                 [:span "unknown entity " [:strong.has-text-danger id]]
                 [value-or-errror-message entity key-to-present])]
    [:td
     value]))