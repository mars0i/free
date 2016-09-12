;; Exercise 3 in Bogacz

(ns free.exercise-3
  (:use [free.scalar-arithmetic])
  (:require [clojure.math.numeric-tower :as nt]
            [free.level :as lvl]
            [free.dists :as pd])) ; will be clj or cljs depending on dialect

;; Bogacz's exercises 1 and 3:
;; p. 3:
;; Exercise 1. Assume that our animal observed the light intensity u =
;; 2, the level of noise in its receptor is Σ_u = 1, and the mean and
;; variance of its prior expectation of size are v_p = 3 and Σ_p = 1. Write
;; a computer program that computes the posterior probabilities of sizes
;; from 0.01 to 5, and plots them.
;; p. 4:
;; Exercise 3. Simulate the model from Fig. 3 for the problem
;; from Exercise 1. In particular, initialize φ = v_p, ε_p = ε_u = 0, and
;; find their values for the next 5 units of time.

;; The data that results is supposed to look like the rhs of fig. 2.
;; i.e. phi from the middle level is phi in that plot;
;; eps from middle is eps_p and eps from bottom is eps_u.
;; Here's one way to do this:
;; 
;; (def s500 (take 500 stages)) ; number of timesteps Bogacz uses: (/ 5 0.01)
;; (use '[incanter.charts])
;; (def xy (xy-plot (range) (map (comp :phi second) s500)))
;; (add-lines xy    (range) (map (comp :eps second) s500))
;; (add-lines xy    (range) (map (comp :eps first)  s500))
;; (use '[incanter.core])
;; (view xy)
;; (use '[incanter.pdf])
;; (save-pdf xy "ex3.pdf")

;;;;;;;;;;;;;;;;;;

(def dt 0.01) ; version in Bogacz

;; all-level parameters
(def theta (make-identity-obj 1)) ; i.e. pass value of h(phi) through unchanged
(defn h  [phi] (lvl/m-square phi))
(defn h' [phi] (m* phi 2))

;; bottom level params
(def u 2)       ; phi
(def error-u 0) ; eps
(def sigma-u 1)
(def next-bottom (lvl/make-next-bottom (constantly u)))

;; middle level params
(def v-p 3)    ; what phi is initialized to
(def error-p 0)
(def sigma-p 1)

(def init-bot
  (lvl/map->Level {:phi u
                  :eps error-u
                  :sigma sigma-u
                  :theta theta   ; preserves h(phi)
                  :h  nil ; unused at bottom since eps update uses higher h
                  :h' nil ; unused at bottom since phi comes from outside
                  :phi-dt dt
                  :eps-dt dt
                  :sigma-dt 0    ; sigma never changes
                  :theta-dt 0})) ; theta never changes

(def init-mid
  (lvl/map->Level {:phi v-p
                  :eps error-p
                  :sigma sigma-p
                  :theta theta       ; preserves h(phi)
                  :h  h
                  :h' h'
                  :phi-dt dt
                  :eps-dt dt
                  :sigma-dt 0    ; sigma never changes
                  :theta-dt 0})) ; theta never changes

(def top (lvl/make-top-level v-p))

(def init-levels [init-bot init-mid top])

(def stages (iterate (partial lvl/next-levels next-bottom) init-levels))
