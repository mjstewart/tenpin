# tenpin

Matt Stewart

smtp.matt@gmail.com


## Installation

Easiest to run via leiningen or load `core.clj` in the repl.
 
# API


## `create-scorecard :: [[Int]]`

the scorecard is represented by a 2d vector containing 12 frames `[[-1 -1] [-1 -1] ...]`.

The raw data structure uses 12 frames as it simplifies handling the 2 fill frames should a spare or strike occur on the 10th frame.

Frame type representations:
 
### strike 
`[10 -1]`

### spare
both rolls must be 0 - 10 inclusive and sum to 10

`[7 3]`

### open
both rolls must be 0 - 10 inclusive and sum to less than 10

`[1 3]`

### not used: 
Not rolls yet for this frame

`[-1 -1]`

### special 10th frame case

If a spare is rolled on the 10th frame, 1 more roll is needed for the 11th frame. The vector must have
its second roll fixed at zero.
 
`[5 0]`

## `calculate-scorecard :: [[Int]] -> CalculatedScorecardResult`
transforms the raw scorecard into a calculated result. The game is complete when the total is not -1 as it
implies every frame has sucessfully resolved its computed score.

where `CalculatedScorecardResult` is a map -> `{
    :calculated-scorecard [{:score Int :symbol String :rolls [Int]}] 
    :total Int
}`

Example:
```
(calculate-scorecard [[10 -1] [10 -1] [10 -1] [10 -1] [10 -1] [10 -1] [10 -1] [10 -1] [10 -1] [10 -1] [10 -1] [10 -1]])

=>
{
 :calculated-scorecard [
    {:score 30, :symbol "X", :rolls [10 10 10]} 
    {:score 30, :symbol "X", :rolls [10 10 10]} 
    {:score 30, :symbol "X", :rolls [10 10 10]} 
    {:score 30, :symbol "X", :rolls [10 10 10]} 
    {:score 30, :symbol "X", :rolls [10 10 10]} 
    {:score 30, :symbol "X", :rolls [10 10 10]} 
    {:score 30, :symbol "X", :rolls [10 10 10]} 
    {:score 30, :symbol "X", :rolls [10 10 10]} 
    {:score 30, :symbol "X", :rolls [10 10 10]} 
    {:score 30, :symbol "X X X", :rolls [10 10 10]}
    ], 
:total 300
}

```

## `finished? :: CalculatedScorecardResult -> Bool`
Simply just returns the result of `total != -1`

## `score-frame :: [[Int]] -> Int -> [Int] -> [[Int]]`
Updates a frames score by taking in the scorecard, index position of the frame and the new frame and returns
back the updated scorecard.

For example, to update the scorecards first frame with a strike
```
score-frame [[-1 -1] [-1 -1] [-1 -1] [-1 -1] [-1 -1] [-1 -1] [-1 -1] [-1 -1] [-1 -1] [-1 -1] [-1 -1] [-1 -1]] 0 [10 -1])
```

=>
```
[[10 -1] [-1 -1] [-1 -1] [-1 -1] [-1 -1] [-1 -1] [-1 -1] [-1 -1] [-1 -1] [-1 -1] [-1 -1] [-1 -1]] 
```


# Validation

clojure spec is used to validate at the edges only. This simplifies internal helper functions as they can
guarantee the scoreboard vector is in a valid state. Its assumed the `create-scoreboard` is
used to create the initial board, then the `score-frame` receives most of the validation to ensure the scorecard
remains valid throughout the games lifetime.

