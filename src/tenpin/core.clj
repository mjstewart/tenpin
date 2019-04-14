(ns tenpin.core
  (:require [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]))

; denotes a frame yet to receive a roll or total score cannot be calculated yet
(def ^:const missing -1)

; 10 standard frames plus an extra 2 to handle the 10th frame
(def ^:const total-frames 12)

(defn legal-roll? [roll]
  "A single roll is legal if its between 0 and 10 inclusive"
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
  (-frame-score-satisfies? frame ==))

(defn open? [frame]
  "both rolls must be between 0 and 10 inclusive and sum to less than 10"
  (-frame-score-satisfies? frame <))

(defn valid-frame? [frame]
  "a frame is valid when its either an open, spare or strike frame"
  (or (open? frame) (spare? frame) (strike? frame)))

(defn find-next-rolls
  "Calculates the first frames score by looking ahead x number of rolls. If no score can be calculated, [] is returned"
  ([[head & _ :as frames]]
   (cond
     (or (strike? head) (spare? head)) (find-next-rolls frames 3)
     (open? head) (find-next-rolls frames 2)
     :else []))
  ([frames n]
   "Example for resolving the score of a strike
   find-next-rolls ([10 -1] [10 -1] [10 -1]) 3

   [10 10 10] (after flatten and filtering the missing placeholders)
   strike requires 3 rolls to compute the first frames score, otherwise its implied more frames need to be available
   "
   (->> frames
        (flatten)
        (filter #(not= missing %))
        (take n)
        (#(if (== (count %) n) % [])))))

(defn symbolize-frame10 [[head & rest :as all]]
  "frame 10 includes all fill frames that have counted towards the score"
  (str/trim (if (nil? head)
              ""
              (cond
                (== head 10) (str "X " (symbolize-frame10 rest))
                (== 10 (reduce + 0 (take 2 all))) (str head " / "  (symbolize-frame10 (drop 2 all)))
                :else (str head " " (symbolize-frame10 rest))))))

(defn symbolize-frame [i next-rolls [head & _]]
  "pretty prints a frame. frame 10 symbol includes any fill frames"
  (if (== i 9)
    (symbolize-frame10 next-rolls)
    (cond
      (strike? head) "X"
      (spare? head) (str (first head) " /")
      (open? head) (str/join " " head)
      :else "")))

(defn frame-stat [i [head & rest :as frames]]
  "returns a map with 3 keys :score, :symbol, :rolls
  :score - contains the computed score using any following frames
  :symbol - pretty symbol to represent the score
  :rolls - All the rolls in encounter order that were used to compute the score
  "
  (let [next-rolls (find-next-rolls frames)]
    {:score (if (empty? next-rolls) missing (reduce + next-rolls))
     :symbol (if (empty? next-rolls) "" (symbolize-frame i next-rolls frames))
     :rolls (into [] next-rolls)}))

(defn calculate-scorecard [scorecard]
  "Given the raw scorecard as a 2d list of frames, calculate the scores and sum the total.
   If the total is -1 then it means the game is not finished"
  (let [indexed-frames (map vector (range 0 (count scorecard)) (partition 3 1 scorecard))]
    (reduce (fn [acc [i [head & _ :as window]]]
              (let [frame-stat (frame-stat i window)]
                (-> acc
                    (update-in [:calculated-scorecard] #(conj % frame-stat))
                    (update-in [:total] #(if (or (== % missing) (== (:score frame-stat) missing))
                                           missing
                                           (+ % (:score frame-stat)))))))
            {:calculated-scorecard [] :total 0} indexed-frames)))

(defn finished? [calculated-scorecard-result]
  "true when the game has been finished indicated by a non -1 total score"
  (not= missing (:total calculated-scorecard-result)))

(defn score-frame [scorecard nth frame]
  "associates the nth frame with a new score and returns the updated scorecard.
   If the 10th frame is a spare then the 11th frame only needs to contain 1 roll and should be supplied as a vector [roll 0]
  "
  (assoc scorecard nth frame))

(defn create-scorecard []
  "2d vector containing 12 frames with with 2 extra to handle strike and spares on the 10th frame"
  (->> (constantly [missing missing])
       (repeatedly total-frames)
       (into [])))

;; specs

(s/def ::frame (s/and (s/tuple int? int?) valid-frame?))
(s/def ::scorecard (s/coll-of vector? :count total-frames))

(s/fdef score-frame
  :args (s/and (s/cat :scorecard ::scorecard :nth (s/int-in 0 total-frames) :frame ::frame)))

(s/fdef calculate-scorecard
  :args (s/and (s/cat :scorecard ::scorecard)))

(s/def ::score int?)
(s/def ::symbol string?)
(s/def ::total int?)
(s/def ::rolls any?)
(s/def ::frame-stat (s/keys :req [::score ::symbol ::rolls]))
(s/def ::calculated-scorecard (s/coll-of ::frame-stat :count 10))
(s/def ::calculated-scorecard-result-spec (s/keys :req [::calculated-scorecard ::total]))

; (s/fdef finished?
;   :args (s/cat :calculated-scorecard-result ::calculated-scorecard-result-spec))

(stest/instrument)