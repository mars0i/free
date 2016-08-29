ScalarMatrixPolymorphism.md
===
Notes on how to write efficient code that works either with scalars or
vectors and matrices.

Trying to get efficient scalars using protocols seems to be a failure.
Using m* from free.quant (at git commit 7f01fd8):

    (require '[clojure.core.matrix :as m])
    (use '[free.quant])
    (require '[criterium.core :as c])

`(c/bench (* 2.0 3.0))`:

    Evaluation count : 1349966340 in 60 samples of 22499439 calls.
                 Execution time mean : 9.563432 ns
        Execution time std-deviation : 5.344949 ns
       Execution time lower quantile : 5.279796 ns ( 2.5%)
       Execution time upper quantile : 25.856155 ns (97.5%)
                       Overhead used : 31.876956 ns
    
    Found 5 outliers in 60 samples (8.3333 %)
    	low-severe	 3 (5.0000 %)
    	low-mild	 2 (3.3333 %)
     Variance from outliers : 98.2527 % Variance is severely inflated by outliers

`(c/bench (m* 2.0 3.0))`:

    Evaluation count : 476824200 in 60 samples of 7947070 calls.
                 Execution time mean : 109.658589 ns
        Execution time std-deviation : 19.601843 ns
       Execution time lower quantile : 93.907926 ns ( 2.5%)
       Execution time upper quantile : 164.150536 ns (97.5%)
                       Overhead used : 31.876956 ns
    
    Found 4 outliers in 60 samples (6.6667 %)
    	low-severe	 3 (5.0000 %)
    	low-mild	 1 (1.6667 %)
     Variance from outliers : 89.3398 % Variance is severely inflated by outliers

`(c/bench (m/mmul 2.0 3.0))`:

    Evaluation count : 360636840 in 60 samples of 6010614 calls.
                 Execution time mean : 130.653577 ns
        Execution time std-deviation : 4.360712 ns
       Execution time lower quantile : 125.431564 ns ( 2.5%)
       Execution time upper quantile : 136.819927 ns (97.5%)
                       Overhead used : 31.876956 ns
    
    Found 1 outliers in 60 samples (1.6667 %)
    	low-severe	 1 (1.6667 %)
     Variance from outliers : 20.5692 % Variance is moderately inflated by outliers
