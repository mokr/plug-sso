(ns plug-sso.db.import)


(defmulti category-based-import :category)


(defmethod category-based-import :default [{:keys [category]}]
  (throw (IllegalArgumentException.
           (format "Don't know how to import transactions for category %s" category))))