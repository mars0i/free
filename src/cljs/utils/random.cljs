(ns utils.random
  (:require [cljsjs.chance :as ran])) ; http://chancejs.com

(defn make-long-seed
  []
  (- (.getTime (js/Date.))
     (rand-int js/Number.MAX_SAFE_INTEGER)))

(defn flush-rng
  "Flush out initial order from a Mersenne Twister."
  [rng]
  (dotimes [_ 1500] (.integer rng)))  ; see ;; https://listserv.gmu.edu/cgi-bin/wa?A1=ind1609&L=MASON-INTEREST-L#1

(defn make-rng
  "Make an instance of a chance.js MersenneTwister RNG and flush out its initial
  minimal lack of entropy."
  ([] (make-rng (make-long-seed)))
  ([long-seed] 
   (let [rng (js/Chance. long-seed)]
     (flush-rng rng)
     rng))) 

(defn rand-idx
  [rng n]
  (.integer rng
            (clj->js {:min 0 :max (dec n)}))) ; min and max are allowable values

(defn next-long
  [rng]
  (.integer rng))

(defn next-double
"Returns a random double in the half-open range from [0.0,1.0)."
  [rng]
  (.floating rng))

(defn next-gaussian
  "Returns a random double from a Gaussian distribution.  If mean and sd aren't
  supplied, uses a standard Gaussian distribution with mean = 0 and sd = 1,
  otherwise uses the supplied mean and standard deviation."
  ([rng] (.normal rng))
  ([rng mean sd]
   (.normal rng 
            (clj->js {:mean mean :dev sd}))))
