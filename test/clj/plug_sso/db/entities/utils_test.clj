(ns plug-sso.db.entities.utils-test
  (:require [clojure.test :refer :all]
            [plug-sso.db.utils :as $]))


(deftest maybe-test
  (testing "Valid input returning plain value"
    (are [x y] (= x y)
         "app1" ($/maybe :app/name "app1")
         "app1" ($/maybe :app/name {:app/name "app1"})
         "bob@example.com" ($/maybe :user/email {:user/email "bob@example.com"})
         "bob@example.com" ($/maybe :user/email "bob@example.com"))
    ;(is (= "app1"
    ;       ($/maybe-a :app/name "app1")))
    ;(is (= "app1"
    ;       ($/maybe-a :app/name {:app/name "app1"})))
    ;(is (= "bob@example.com"
    ;       ($/maybe-a :user/email "bob@example.com")))
    ;(is (= "bob@example.com"
    ;       ($/maybe-a :user/email {:user/email "bob@example.com"})))
    )
  (testing "Invalid returning nil"
    (is (nil? ($/maybe :user/email "bob")) "Not a valid email string")
    (is (nil? ($/maybe :user/email {:email "bob@example.com"})) "Wrong key in input")
    ))
