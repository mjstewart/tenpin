(ns tenpin.core-test
  (:require [clojure.test :refer :all]
            [tenpin.core :refer :all]
            [clojure.spec.alpha :as s]))

(deftest legal-rolls-scenarios
  (testing "single legal roll must be between 0 - 10 inclusive"
    (is (every? #(legal-roll? %) [0 5 10]))
    (is (every? #(not (legal-roll? %)) [-1 11])))
  (testing "both rolls must be between 0 - 10 inclusive"
    (is (every? #(two-legal-rolls? %) [[0 0] [10 10] [1 9] [9 1]]))
    (is (every? #(not (two-legal-rolls? %)) [[0 11] [11 0] [-1 10] [10 -1]]))))

(deftest strike-scenarios
  (testing "strike only when frame is [10 -1]"
    (is (true? (strike? [10 -1])))
    (is (every? #(not (strike? %)) [[10 10] [-1 10] [0 10] [10 0]]))))

(deftest spare-scenarios
  (testing "frame is a spare when both rolls are between 0 and 10 inclusive and sum to exactly 10"
    (is (every? #(spare? %) [[10 0] [0 10] [5 5] [1 9] [9 1]]))
    (is (every? #(not (spare? %)) [[-1 11] [15 -5] [0 0] [1 0] [0 1] [3 4]]))))

(deftest open-scenarios
  (testing "frame is open when both rolls are between 0 and 10 inclusive and sum to less than 10"
    (is (every? #(open? %) [[0 0] [9 0] [0 9] [1 8] [7 2]]))
    (is (every? #(not (open? %)) [[-1 -1] [10 0] [0 10] [5 5] [1 9] [9 1] [-1 11] [-2 11] [15 -5] [15 -6]]))))

(deftest valid-frame-scenarios
  (testing "a frame is valid if its either open, spare or a strike else false"
    (is (every? #(valid-frame? %) [[10 -1] [3 7] [0 0] [7 2]]))
    (is (every? #(not (valid-frame? %)) [[-1 -1] [10 10] [5 6] [0 -1]]))))

(deftest score-frame-scenarios
  (testing "updates the scorecard by replacing the nth frame"
    (is (= [[0 0] [-1 -1] [-1 -1] [-1 -1] [-1 -1] [-1 -1] [-1 -1] [-1 -1] [-1 -1] [-1 -1] [-1 -1] [-1 -1]]
           (score-frame (create-scorecard) 0 [0 0])) "frame 0 did not get updated")
    (is (= [[-1 -1] [-1 -1] [-1 -1] [-1 -1] [-1 -1] [-1 -1] [-1 -1] [-1 -1] [-1 -1] [-1 -1] [-1 -1] [0 0]]
           (score-frame (create-scorecard) 11 [0 0])) "frame 12 did not get updated")))

(deftest find-next-rolls-scenarios
  (testing "when frame 1 is missing"
    (is (= [] (find-next-rolls [[-1 -1] [-1 -1] [-1 -1]]))))
  (testing "when frame 1 is open"
    (is (= [0 0] (find-next-rolls [[0 0] [-1 -1] [-1 -1]])))
    (is (= [5 4] (find-next-rolls [[5 4] [-1 -1] [-1 -1]]))))
  (testing "when frame 1 is a strike"
    (is (= [] (find-next-rolls [[10 -1] [-1 -1] [-1 -1]])) "strike needs 2 more rolls")
    (is (= [] (find-next-rolls [[10 -1] [10 -1] [-1 -1]])) "strike needs 1 more roll")
    (is (= [10 10 10] (find-next-rolls [[10 -1] [10 -1] [10 -1]])) "3 strikes")
    (is (= [10 10 1] (find-next-rolls [[10 -1] [10 -1] [1 0]])) "2 strikes then use first open roll")
    (is (= [10 10 0] (find-next-rolls [[10 -1] [10 -1] [0 10]])) "2 strikes then use first open roll")
    (is (= [10 0 0] (find-next-rolls [[10 -1] [0 0] [10 -1]])) "strike then use both of frame 2 rolls")
    (is (= [10 3 2] (find-next-rolls [[10 -1] [3 2] [10 -1]])) "strike then use both of frame 2 rolls")
    (is (= [10 3 7] (find-next-rolls [[10 -1] [3 7] [10 -1]])) "strike then spare"))
  (testing "when frame 1 is a spare"
    (is (= [] (find-next-rolls [[7 3] [-1 -1] [-1 -1]])) "spare needs 1 more roll")
    (is (= [1 9 0] (find-next-rolls [[1 9] [0 0] [-1 -1]])) "spare uses first roll of next frame")
    (is (= [1 9 9] (find-next-rolls [[1 9] [9 1] [-1 -1]])) "spare uses first roll of next frame")
    (is (= [8 2 10] (find-next-rolls [[8 2] [10 -1] [10 -1]])) "spare uses first roll of next frame")))

(deftest calculate-scorecard-scenarios
  (testing "empty score scard has -1 score"
    (let [scorecard (into [] (repeatedly 12 (constantly [-1 -1])))
          calculated-scorecard-result (calculate-scorecard scorecard)]
      (is (= [{:score -1 :symbol "" :rolls []}
              {:score -1 :symbol "" :rolls []}
              {:score -1 :symbol "" :rolls []}
              {:score -1 :symbol "" :rolls []}
              {:score -1 :symbol "" :rolls []}
              {:score -1 :symbol "" :rolls []}
              {:score -1 :symbol "" :rolls []}
              {:score -1 :symbol "" :rolls []}
              {:score -1 :symbol "" :rolls []}
              {:score -1 :symbol "" :rolls []}]
             (:calculated-scorecard calculated-scorecard-result)))
      (is (= -1 (:total calculated-scorecard-result)))
      (is (false? (finished? calculated-scorecard-result)))))
  (testing "all gutter balls should be 0 total"
    (let [scorecard (into [] (repeatedly 12 (constantly [0 0])))
          calculated-scorecard-result (calculate-scorecard scorecard)]
      (is (= [{:score 0 :symbol "0 0" :rolls [0 0]}
              {:score 0 :symbol "0 0" :rolls [0 0]}
              {:score 0 :symbol "0 0" :rolls [0 0]}
              {:score 0 :symbol "0 0" :rolls [0 0]}
              {:score 0 :symbol "0 0" :rolls [0 0]}
              {:score 0 :symbol "0 0" :rolls [0 0]}
              {:score 0 :symbol "0 0" :rolls [0 0]}
              {:score 0 :symbol "0 0" :rolls [0 0]}
              {:score 0 :symbol "0 0" :rolls [0 0]}
              {:score 0 :symbol "0 0" :rolls [0 0]}]
             (:calculated-scorecard calculated-scorecard-result)))
      (is (= 0 (:total calculated-scorecard-result)))
      (is (true? (finished? calculated-scorecard-result)))))
  (testing "all strikes should have the max total of 300"
    (let [scorecard (into [] (repeatedly 12 (constantly [10 -1])))
          calculated-scorecard-result (calculate-scorecard scorecard)]
      (is (= [{:score 30 :symbol "X" :rolls [10 10 10]}
              {:score 30 :symbol "X" :rolls [10 10 10]}
              {:score 30 :symbol "X" :rolls [10 10 10]}
              {:score 30 :symbol "X" :rolls [10 10 10]}
              {:score 30 :symbol "X" :rolls [10 10 10]}
              {:score 30 :symbol "X" :rolls [10 10 10]}
              {:score 30 :symbol "X" :rolls [10 10 10]}
              {:score 30 :symbol "X" :rolls [10 10 10]}
              {:score 30 :symbol "X" :rolls [10 10 10]}
              {:score 30 :symbol "X X X" :rolls [10 10 10]}]
             (:calculated-scorecard calculated-scorecard-result)))
      (is (= 300 (:total calculated-scorecard-result)))
      (is (true? (finished? calculated-scorecard-result)))))
  (testing "all spares with 1 open fill frame that must have its second roll as zero"
    (let [scorecard [[9 1] [9 1] [9 1] [9 1] [9 1] [9 1] [9 1] [9 1] [9 1] [9 1] [5 0] [-1 -1]]
          calculated-scorecard-result (calculate-scorecard scorecard)]
      (is (= [{:score 19 :symbol "9 /" :rolls [9 1 9]}
              {:score 19 :symbol "9 /" :rolls [9 1 9]}
              {:score 19 :symbol "9 /" :rolls [9 1 9]}
              {:score 19 :symbol "9 /" :rolls [9 1 9]}
              {:score 19 :symbol "9 /" :rolls [9 1 9]}
              {:score 19 :symbol "9 /" :rolls [9 1 9]}
              {:score 19 :symbol "9 /" :rolls [9 1 9]}
              {:score 19 :symbol "9 /" :rolls [9 1 9]}
              {:score 19 :symbol "9 /" :rolls [9 1 9]}
              {:score 15 :symbol "9 / 5" :rolls [9 1 5]}]
             (:calculated-scorecard calculated-scorecard-result)))
      (is (= 186 (:total calculated-scorecard-result)))
      (is (true? (finished? calculated-scorecard-result)))))
  (testing "all spares with 1 overfill strike"
    (let [scorecard [[9 1] [9 1] [9 1] [9 1] [9 1] [9 1] [9 1] [9 1] [9 1] [9 1] [10 -1] [-1 -1]]
          calculated-scorecard-result (calculate-scorecard scorecard)]
      (is (= [{:score 19 :symbol "9 /" :rolls [9 1 9]}
              {:score 19 :symbol "9 /" :rolls [9 1 9]}
              {:score 19 :symbol "9 /" :rolls [9 1 9]}
              {:score 19 :symbol "9 /" :rolls [9 1 9]}
              {:score 19 :symbol "9 /" :rolls [9 1 9]}
              {:score 19 :symbol "9 /" :rolls [9 1 9]}
              {:score 19 :symbol "9 /" :rolls [9 1 9]}
              {:score 19 :symbol "9 /" :rolls [9 1 9]}
              {:score 19 :symbol "9 /" :rolls [9 1 9]}
              {:score 20 :symbol "9 / X" :rolls [9 1 10]}]
             (:calculated-scorecard calculated-scorecard-result)))
      (is (= 191 (:total calculated-scorecard-result)))
      (is (true? (finished? calculated-scorecard-result)))))
  (testing "sample game with spare overfil"
    (let [scorecard [[10 -1] [7 3] [7 2] [9 1] [10 -1] [10 -1] [10 -1] [2 3] [6 4] [7 3] [3 0] [-1 -1]]
          calculated-scorecard-result (calculate-scorecard scorecard)]
      (is (= [{:score 20 :symbol "X" :rolls [10 7 3]}
              {:score 17 :symbol "7 /" :rolls [7 3 7]}
              {:score 9 :symbol "7 2" :rolls [7 2]}
              {:score 20 :symbol "9 /" :rolls [9 1 10]}
              {:score 30 :symbol "X" :rolls [10 10 10]}
              {:score 22 :symbol "X" :rolls [10 10 2]}
              {:score 15 :symbol "X" :rolls [10 2 3]}
              {:score 5 :symbol "2 3" :rolls [2 3]}
              {:score 17 :symbol "6 /" :rolls [6 4 7]}
              {:score 13 :symbol "7 / 3" :rolls [7 3 3]}]
             (:calculated-scorecard calculated-scorecard-result)))
      (is (= 168 (:total calculated-scorecard-result)))
      (is (true? (finished? calculated-scorecard-result)))))
  (testing "sample game with no overfills"
    (let [scorecard [[10 -1] [7 3] [7 2] [9 1] [10 -1] [10 -1] [10 -1] [2 3] [6 4] [0 0] [-1 -1] [-1 -1]]
          calculated-scorecard-result (calculate-scorecard scorecard)]
      (is (= [{:score 20 :symbol "X" :rolls [10 7 3]}
              {:score 17 :symbol "7 /" :rolls [7 3 7]}
              {:score 9 :symbol "7 2" :rolls [7 2]}
              {:score 20 :symbol "9 /" :rolls [9 1 10]}
              {:score 30 :symbol "X" :rolls [10 10 10]}
              {:score 22 :symbol "X" :rolls [10 10 2]}
              {:score 15 :symbol "X" :rolls [10 2 3]}
              {:score 5 :symbol "2 3" :rolls [2 3]}
              {:score 10 :symbol "6 /" :rolls [6 4 0]}
              {:score 0 :symbol "0 0" :rolls [0 0]}]
             (:calculated-scorecard calculated-scorecard-result)))
      (is (= 148 (:total calculated-scorecard-result)))
      (is (true? (finished? calculated-scorecard-result)))))
  (testing "sample game with 2 strike overfils"
    (let [scorecard [[10 -1] [7 3] [7 2] [9 1] [10 -1] [10 -1] [10 -1] [2 3] [6 4] [10 -1] [10 -1] [9 0]]
          calculated-scorecard-result (calculate-scorecard scorecard)]
      (is (= [{:score 20 :symbol "X" :rolls [10 7 3]}
              {:score 17 :symbol "7 /" :rolls [7 3 7]}
              {:score 9 :symbol "7 2" :rolls [7 2]}
              {:score 20 :symbol "9 /" :rolls [9 1 10]}
              {:score 30 :symbol "X" :rolls [10 10 10]}
              {:score 22 :symbol "X" :rolls [10 10 2]}
              {:score 15 :symbol "X" :rolls [10 2 3]}
              {:score 5 :symbol "2 3" :rolls [2 3]}
              {:score 20 :symbol "6 /" :rolls [6 4 10]}
              {:score 29 :symbol "X X 9" :rolls [10 10 9]}]
             (:calculated-scorecard calculated-scorecard-result)))
      (is (= 187 (:total calculated-scorecard-result)))
      (is (true? (finished? calculated-scorecard-result)))))
  (testing "sample game that has been started but not finished"
    (let [scorecard [[10 -1] [7 3] [7 2] [9 1] [-1 -1] [-1 -1] [-1 -1] [-1 -1] [-1 -1] [-1 -1] [-1 -1] [-1 -1]]
          calculated-scorecard-result (calculate-scorecard scorecard)]
      (is (= [{:score 20 :symbol "X" :rolls [10 7 3]}
              {:score 17 :symbol "7 /" :rolls [7 3 7]}
              {:score 9 :symbol "7 2" :rolls [7 2]}
              {:score -1 :symbol "" :rolls []}
              {:score -1 :symbol "" :rolls []}
              {:score -1 :symbol "" :rolls []}
              {:score -1 :symbol "" :rolls []}
              {:score -1 :symbol "" :rolls []}
              {:score -1 :symbol "" :rolls []}
              {:score -1 :symbol "" :rolls []}]
             (:calculated-scorecard calculated-scorecard-result)))
      (is (= -1 (:total calculated-scorecard-result)))
      (is (false? (finished? calculated-scorecard-result))))))
