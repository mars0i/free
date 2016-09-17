(ns free.dists
  (require [utils.random :as ran]))

(def session-id (ran/make-long-seed)) (println "Session id/seed:" session-id)
(def rng (ran/make-rng session-id))

(defn next-gaussian
  ([] (ran/next-gaussian rng))
  ([mean sd] (ran/next-gaussian rng mean sd)))

(defn next-double
  ([] (ran/next-double rng))
  ([mean sd] (ran/next-double rng mean sd)))

;; Incanter versions:
;;  (:require [incanter.stats :as istat]))
;(def pdf-normal istat/pdf-normal)
;(def sample-normal istat/sample-normal)
;(def sample-uniform istat/sample-uniform)
