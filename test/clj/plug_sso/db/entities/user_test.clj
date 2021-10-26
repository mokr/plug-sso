(ns plug-sso.db.entities.user-test
  (:require [clojure.test :refer :all]
            [datalevin.core :as d]
            [plug-sso.db.core :as db]
            [plug-sso.db.user :as $ :refer [get-user-by-token]]))


;;TODO: Look into use-fixtures (with mount)
(defn fake-conn-with [data]
  (let [fake-conn (d/create-conn nil db/schema)]
    (d/transact! fake-conn data)
    fake-conn))


(def test-token #uuid"4a1e987b-f8ee-40f3-9f33-c2eb258d2180")
(def test-email "bob@example.com")
(def test-data [{:user/email  test-email
                 :user/name   "Bobby"
                 :reset/token test-token}])


;|-------------------------------------------------
;| TESTS

(deftest get-user-by-email-test
  (with-redefs [db/conn (fake-conn-with test-data)]
    (testing "Finding user by email"
      (is "Bobby"
          (:user/name
            ($/get-user-by-token test-token))))))


(deftest get-user-by-token-test
  (with-redefs [db/conn (fake-conn-with test-data)]
    (testing "Finding user by token"
      (is (= "Bobby"
             (:user/name
               ($/get-user-by-email test-email)))))))
