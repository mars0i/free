;; This software is copyright 2016 by Marshall Abrams, and is distributed
;; under the Gnu General Public License version 3.0 as specified in the
;; the file LICENSE.

(ns free.model
  (:require [cljs.spec :as s]    ; only for clojure spec tests a bottom
            [reagent.core :as r]
            [free.level :as lvl]
            [free.random :as ran])) ; will be clj or cljs depending on dialect
;; This file contains whatever is current default model for the Clojurescript
;; version of free.  The material below may be repeatedly edited or replaced.

;; Generative function phi^2:
(defn gen  [phi] (* phi phi)) ;; or: (lvl/m-square phi)
(defn gen' [phi] (* phi 2.0))   ;; or: (ar/m* phi 2))

(def init-theta 1) ; i.e. initially pass value of gen(phi) through unchanged

(defn stepped-range  ; from http://stackoverflow.com/a/32042392/1455243
  "Like range, but returning every step'th number forever."
  [start step]
  (iterate #(+ % step) start))

(defn swap-rest!
  "Replace the sequence value of key k in the map to which map-atom$ refers
  with the rest of the sequence, then return the previous first element.
  (The last step differs from the behavior of swap!.)"
  [seq$]
  (let [prev-first (first @seq$)]
    (swap! seq$ rest)
    prev-first))

;USE THIS:
(take 30 (reductions + (cycle [1 10 100 1000])))

;; all this atom stuff is "bad", below but is really just implementing a loop while allowing the function to be arg-less
;; then again, now I'm having to set it here from plot-pages.
;; shouldn't it be passed into a function that creates a new model?
;; and define next-bottom in that function?

;; This will be used by free.plot-pages.  It should have one element for each level--nil if no params needed for that level.
(defonce other-model-params [(atom {:description "sensory:" ; params for sensory input data generation function
                                    :change-ticks [1500 100]
                                    :means [2 20]
			            :stddevs [3]})
                             nil
                             nil])

(defn make-next-bottom
  "Local make-next-bottom that uses other-model-params.  Wrapping 
  level/make-next-bottom in a function here allows us to easily
  remake next-bottom when parameters change."
  [other-model-params]
  (lvl/make-next-bottom 
    (let [tick$ (atom 0)
          model-params$ (first other-model-params) ; we only use level 0 params, to generate sensory data
          curr-mean$ (atom (first (:means @model-params$))) ; need to store current value across ticks, but allow it to be altered
          curr-sd$ (atom (first (:stddevs @model-params$))) ; ditto
          means-cycle$ (atom (rest (cycle (:means @model-params$)))) ; skip first value, since it's now in curr-mean$
          sds-cycle$ (atom (rest (cycle (:stddevs @model-params$)))) ; skip first value, since it's now in curr-sd$
          change-ticks-cycle$ (atom (reductions + (cycle (:change-ticks @model-params$))))] ; sequence of ticks separated by cycling values in change-ticks
      (fn []
        (when (= (swap! tick$ inc) (first @change-ticks-cycle$))
          (swap! change-ticks-cycle$ rest)
          (reset! curr-mean$ (swap-rest! means-cycle$))
          (reset! curr-sd$ (swap-rest! sds-cycle$)))
        (ran/next-gaussian @curr-mean$ @curr-sd$)))))

(def sigma-u 2) ; controls degree of fluctuation in phi at level 1
(def error-u 0) ; epsilon
;; Note that the bottom-level phi needs to be an arbitrary number so that 
;; epsilon-inc doesn't NPE on the first tick, but the number doesn't matter, and 
;; it will immediately replaced when next-bottom is run.

;; middle level params
(def v-p 3) ; what phi is initialized to, and prior mean at top
(def sigma-p 2) ; controls how close to true value at level 1
(def error-p 0)

(def bot-map {:phi nil ; APPARENTLY NOT: needs a number for epsilon-inc on 1st tick; immediately replaced by next-bottom
              :epsilon error-u
              :sigma sigma-u
              :theta init-theta
              :gen  nil ; unused at bottom since epsilon update uses higher gen
              :gen' nil ; unused at bottom since phi comes from outside
              :phi-dt nil
              :epsilon-dt 0.01
              :sigma-dt nil
              :theta-dt nil})

(def mid-map {:phi v-p
              :epsilon error-p
              :sigma sigma-p
              :theta init-theta
              :gen  gen  ; used to calc error at next level down, i.e. epsilon
              :gen' gen' ; used to update phi at this level
              :phi-dt 0.001
              :epsilon-dt 0.01
              :sigma-dt 0.01
              :theta-dt 0.00001})

(def init-bot (lvl/map->Level bot-map))
(def init-mid (lvl/map->Level mid-map))
(def init-mid-fixed-theta (lvl/map->Level (assoc mid-map :theta-dt 0.0)))
(def top (lvl/make-top-level v-p)) ; will have phi, and identity as :gen ; other fields nil

(def first-stage [init-bot init-mid top])

(defn make-stages
  [stage next-bottom]
  (iterate (partial lvl/next-levels (make-next-bottom other-model-params)) 
           stage))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; OPTIONAL CODE
;; Clojure spec tests: can be used in free.plot-pages for html form validation.

(s/def ::pos-num (s/and number? pos?))

(s/def ::stddevs (s/+ ::pos-num))
(s/def ::change-ticks (s/+ pos-int?))
(s/def ::means (s/+ number?))
                                       
(s/def ::other-params
  (s/keys :req-un [::stddevs ::change-ticks ::means]))
