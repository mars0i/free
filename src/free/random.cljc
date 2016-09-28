(ns free.random
  (require [utils.random :as r])) ; clj or cljs, depending

(def session-id (r/make-long-seed)) (println "seed:" session-id)
(def rng$ (atom (r/make-rng session-id)))

(defn set-new-rng!
  ([]     (reset! rng$ (r/make-rng)))
  ([seed] (reset! rng$ (r/make-rng seed))))

(defn next-gaussian
  ([] (r/next-gaussian @rng$))
  ([mean sd] (ran/next-gaussian @rng$ mean sd)))

(defn next-double
  ([] (r/next-double @rng$))
  ([mean sd] (r/next-double @rng$ mean sd)))

;; Incanter versions:
;;  (:require [incanter.stats :as istat]))
;(def pdf-normal istat/pdf-normal)
;(def sample-normal istat/sample-normal)
;(def sample-uniform istat/sample-uniform)
