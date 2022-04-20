(ns plug-sso.pages.import-export
  (:require [plug-sso.import-export.events]
            [plug-sso.import-export.subs]
            [plug-sso.import-export.ui.export :as export]
            [plug-sso.import-export.ui.import :as import]))


;|-------------------------------------------------
;| PAGE

(defn page []
  [:section.section>div.container>div.content
   [:div [:strong.has-text-danger "*alpha*"] [:em " Just a crude, first implementation"]]
   [:h3.title.is.3 "Export to file"]
   [:div.box
    [export/export-features]]
   [:h3.title.is.3 "Import from file"]
   [:div.box
    [import/file-open-or-info]
    [:br]
    [import/info-about-imported-data]]])