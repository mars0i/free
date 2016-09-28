(ns free.random
  (:require [utils.random :as ran])) ; clj or cljs, depending

(def session-id (ran/make-long-seed)) (println "seed:" session-id)
(def rng$ (atom (ran/make-rng session-id)))

(defn set-new-rng!
  ([]     (reset! rng$ (ran/make-rng)))
  ([seed] (reset! rng$ (ran/make-rng seed))))

(defn next-gaussian
  ([] (ran/next-gaussian @rng$))
  ([mean sd] (ran/next-gaussian @rng$ mean sd)))

(defn next-double
  ([] (ran/next-double @rng$))
  ([mean sd] (ran/next-double @rng$ mean sd)))

;; Incanter versions:
;;  (:require [incanter.stats :as istat]))
;(def pdf-normal istat/pdf-normal)
;(def sample-normal istat/sample-normal)
;(def sample-uniform istat/sample-uniform)
