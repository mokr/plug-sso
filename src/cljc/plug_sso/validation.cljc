(ns plug-sso.validation
  (:require [bouncer.core :as b]
            [bouncer.validators :as v]
    ;[struct.core :as st]
            ))


;;------------------------------
;; VALIDATORS

(v/defvalidator satisfies-length
                {:default-message-format "%s must be at least 10 charactes long."}
                [s]
                (v/min-count s 10))

(v/defvalidator has-number
                {:default-message-format "%s must contain at least one number."}
                [s]
                (v/matches s #"\d+"))

(v/defvalidator has-lowercase-letter
                {:default-message-format "%s must contain at least one lowercase character."}
                [s]
                (v/matches s #"[a-zæøå]+"))

(v/defvalidator has-uppercase-letter
                {:default-message-format "%s must contain at least one uppercase character."}
                [s]
                (v/matches s #"[A-ZÆØÅ]+"))


;;------------------------------
;; VALIDATION FUNCTIONS

(defn validate-password
  "Validate a new password the user wants to store."
  [password]
  (b/validate {:password password}
              :password [v/required
                         satisfies-length
                         has-number
                         has-lowercase-letter
                         has-uppercase-letter]))
