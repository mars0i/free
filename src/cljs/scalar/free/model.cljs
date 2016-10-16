;; This software is copyright 2016 by Marshall Abrams, and is distributed
;; under the Gnu General Public License version 3.0 as specified in the
;; the file LICENSE.

(ns free.model
  (:require [free.level :as lvl]
            [free.random :as ran])) ; will be clj or cljs depending on dialect
;; This file contains whatever is current default model for the Clojurescript
;; version of free.  The material below may be repeatedly edited or replaced.


;; from free.example-6:

;; Generative function phi^2:
(defn gen  [phi] (* phi phi)) ;; or: (lvl/m-square phi)
(defn gen' [phi] (* phi 2.0))   ;; or: (ar/m* phi 2))

(def init-theta 1) ; i.e. initially pass value of gen(phi) through unchanged

;; next-bottom function
;; all this atom stuff is "bad", but is really just implementing a loop while allowing the function to be arg-less
(def change-every 20000)  ; change in inputs every this many ticks
(def stop-changing-after 3000000000)
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
(def error-u 0) ; epsilon
;; Note that the bottom-level phi needs to be an arbitrary number so that 
;; epsilon-inc doesn't NPE on the first tick, but the number doesn't matter, and 
;; it will immediately replaced when next-bottom is run.

;; middle level params
(def v-p 3) ; what phi is initialized to, and prior mean at top
(def sigma-p 2) ; controls how close to true value at level 1
(def error-p 0)

(def bot-map {:phi 0 ; needs a number for epsilon-inc on 1st tick; immediately replaced by next-bottom
              :epsilon error-u
              :sigma sigma-u
              :theta init-theta
              :gen  nil ; unused at bottom since epsilon update uses higher gen
              :gen' nil ; unused at bottom since phi comes from outside
              :phi-dt 0.01
              :epsilon-dt 0.01
              :sigma-dt 0.0
              :theta-dt 0.0})

(def mid-map {:phi v-p
              :epsilon error-p
              :sigma sigma-p
              :theta init-theta
              :gen  gen  ; used to calc error at next level down, i.e. epsilon
              :gen' gen' ; used to update phi at this level
              :phi-dt 0.0001
              :epsilon-dt 0.01
              :sigma-dt 0.0001
              :theta-dt 0.0000001})

(def init-bot (lvl/map->Level bot-map))
(def init-mid (lvl/map->Level mid-map))
(def init-mid-fixed-theta (lvl/map->Level (assoc mid-map :theta-dt 0.0)))
(def top (lvl/make-top-level v-p)) ; will have phi, and identity as :gen ; other fields nil

(defn make-stages [] (iterate (partial lvl/next-levels next-bottom)
                              [init-bot init-mid top]))
