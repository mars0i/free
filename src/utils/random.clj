;;; This software is copyright 2013, 2014, 2015, 2016 by Marshall Abrams, and
;;; is distributed under the Gnu General Public License version 3.0 as
;;; specified in the file LICENSE.

(ns utils.random
  (:import [ec.util MersenneTwisterFast]))

(defn make-long-seed
  [] 
  (- (System/currentTimeMillis)
     (rand-int Integer/MAX_VALUE)))

(defn make-rng-mtf
  "Make an instance of a MersenneTwisterFast RNG and flush out its initial
  minimal lack of entropy."
  ([] (make-rng-mtf (make-long-seed)))
  ([long-seed] 
   (let [rng (MersenneTwisterFast. long-seed)]
     (dotimes [_ 1500] (.nextInt rng))  ; see ;; https://listserv.gmu.edu/cgi-bin/wa?A1=ind1609&L=MASON-INTEREST-L#1
     rng))) 

(def make-rng make-rng-mtf)

(defn make-rng-print-seed
  "Make a seed, print it to stdout, then pass it to make-rng."
  []
  (let [seed (make-long-seed)]
    (println seed)
    (make-rng seed)))

(defn rand-idx [rng n] (.nextInt rng n))

(defn next-long [rng] (.nextLong rng))

(defn next-double
"Returns a random double in the half-open range from [0.0,1.0)."
  [rng]
  (.nextDouble rng))

(defn next-gaussian
  "Returns a random double from a Gaussian distribution.  If mean and sd aren't
  supplied, uses a standard Gaussian distribution with mean = 0 and sd = 1,
  otherwise uses the supplied mean and standard deviation.  If left and right
  are also provided, samples from a truncated normal distribution, truncated
  so that values will in the interval [left, right].  (Truncation might not be 
  particular efficient; it simply throws out values outside the interval, and
  tries again.)"
  ([rng] (.nextGaussian rng))
  ([rng mean sd]
   (+ mean (* sd (next-gaussian rng)))))

(defn truncate
  "Given an a function and arguments that generate random samples, returns a 
  random number generated with that function, but constrained to to lie within 
  [left,right].  Might not be particularly efficient--better for initialization
  than runtime?  Example: (truncate -1 1 next-gaussian rng 0 0.5)"
  [left right rand-fn & addl-args]
  (loop [candidate (apply rand-fn addl-args)]
    (if (and (>= candidate left) (<= candidate right))
      candidate
      (recur (apply rand-fn addl-args)))))

;; lazy
;; This version repeatedly calls nth coll with a new random index each time.
;(defn sample-with-repl-1
;  [rng num-samples coll]
;  (let [size (count coll)]
;    (repeatedly num-samples 
;                #(nth coll (rand-idx rng size)))))

;; lazy
;; This version is inspired by Incanter, which does it like this:
;;        (map #(nth x %) (sample-uniform size :min 0 :max max-idx :integers true))
;; You get a series of random ints between 0 and the coll size,
;; and then map nth coll through them.
;(defn sample-with-repl-2
;  [rng num-samples coll]
;  (let [size (count coll)]
;    (map #(nth coll %) 
;         (repeatedly num-samples #(rand-idx rng size)))))

;; lazy
;(def sample-with-repl sample-with-repl-3) ; see samplingtests2.xlsx
(defn sample-with-repl
  "Return num-samples from coll, sampled with replacement."
  [rng num-samples coll]
  (let [size (count coll)]
    (for [_ (range num-samples)]
      (nth coll (rand-idx rng size)))))

;; not lazy
;(defn sample-with-repl-4
;  [rng num-samples coll]
;  (let [size (count coll)]
;    (loop [remaining num-samples result []] 
;      (if (> remaining 0)
;        (recur (dec remaining) (conj result 
;                                     (nth coll (rand-idx rng size))))
;        result))))



;; lazy if more than one sample
;; (deal with license issues)
(defn sample-without-repl
  "Derived from Incanter's algorithm from sample-uniform for sampling without replacement."
  [rng num-samples coll]
  (let [size (count coll)
        max-idx size]
    (cond
      (= num-samples 1) (list (nth coll (rand-idx rng size)))  ; if only one element needed, don't bother with the "with replacement" algorithm
      ;; Rather than creating subseqs of the original coll, we create a seq of indices below,
      ;; and then [in effect] map (partial nth coll) through the indices to get the samples that correspond to them.
      (< num-samples size) (map #(nth coll %) 
                                (loop [samp-indices [] indices-set #{}]    ; loop to create the set of indices
                                  (if (= (count samp-indices) num-samples) ; until we've collected the right number of indices
                                    samp-indices
                                    (let [i (rand-idx rng size)]             ; get a random index
                                      (if (contains? indices-set i)      ; if we've already seen that index,
                                        (recur samp-indices indices-set) ;  then try again
                                        (recur (conj samp-indices i) (conj indices-set i))))))) ; otherwise add it to our indices
      :else (throw (Exception. "num-samples can't be larger than (count coll).")))))
