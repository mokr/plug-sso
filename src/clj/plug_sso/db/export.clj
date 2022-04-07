(ns plug-sso.db.export
  (:require
    [datalevin.core :as d]
    [plug-sso.db.core :as db]
    [plug-sso.db.queries :as q]))


;|-------------------------------------------------
;| HELPERS

(defn ^:private remove-db-id [m]
  (dissoc m :db/id))


;|-------------------------------------------------
;| PARTIAL DB EXPORT

(defn- users-data
  "Return all users without :db/id"
  []
  (->>
    (d/q q/export-users-data
         (d/db db/conn))
    (map remove-db-id)))


(defn- apps-data
  "Return all apps without :db/id"
  []
  (->>
    (d/q q/export-apps-data
         (d/db db/conn))
    (map remove-db-id)))


(defn- accesses-data
  "Return all accesses including essential attrs for (lookup) refs"
  []
  (->>
    (d/q q/export-accesses-data
         (d/db db/conn))
    (map remove-db-id)))


;|-------------------------------------------------
;| DB DATA EXPORT

(defn db-data-as-map
  "Export as data without :db/id.
  A \"pure\" data approach not affected by how DB happened to store data generated IDs."
  []
  {:post [(map? %)]}
  {:users    (users-data)
   :apps     (apps-data)
   :accesses (accesses-data)})


(defn db-as-transaction-data
  "Export as data without :db/id.
  Return a collection of Db entities as maps that can be transacted"
  []
  {:post [(sequential? %)]}
  (concat (users-data)
          (apps-data)
          (accesses-data)))
