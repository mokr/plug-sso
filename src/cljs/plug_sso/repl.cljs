(ns plug-sso.repl
  (:require [plug-utils.re-frame :refer [<sub >evt]]))


(comment
  (set! *print-namespace-maps* false)

  (>evt [:fetch/export-transactions])
  (<sub [:export/transactions])
  )