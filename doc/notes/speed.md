notes on speed
===


## Scalar vs matrix

Using commit 1cc24e8 ...

Using scripts scalclj and matclj to run the scalar and matrix versions
of free, and then running `free.example5` like this:

    (use 'criterium.core)
    (require '[free.example-5 :as e] :reload)
    (bench (def k (nth (e/make-stages) 100000)))

### Results:

#### scalar:

    Evaluation count : 120 in 60 samples of 2 calls.
                 Execution time mean : 863.319080 ms
        Execution time std-deviation : 33.391864 ms
       Execution time lower quantile : 837.246899 ms ( 2.5%)
       Execution time upper quantile : 952.090470 ms (97.5%)
                       Overhead used : 7.897985 ns
        
    Found 12 outliers in 60 samples (20.0000 %)
    	low-severe	 3 (5.0000 %)
    	low-mild	 9 (15.0000 %)
     Variance from outliers : 25.4288 % Variance is moderately inflated by outliers

#### matrix:

    Evaluation count : 60 in 60 samples of 1 calls.
                 Execution time mean : 1.125688 sec
        Execution time std-deviation : 14.460532 ms
       Execution time lower quantile : 1.102335 sec ( 2.5%)
       Execution time upper quantile : 1.152738 sec (97.5%)
                       Overhead used : 7.835903 ns


*So scalar is only about 25% faster than matrix.*

I'm surprised.  I wonder if the overhead of the RNG is a big part of
it.  This could be tested.  

Or garbage collection?  What if I up the RAM?  Although in this
experiment I'm not holding onto the sequence at all (unlike when I feed
the sequence into `plot-level`), so the memory use shouldn't be high..
