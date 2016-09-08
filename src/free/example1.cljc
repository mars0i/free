(ns free.example1
  (:use ;[free.matrix-arithmetic]
        [free.scalar-arithmetic]
        [free.level])
  (:require [free.dists :as pd])) ; will be clj or cljs depending on dialect

;;;;;;;;;;;;;;;;;;;;;
;; These define the function g that Bogacz offers as an example on p. 2.
;; i.e. for g(phi) = theta * h(phi), where g just squares its argument.

(def example-theta (make-identity-obj dims))

;; these really shouldn't be the same at every level--doesn't make sense
(defn example-h  [phi] (m-square phi))
(defn example-h' [phi] (m* phi 2))

;; To see that it's necessary to calculate the error in the usual way
;; at the bottom level, cf. e.g. eq (14) in Bogacz.
(defn next-bottom
  [[level-0 level-1]]
  (->Level (pd/sample-normal 1 :mean 4 :sd 2) ; phi: inputs from world
           (next-eps   [nil level-0 level-1])
           (next-sigma [nil level-0 nil])  ; (?) we want sigma to be updated from inputs ...
           (next-theta [nil level-0 level-1])
           example-h
           example-h'))

(def prior-mean-fn (constantly 2.0))
(def prior-sigma-fn (constantly 1.0))

;; quasi-level above top level. all it does is provide params of initial priors
(def super-top (->Level nil nil nil nil prior-mean-fn nil))

;; MAYBE THIS IS WRONG.  MAYBE JUST USE REGULAR next-level BUT MAKE
;; THE TOP LEVEL BE WHAT HERE IS super-top.
(defn next-top
  [[level-n-1 level-n]]
  (->Level (next-phi [level-n-1 level-n nil])
           (next-eps [nil level-n super-top])
           ;; shouldn't this be constant?:
           (next-sigma [nil level-n nil]) ; (??) given first time, then adjusts
           (next-theta [nil level-n super-top])
           example-h
           example-h'))


;;;;;;;;;;;;;;;;;;
;; from ex. 3 in Bogacz
;(def v-p 3)
;(def sigma-p 1)
;(def sigma-u 1)
;(def u 2)
;(def dt 0.01)
;(def phi v-p)
;(def error-p 0)
;(def error-u 0)
