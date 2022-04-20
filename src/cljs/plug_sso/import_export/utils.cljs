(ns plug-sso.import-export.utils
  (:require [cljs.pprint :as pp]))

;; Inspiration:
;; - http://blog.find-method.de/index.php?/archives/218-File-download-with-ClojureScript.html
;; - http://marianoguerra.org/posts/download-frontend-generated-data-to-a-file-with-clojurescript.html
;; - https://gist.github.com/zoren/cc74758198b503b1755b75d1a6b376e7


(defn create-file-blob [datamap mimetype]
  (js/Blob. [(with-out-str (pp/pprint datamap))] {"type" mimetype}))


(defn add-link-data-for
  "Add a blob of data to a dom node (anchor element) and give it the "
  [dom-node blob filename]
  (doto dom-node
    (set! -download filename)
    (set! -href (.createObjectURL js/URL blob))))
