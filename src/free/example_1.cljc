(ns free.example-1
  (:require [clojure.math.numeric-tower :as nt]
            [free.scalar-arithmetic :as ar]
            ;[free.matrix-arithmetic :as ar]
            [free.level :as lvl]
            [free.dists :as pd])) ; will be clj or cljs depending on dialect

;; all-level parameters

(def dt 0.001) ; for phi and eps
(def sigma-dt 0.0001)
(def theta-dt 0.001)

;; This example uses a function h(phi)=phi^2 as its generative function.
;;
;; We are trying to infer a parameter that's the radius of a circle,
;; e.g. because the object is a circle, and the light from it tends
;; to be proportional to its area.  So phi up at the top counts as
;; this radius, but the data is the light, so that follows the square.
;; Thus when we infer the estimate for this phi, it should be the
;; square root of the light.
(defn h  [phi] (lvl/m-square phi))
(defn h' [phi] (ar/m* phi 2))
(def theta (ar/make-identity-obj 1)) ; i.e. pass value of h(phi) through unchanged

;; bottom level params
(def next-bottom (lvl/make-next-bottom #(pd/sample-normal 1 :mean 2 :sd 1)))
(def sigma-u 1) ; controls degree of fluctuation in phi at level 1
(def error-u 0) ; eps
;; Note that the bottom-level phi needs to be an arbitrary number so that 
;; eps-inc doesn't NPE on the first tick, but the number doesn't matter, and 
;; it will immediately replaced when next-bottom is run.

;; middle level params
(def v-p 3) ; what phi is initialized to, and prior mean at top
(def sigma-p 1) ; controls how close to true value at level 1
(def error-p 0)

(def init-bot
  (lvl/map->Level {:phi 0 ; needs a number for eps-inc on 1st tick; immediately replaced by next-bottom
                  :eps error-u
                  :sigma sigma-u
                  :theta theta
                  :h  nil ; unused at bottom since eps update uses higher h
                  :h' nil ; unused at bottom since phi comes from outside
                  :phi-dt dt
                  :eps-dt dt
                  :sigma-dt sigma-dt
                  :theta-dt theta-dt}))

(def init-mid
  (lvl/map->Level {:phi v-p
                  :eps error-p
                  :sigma sigma-p
                  :theta theta
                  :h  h  ; used to calc error at next level down, i.e. eps
                  :h' h' ; used to update phi at this level
                  :phi-dt dt
                  :eps-dt dt
                  :sigma-dt sigma-dt
                  :theta-dt theta-dt}))

(def top (lvl/make-top-level v-p)) ; will have phi, and identity as :h ; other fields will be nil

(def init-levels [init-bot init-mid top])

(def stages (iterate (partial lvl/next-levels next-bottom) init-levels))
