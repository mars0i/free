(ns free.example-1
  (:use [free.scalar-arithmetic]
        ;[free.matrix-arithmetic]
        )
  (:require [clojure.math.numeric-tower :as nt]
            [free.level :as lv]
            [free.dists :as pd])) ; will be clj or cljs depending on dialect

;; all-level parameters

(def dt 0.01) ; for phi and eps
(def sigma-dt 0)
(def theta-dt 0)

;; We are trying to infer a parameter that's the radius of a circle,
;; e.g. because the object is a circle, and the light from it tends
;; to be proportional to its area.  So phi up at the top counts as
;; this radius, but the data is the light, so that follows the square.
;; Thus when we infer the estimate for this phi, it should be the
;; square root of the light.
(defn h  [phi] (lv/m-square phi))
(defn h' [phi] (m* phi 2))
(def theta (make-identity-obj 1)) ; i.e. pass value of h(phi) through unchanged

;; bottom level params
(def u 2)       ; phi
(def next-bottom (lv/make-next-bottom #(pd/sample-normal 1 :mean 2 :sd 1)))
(def sigma-u 1) ; controls degree of fluctuation in phi at level 1
(def error-u 0) ; eps

;; middle level params
(def init-phi-v-p 3)    ; what phi is initialized to
(def sigma-p 1) ; controls how close to true value at level 1
(def error-p 0)

;; top level param
;(def top-v-p (nt/sqrt 2))
(def top-v-p 2)

(def init-bot
  (lv/map->Level {:phi u
                  :eps error-u
                  :sigma sigma-u
                  :theta theta
                  :h  h   ; used to calc error, i.e. eps
                  :h' nil ; unused at bottom level since phi comes from outside
                  :phi-dt   dt
                  :eps-dt   dt
                  :sigma-dt sigma-dt
                  :theta-dt theta-dt}))

(def init-mid
  (lv/map->Level {:phi init-phi-v-p
                  :eps error-p
                  :sigma sigma-p
                  :theta theta
                  :h  identity ; just passing down phi from top
                  :h' h'
                  :phi-dt   dt
                  :eps-dt   dt
                  :sigma-dt sigma-dt
                  :theta-dt theta-dt}))

(def top (lv/map->Level {:phi top-v-p})) ; other fields will be nil

(def init-levels [init-bot init-mid top])

(def stages (iterate (partial lv/next-levels next-bottom) init-levels))
