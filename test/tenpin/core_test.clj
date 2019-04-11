(ns tenpin.core-test
  (:require [clojure.test :refer :all]
            [tenpin.core :refer :all]))

(deftest strike
  (testing "strike when first roll is 10"
    (is (true? (strike? [10 0])))
    (is (true? (strike? [10 1])))
    (is (true? (strike? [10 10])))
    (is (true? (strike? [10 10 10 10])))
    (is (false? (strike? [0 0])))
    (is (false? (strike? [0 1])))
    (is (false? (strike? [5 5])))
    (is (false? (strike? [3 4])))
    (is (false? (strike? [0 10])))))

(deftest spare
  (testing "spare when 2 rolls sum to 10"
    (is (true? (spare? [10 0])))
    (is (true? (spare? [0 10])))
    (is (true? (spare? [5 5])))
    (is (true? (spare? [1 9])))
    (is (true? (spare? [9 1])))
    (is (true? (spare? [9 1 1])))
    (is (true? (spare? [10 0 10])))
    (is (false? (spare? [0 0])))
    (is (false? (spare? [1 0])))
    (is (false? (spare? [0 1])))
    (is (false? (spare? [10])))
    (is (false? (spare? [3 4])))))