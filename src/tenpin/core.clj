(ns tenpin.core)

(defn legal-roll? [roll]
  (and (>= roll 0) (<= roll 10)))

(defn two-rolls? [[roll-1 roll-2]]
  (and (legal-roll? roll-1) (legal-roll? roll-2)))

(defn strike? [[roll-1 roll-2]]
  (and (== roll-1 10) (== roll-2 -1)))

(defn frame-score-satisfies? [frame f]
  (and (two-rolls? frame) (f (reduce + frame) 10)))

(defn spare? [frame]
  (frame-score-satisfies? frame ==))

(defn open? [frame]
  (frame-score-satisfies? frame <))

(defn spare-or-open? [frame]
  (or (spare? frame) (open? frame)))

(defn frame-complete? [frame]
  (or (open? frame) (spare? frame) (strike? frame)))

 

;; (split-at 9 c)

;; overfil after 10 is 1 element list of every next throw to calculate score for 10
(def c [[10 -1] [7 3] [7 2] [9 1] [10 -1] [10 -1] [10 -1] [2 3] [6 4] [7 3]        [3 -1]  [-1 -1]])
(def d [[10 -1] [7 3] [7 2] [9 1] [10 -1] [10 -1] [10 -1] [2 3] [10 -1] [10 -1]    [10 -1] [10 -1]])
(def e [[10 -1] [7 3] [7 2] [9 1] [10 -1] [10 -1] [10 -1] [2 3] [10 -1] [10 -1]    [0 0]   [0 0]])


(defn create-overfill []
  [[0 0] [0 0]])

;; todo how to make this annon lambda? or just make it 12 . 10 + optional 2 overflows for strike on 10th
(defn create-scorecard []
  (concat (repeatedly 10 (fn [] [-1 -1])) (create-overfill)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]

  (println (calculate c)))
