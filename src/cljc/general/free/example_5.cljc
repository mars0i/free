;; This software is copyright 2016 by Marshall Abrams, and is distributed
;; under the Gnu General Public License version 3.0 as specified in the
;; the file LICENSE.

;; This example shows that
;; (a) Sigma responds to changing error rates due to changing
;;     causes (i.e. change in the mean of generated inputs), by
;;     going up and down in response,
;; (b) Eventually settling in a region with a stable cycle
;; (c) When change stops, sigma goes to zero (or as low as we allow).
;; Here are command that will show this:
;;    (use '[free.plots] :reload)
;;    (require '[free.example-5 :as e] :reload)
;;    (plot-level (e/make-stages) 1 300000) ; 300K ticks
;;    (plot-level (e/make-stages) 1 3500000 100) 3.5M ticks, sampled every 100
;; To see what's going on at the initial sensory level, replace 1 with 0.

(ns free.example-5
  (:require [free.level :as lvl]
            [free.random :as ran])) ; will be clj or cljs depending on dialect

;; Generative function phi^2:
(defn gen  [phi] (* phi phi)) ;; or: (lvl/m-square phi)
(defn gen' [phi] (* phi 2.0))   ;; or: (ar/m* phi 2))

(def init-gen-wt 1) ; i.e. initially pass value of gen(phi) through unchanged

;; next-bottom function
;; all this atom stuff is "bad", but is really just implementing a loop while allowing the function to be arg-less
(def change-every 20000)  ; change in inputs every this many ticks
(def stop-changing-after 3000000)
(def change-ticks$ (atom (range change-every stop-changing-after change-every)))
(def means$ (atom (cycle [40 2]))) ; cycle between these means
(def mean$ (atom 2)) ; initial value of mean
(def sd 5)           ; constant stddev
(def tick$ (atom 0)) ; timestep
(def next-bottom (lvl/make-next-bottom 
                   (fn []
                     (when (and (first @change-ticks$) ; stop when sequence exhausted
		                (= (swap! tick$ inc) (first @change-ticks$)))
                       (reset! mean$ (first @means$))
                       (swap! change-ticks$ rest)
                       (swap! means$ rest))
                     (ran/next-gaussian @mean$ sd)))) ; SHOULD THIS BE FED INTO gen ?

(def sigma-u 2) ; controls degree of fluctuation in phi at level 1
(def error-u 0) ; err
;; Note that the bottom-level phi needs to be an arbitrary number so that 
;; err-inc doesn't NPE on the first tick, but the number doesn't matter, and 
;; it will immediately replaced when next-bottom is run.

;; middle level params
(def v-p 3) ; what phi is initialized to, and prior mean at top
(def sigma-p 2) ; controls how close to true value at level 1
(def error-p 0)

(def bot-map {:phi 0 ; needs a number for err-inc on 1st tick; immediately replaced by next-bottom
              :err error-u
              :sigma sigma-u
              :gen-wt init-gen-wt
              :gen  nil ; unused at bottom since err update uses higher gen
              :gen' nil ; unused at bottom since phi comes from outside
              :phi-dt 0.01
              :err-dt 0.01
              :sigma-dt 0.0
              :gen-wt-dt 0.0})

(def mid-map {:phi v-p
              :err error-p
              :sigma sigma-p
              :gen-wt init-gen-wt
              :gen  gen  ; used to calc error at next level down, i.e. err
              :gen' gen' ; used to update phi at this level
              :phi-dt 0.0001
              :err-dt 0.01
              :sigma-dt 0.0001
              :gen-wt-dt 0.00005})

(def init-bot (lvl/map->Level bot-map))
(def init-mid (lvl/map->Level mid-map))
(def init-mid-fixed-gen-wt (lvl/map->Level (assoc mid-map :gen-wt-dt 0.0)))
(def top (lvl/make-top-level v-p)) ; will have phi, and identity as :gen ; other fields nil

(defn make-stages [] (iterate (partial lvl/next-levels next-bottom)
                              [init-bot init-mid top]))
