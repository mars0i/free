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

(defn stepped-range  ; from http://stackoverflow.com/a/32042392/1455243
  "Like range, but returning every step'th number forever."
  [start step]
  (iterate #(+ % step) start))

(defn swap-rest!
  "Replace the sequence value of key k in the map to which map-atom$ refers
  with the rest of the sequence.  Unlike swap!, returns the previous first 
  element."
  [map-atom$ k]
  (let [prev-first (first (:k @map-atom$))]
    (swap! map-atom$ 
           assoc k (rest (k @map-atom$)))
    prev-first))

;; next-bottom function
;; all this atom stuff is "bad", but is really just implementing a loop while allowing the function to be arg-less
(def init-means (cycle [20 2])) ; need to be able to reset means to this later
(def other-model-params$ (atom {:change-ticks (interleave (stepped-range 3000 3000) (stepped-range 3100 3000))
                                :init-means init-means
                                :means init-means
                                :sd 5}))

(def curr-mean$ (atom 2)) ; initial value of mean
(def tick$ (atom 0)) ; timestep
(def next-bottom (lvl/make-next-bottom 
                   (fn []
                     (when (= (swap! tick$ inc) (first (:change-ticks @other-model-params$)))
                       (swap-rest! other-model-params$ :change-ticks)
                       (reset! curr-mean$ (swap-rest! other-model-params$ :means)))
                     (ran/next-gaussian @curr-mean$ (:sd @other-model-params$)))))

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
              :phi-dt 0.01
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

(defn make-stages [stage] (iterate (partial lvl/next-levels next-bottom) stage))
