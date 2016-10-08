;; This software is copyright 2016 by Marshall Abrams, and is distributed
;; under the Gnu General Public License version 3.0 as specified in the
;; the file LICENSE.

(ns free.example-4
  (:require [free.level :as lvl]
            [free.random :as ran])) ; will be clj or cljs depending on dialect

;; Generative function phi^2:
(defn gen  [phi] (* phi phi)) ;; or: (lvl/m-square phi)
(defn gen' [phi] (* phi 2.0))   ;; or: (ar/m* phi 2))

(def init-gen-wt 1) ; i.e. initially pass value of gen(phi) through unchanged

;; next-bottom function
;; all this atom stuff is "bad", but is really just implementing a loop while allowing the function to be arg-less
(def change-ticks$ (atom (range 20000 1000000000 20000)))
(def means$ (atom (cycle [40 2])))
(def mean$ (atom 2))
(def sd 5)
(def tick$ (atom 0))
(def next-bottom (lvl/make-next-bottom 
                   (fn []
                     (when (= (swap! tick$ inc) (first @change-ticks$))
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
