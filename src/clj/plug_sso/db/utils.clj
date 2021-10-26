(ns plug-sso.db.utils
  (:require [taoensso.timbre :as log]
            [clojure.spec.alpha :as s]
            [datalevin.core :as d]
            [plug-utils.spec :refer [valid?]]
            [plug-sso.specs :as spec'ed]
            [plug-sso.db.core :as db])
  (:import [java.util Date]))


(defn nil-if-invalid [x]
  (if (= :clojure.spec.alpha/invalid x)
    nil
    x))


(defn maybe
  "Try to coerce/unwrap value into something that conforms to spec for given key.

  Value is either a plain value, or a value that is typically stored
  under the given (and spec'ed) key.

  Otherwise, return nil

  Example:
  Both [\"foo\" :some/key]
  and  [{:some/key \"foo\"}]
  will return \"foo\" if \"foo\" conforms to spec for :some/key"
  [key value]
  (->> (get value key value)                                ;; Try to extract in case value is a map. Fall back to plain value otherwise
       (s/conform key)                                      ;; Conform assuming there is a defined spec for this key. Like (s/def :app/name ,,,) if key = :app/name
       (nil-if-invalid)))


(defn get-entity
  "Get entity as a map like structure where keys are lazily fetched"
  [id]
  (some->
    (d/entity (d/db db/conn) id)))


(defn get-entity-as-map
  "Get full entity as a map. (not a lazy psoudo map)"
  [id]
  (-> (get-entity id)
      (d/touch)))


(defn delete-entity
  [id]
  (d/transact! db/conn [[:db/retractEntity id]]))


(defn delete-entities
  [ids]
  (d/transact! db/conn
               (mapv #(vector :db/retractEntity %) ids)))   ;; Create vector of vectors.


(defn add-creation-keys
  "Add keys that many entities have for tracking when and who added them"
  [m identity]
  {:pre [(valid? ::spec'ed/identity identity)]}
  (assoc m
    :created/by identity
    :created/at (Date.)))


(defn add-modification-keys
  "Add keys that many entities have for tracking when and who modified them"
  [m identity]
  {:pre [(valid? ::spec'ed/identity identity)]}
  (assoc m
    :modified/by identity
    :modified/at (Date.)))