;; This software is copyright 2016 by Marshall Abrams, and is distributed
;; under the Gnu General Public License version 3.0 as specified in the
;; the file LICENSE.

;; simple scalar example

(ns free.example-1
  (:require [free.level :as lvl]
            [free.random :as ran])) ; will be clj or cljs depending on dialect

#?(:clj (require '[clojure.math.numeric-tower :as nt]))

;; Generative function phi^2:
(defn gen  [phi] (* phi phi)) ;; or: (lvl/m-square phi)
(defn gen' [phi] (* phi 2.0))   ;; or: (ar/m* phi 2))

;; Alternative generative function phi^8:
;(defn gen  [phi] (nt/expt phi 8))
;(defn gen' [phi] (* 8.0 (nt/expt phi 7.0))) 

(def init-theta 1) ; i.e. initially pass value of gen(phi) througgen unchanged

;; simple next-bottom function
(def next-bottom (lvl/make-next-bottom #(ran/next-gaussian 9 21)))

;; experimental next-bottom function
;(def ticks-between 2000)
;(def top-tick 20000)
;(def tick$ (atom 0))
;; Note that since the generative function is exponential, it's
;; potentially problematic to make the mean negative.
;; TODO I really ought to get rid of this atom stuff and do it instead
;; with args passed along.  Maybe.
;(def next-bottom (lvl/make-next-bottom 
;                   (let [mean$ (atom 52)
;                         sd$ (atom 5)]
;                     (fn []
;                       (swap! tick$ inc)
;                       (when (== @tick$ top-tick)
;                         (println (swap! mean$ #(- % 40))))
;                       (ran/next-gaussian @mean$ @sd$)))))

(def sigma-u 2) ; controls degree of fluctuation in phi at level 1
(def error-u 0) ; epsilon
;; Note that the bottom-level phi needs to be an arbitrary number so that 
;; epsilon-inc doesn't NPE on the first tick, but the number doesn't matter, and 
;; it will immediately replaced when next-bottom is run.

;; middle level params
(def v-p 5) ; what phi is initialized to, and prior mean at top
(def sigma-p 2) ; controls how close to true value at level 1
(def error-p 0)

(def init-bot
  (lvl/map->Level {:phi 0 ; needs a number for epsilon-inc on 1st tick; immediately replaced by next-bottom
                  :epsilon error-u
                  :sigma sigma-u
                  :theta init-theta
                  :gen  nil ; unused at bottom since epsilon update uses higher gen
                  :gen' nil ; unused at bottom since phi comes from outside
                  :phi-dt 0.01
                  :epsilon-dt 0.001
                  :sigma-dt 0.0
                  :theta-dt 0.0}))

(def init-mid
  (lvl/map->Level {:phi v-p
                  :epsilon error-p
                  :sigma sigma-p
                  :theta init-theta
                  :gen  gen  ; used to calc error at next level down, i.e. epsilon
                  :gen' gen' ; used to update phi at this level
                  :phi-dt 0.00001
                  :epsilon-dt 0.001
                  :sigma-dt 0.001
                  :theta-dt 0.000001}))

(def top (lvl/make-top-level v-p)) ; will have phi, and identity as :gen ; other fields will be nil

(def init-levels [init-bot init-mid top])

;(def stages (iterate (partial lvl/next-levels next-bottom) init-levels))
(defn make-stages []
  (iterate (partial lvl/next-levels next-bottom) init-levels))

;; e.g.
;;(plot-level (make-stages)) 1 1000000 1000) ; uses regular sequence ops
;; witgen transducer:
;;(plot-level (sequence (comp (take 1000000) (take-ntgen 1000)) (make-stages)) 1)
;;(plot-level (into []  (comp (take 1000000) (take-ntgen 1000)) (make-stages)) 1)
;; or even more efficient:
;; (plot-level (sequence (comp (take 1000000000)
;;                             (take-nth 10000)
;;                             (map #(nth % 1)))
;;                       (make-stages)))

