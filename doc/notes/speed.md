notes on speed
===


## scalar vs matrix with randomness:

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


## scalar vs. matrix without randomness

Running

    (bench (def k (nth (e/make-stages) 1000)))

from `free.exercise-3` in commit 5c8ae8a.

In the results below, the speed of the scalar version is 80% of the
speed of the matrix version.  This shows that it's not the RNG that's
eating up the predicted speed difference between the matrix and scalar
versions.

I wondered if maybe the inverse in `sigma-inc` was part of the
problem, but that's silly.  In the scalar version, this is simple
division, and I bet that's all it is in the matrix version, too.

### Results:

#### scalar:

    Evaluation count : 9540 in 60 samples of 159 calls.
                 Execution time mean : 6.512737 ms
        Execution time std-deviation : 287.399512 µs
       Execution time lower quantile : 6.251808 ms ( 2.5%)
       Execution time upper quantile : 7.151339 ms (97.5%)
                       Overhead used : 7.991068 ns
    
    Found 5 outliers in 60 samples (8.3333 %)
    	low-severe	 4 (6.6667 %)
    	low-mild	 1 (1.6667 %)
     Variance from outliers : 30.3257 % Variance is moderately inflated by outliers

#### matrix:

    Evaluation count : 7440 in 60 samples of 124 calls.
                 Execution time mean : 8.127744 ms
        Execution time std-deviation : 48.569099 µs
       Execution time lower quantile : 8.054842 ms ( 2.5%)
       Execution time upper quantile : 8.260081 ms (97.5%)
                       Overhead used : 7.944598 ns
    
    Found 4 outliers in 60 samples (6.6667 %)
    	low-severe	 3 (5.0000 %)
    	low-mild	 1 (1.6667 %)
     Variance from outliers : 1.6389 % Variance is slightly inflated by outliers


## transducer vs. traditional lazy sequence

This shows that using a transducer to construct the sequence of stages
doesn't seem to improve speed significantly.

    user=> (require '[free.example-5 :as e])

### Results:

#### traditional:

    user=> (require '[free.example-5 :as e])
    user=> (bench (def _ (last (take-nth 3000 (take 120000 (e/make-stages))))))
    Evaluation count : 60 in 60 samples of 1 calls.
                 Execution time mean : 5.898407 sec
        Execution time std-deviation : 652.647864 ms
       Execution time lower quantile : 5.408534 sec ( 2.5%)
       Execution time upper quantile : 7.535030 sec (97.5%)
                       Overhead used : 31.932235 ns
    
    Found 7 outliers in 60 samples (11.6667 %)
    	low-severe	 4 (6.6667 %)
    	low-mild	 3 (5.0000 %)
     Variance from outliers : 73.8035 % Variance is severely inflated by outliers


#### lazy transducer:

    user=> (require '[free.example-5 :as e])
    user=> (def xf (comp (take 120000) (take-nth 3000)))
    user=> (bench (def _ (last (sequence xf (e/make-stages)))))
    Evaluation count : 60 in 60 samples of 1 calls.
                 Execution time mean : 5.762172 sec
        Execution time std-deviation : 481.426511 ms
       Execution time lower quantile : 5.409790 sec ( 2.5%)
       Execution time upper quantile : 7.034231 sec (97.5%)
                       Overhead used : 31.932235 ns
    
    Found 7 outliers in 60 samples (11.6667 %)
    	low-severe	 3 (5.0000 %)
    	low-mild	 4 (6.6667 %)
     Variance from outliers : 61.8369 % Variance is severely inflated by outliers


#### non-lazy transducer:

    user=> (require '[free.example-5 :as e])
    user=> (def xf (comp (take 120000) (take-nth 3000)))
    user=> (bench (def _ (last (into [] xf (e/make-stages)))))
    Evaluation count : 60 in 60 samples of 1 calls.
                 Execution time mean : 6.038467 sec
        Execution time std-deviation : 626.724656 ms
       Execution time lower quantile : 5.591108 sec ( 2.5%)
       Execution time upper quantile : 7.243018 sec (97.5%)
                       Overhead used : 31.932235 ns
    
    Found 7 outliers in 60 samples (11.6667 %)
    	low-severe	 4 (6.6667 %)
    	low-mild	 3 (5.0000 %)
     Variance from outliers : 72.0332 % Variance is severely inflated by outliers


## Adding to both ends of levels in `next-levels`:

This is how I `next-levels` adds elements to both ends of a sequence
before mid-October 2016 (e.g. commit 2886cfa):

    (defn add-both-ends-1
      [x1 xs xn]
      (doall (concat [x1] xs [xn])))

Let's test its speed:

    user=> (bench (add-both-ends-1 :a [:b :c] :d))
    Evaluation count : 79975440 in 60 samples of 1332924 calls.
                 Execution time mean : 738.873636 ns
        Execution time std-deviation : 6.155384 ns
       Execution time lower quantile : 727.423897 ns ( 2.5%)
       Execution time upper quantile : 753.145185 ns (97.5%)
                       Overhead used : 7.809086 ns
    
    Found 4 outliers in 60 samples (6.6667 %)
    	low-severe	 1 (1.6667 %)
    	low-mild	 3 (5.0000 %)
     Variance from outliers : 1.6389 % Variance is slightly inflated by outliers

Here's an alternative

    (defn add-both-ends-2
      [x1 xs xn]
      (cons x1 
            (conj (vec xs) xn)))

that's more than 3X faster:

    user=> (bench (add-both-ends-2 :a [:b :c] :d))
    Evaluation count : 315449100 in 60 samples of 5257485 calls.
                 Execution time mean : 200.432284 ns
        Execution time std-deviation : 14.926679 ns
       Execution time lower quantile : 182.528894 ns ( 2.5%)
       Execution time upper quantile : 226.777741 ns (97.5%)
                       Overhead used : 7.809086 ns
    
    Found 1 outliers in 60 samples (1.6667 %)
    	low-severe	 1 (1.6667 %)
     Variance from outliers : 55.1589 % Variance is severely inflated by outliers


## next-levels with both ends code, destructuring, etc.

### old version:

#### code

    (defn next-levels
      [next-bottom levels]
      (concat [(next-bottom (take 2 levels))] ; Bottom level is special case.
              (map next-level                 ; Each middle level depends on levels
                   (partition 3 1 levels))    ;  immediately below and above it.
              [(last levels)]))               ; top is carried forward as-is

#### time

    user=> (bench (def _ (last (take-nth 3000 (take 120000 (e/make-stages))))))
    Evaluation count : 60 in 60 samples of 1 calls.
                 Execution time mean : 5.616549 sec
        Execution time std-deviation : 93.551572 ms
       Execution time lower quantile : 5.488649 sec ( 2.5%)
       Execution time upper quantile : 5.797978 sec (97.5%)
                       Overhead used : 31.379479 ns
    
    Found 1 outliers in 60 samples (1.6667 %)
    	low-severe	 1 (1.6667 %)
     Variance from outliers : 6.2495 % Variance is slightly inflated by outliers

Another run:

    user=> (bench (def _ (last (take-nth 3000 (take 120000 (e/make-stages))))))
    Evaluation count : 60 in 60 samples of 1 calls.
                 Execution time mean : 5.715559 sec
        Execution time std-deviation : 201.409108 ms
       Execution time lower quantile : 5.426575 sec ( 2.5%)
       Execution time upper quantile : 6.132154 sec (97.5%)
                       Overhead used : 31.379479 ns

### new way of adding to both ends of seq

#### code

    (defn next-levels*
      [next-bottom levels]
      (cons (next-bottom (take 2 levels))     ; Bottom level is special case.
            (conj
              (vec (map next-level            ; Each middle level depends on levels
                     (partition 3 1 levels))) ;  immediately below and above it.
              (last levels))))                ; Top is carried forward as-is

#### time

    (bench (def _ (last (take-nth 3000 (take 120000 (e/make-stages*))))))
    Evaluation count : 60 in 60 samples of 1 calls.
                 Execution time mean : 5.579666 sec
        Execution time std-deviation : 480.501335 ms
       Execution time lower quantile : 4.919300 sec ( 2.5%)
       Execution time upper quantile : 6.614420 sec (97.5%)
                       Overhead used : 31.379479 ns
    
    Found 6 outliers in 60 samples (10.0000 %)
    	low-severe	 6 (10.0000 %)
     Variance from outliers : 63.5164 % Variance is severely inflated by outliers

This run was slightly better:

    user=> (bench (def _ (last (take-nth 3000 (take 120000 (e/make-stages*))))))
    Evaluation count : 60 in 60 samples of 1 calls.
                 Execution time mean : 5.361443 sec
        Execution time std-deviation : 535.546539 ms
       Execution time lower quantile : 5.009388 sec ( 2.5%)
       Execution time upper quantile : 6.435113 sec (97.5%)
                       Overhead used : 31.379479 ns
    
    Found 8 outliers in 60 samples (13.3333 %)
    	low-severe	 5 (8.3333 %)
    	low-mild	 3 (5.0000 %)
     Variance from outliers : 70.3262 % Variance is severely inflated by outliers


So that's interesting: Raw tests on different ways to add to both ends
of a sequence show a big difference (above), but that doesn't make a significant
difference on next-levels.


### new both-ends plus destructuring for next-bottom:

#### code

    (defn next-levels**
      [next-bottom [level-0 level-1 :as levels]]
      (cons (next-bottom [level-0 level-1])     ; Bottom level is special case.
            (conj
              (vec (map next-level            ; Each middle level depends on levels
                     (partition 3 1 levels))) ;  immediately below and above it.
              (last levels))))                ; Top is carried forward as-is

#### time

    (bench (def _ (last (take-nth 3000 (take 120000 (e/make-stages**))))))
    Evaluation count : 60 in 60 samples of 1 calls.
                 Execution time mean : 5.333680 sec
        Execution time std-deviation : 627.464346 ms
       Execution time lower quantile : 4.909855 sec ( 2.5%)
       Execution time upper quantile : 7.202714 sec (97.5%)
                       Overhead used : 31.379479 ns
    
    Found 8 outliers in 60 samples (13.3333 %)
    	low-severe	 5 (8.3333 %)
    	low-mild	 3 (5.0000 %)
     Variance from outliers : 75.5607 % Variance is severely inflated by outliers


This is just a little big faster than the non-destructuring versions.

Another run:

    user=> (bench (def _ (last (take-nth 3000 (take 120000 (e/make-stages**))))))
    Evaluation count : 60 in 60 samples of 1 calls.
                 Execution time mean : 5.144079 sec
        Execution time std-deviation : 179.876555 ms
       Execution time lower quantile : 4.944930 sec ( 2.5%)
       Execution time upper quantile : 5.570915 sec (97.5%)
                       Overhead used : 31.379479 ns
    
    Found 4 outliers in 60 samples (6.6667 %)
    	low-severe	 4 (6.6667 %)
     Variance from outliers : 22.1738 % Variance is moderately inflated by outliers



### special three-level version:

### code

(defn next-levels-3
  [next-bottom [level-0 level-1 :as levels]]
  [(next-bottom [level-0 level-1]) ; Bottom level is special case.
   (next-level levels)             ; Each middle level depends on levels immediately below and above it.
   (last levels)])                 ; top is carried forward as-is

### time

    user=> (bench (def _ (last (take-nth 3000 (take 120000 (e/make-stages-3))))))
    Evaluation count : 60 in 60 samples of 1 calls.
                 Execution time mean : 3.534503 sec
        Execution time std-deviation : 536.809760 ms
       Execution time lower quantile : 3.306916 sec ( 2.5%)
       Execution time upper quantile : 4.802435 sec (97.5%)
                       Overhead used : 31.379479 ns
    
    Found 10 outliers in 60 samples (16.6667 %)
    	low-severe	 2 (3.3333 %)
    	low-mild	 8 (13.3333 %)
     Variance from outliers : 84.1843 % Variance is severely inflated by outliers


Again, this is significantly faster.  Enough that I should use it often.
