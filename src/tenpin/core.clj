(ns tenpin.core
  (:require [clojure.string :as str]))

; denotes a frame yet to receive a roll or total score cannot be calculated yet
(def ^:const missing -1)

; 10 standard frames plus an extra 2 to handle the 10th frame
(def ^:const total-frames 12)

(defn legal-roll? [roll]
  "A single roll score is legal if its between 0 and 10 inclusive"
  (and (>= roll 0) (<= roll 10)))

(defn two-legal-rolls? [[roll1 roll2]]
  "true if both rolls have values between 0 and 10 inclusive"
  (and (legal-roll? roll1) (legal-roll? roll2)))

(defn strike? [[roll1 roll2]]
  "true when the first roll is 10 and the second roll is missing"
  (and (== roll1 10) (== roll2 missing)))

(defn -frame-score-satisfies? [frame f]
  "both rolls must be legal with the sum being compared to 10 by the provided equality fn"
  (and (two-legal-rolls? frame) (f (reduce + frame) 10)))

(defn spare? [frame]
  "both rolls must be between 0 and 10 inclusive and sum to 10"
  (frame-score-satisfies? frame ==))

(defn open? [frame]
  "both rolls must be between 0 and 10 inclusive and sum to less than 10"
  (frame-score-satisfies? frame <))

(defn valid-frame? [frame]
  "a frame is valid when its either an open, spare or strike frame"
  (or (open? frame) (spare? frame) (strike? frame)))

(defn score-frame [scorecard nth frame]
  "associates the nth frame with a new score and returns the updated scorecad"
  (assoc scorecard nth frame))

(defn resolve-frame-score
"
([10 -1] [10 -1] [10 -1])


"
  ([[head & tail]]
   (let [rolls (cond
                 (strike? head) (resolve-frame-score tail 2 [(first head)])
                 (spare? head) (resolve-frame-score tail 1 head)
                 (open? head) head
                 :else [])]
     (if (empty? rolls)
       missing
       (reduce + rolls))))
  ([[head & tail] n acc]
   (if (or (<= n 0) (empty? head))
     acc
     (cond
       (strike? head) (resolve-frame-score tail (dec n) (conj acc (first head)))
       (or (open? head) (spare? head)) (resolve-frame-score tail (- n 2) (into acc (take (min n 2) head)))
       :else []))))

(defn symbolize [frame]
  (cond
    (strike? frame) "X"
    (spare? frame) (str (first frame) " /")
    (open? frame) (str/join "," frame)
    :else (str/join "," frame)))

(defn full-scorecard [scorecard]
  (let [sliding-windows (partition 3 1 scorecard)]
    (map (fn [[head & _ :as frames]]
           {:score (resolve-frame-score frames) :symbol (symbolize head)}) sliding-windows)))

(defn total-score [scorecard]
  "returns the sum of 10 valid frames else -1 if no score can be calculated"
  (reduce (fn [acc next]
            (println next acc)
            (let [score (:score next)]
              (if (or (== acc missing) (== score missing))
                missing
                (+ score acc)))) 0 (full-scorecard scorecard)))
 

;; overfil after 10 is 1 element list of every next throw to calculate score for 10
(def c [[10 -1] [7 3] [7 2] [9 1] [10 -1] [10 -1] [10 -1] [2 3] [6 4] [7 3]        [3 5]  [-1 -1]])
(def d [[10 -1] [7 3] [7 2] [9 1] [10 -1] [10 -1] [10 -1] [2 3] [10 -1] [10 -1]    [10 -1] [10 -1]])
(def e [[10 -1] [7 3] [7 2] [9 1] [10 -1] [10 -1] [10 -1] [2 3] [10 -1] [10 -1]    [0 0]   [0 0]])
(def f [[10 -1] [10 -1] [-2 -1] [10 -1] [10 -1] [10 -1] [10 -1] [10 -1] [10 -1] [10 -1]    [10 -1]   [10 -1]])

(defn create-scorecard []
  "2d vector containing 12 frames with with 2 extra to handle strike and spares on the 10th frame"
  (->> (constantly [missing missing])
       (repeatedly total-frames)
       (into [])))

 






 