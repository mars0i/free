;; This software is copyright 2016 by Marshall Abrams, and is distributed
;; under the Gnu General Public License version 3.0 as specified in the
;; the file LICENSE.

;; Exercise 3 in Bogacz

(ns free.exercise-3
  (:require [free.level :as lvl]))

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
;; epsilon from middle is epsilon_p and epsilon from bottom is epsilon_u.
;; Here's one way to do this:
;; 
;; (def s500 (take 500 stages)) ; number of timesteps Bogacz uses: (/ 5 0.01)
;; (use '[incanter.charts])
;; (def xy (xy-plot (range) (map (comp :phi second) s500)))
;; (add-lines xy    (range) (map (comp :epsilon second) s500))
;; (add-lines xy    (range) (map (comp :epsilon first)  s500))
;; (use '[incanter.core])
;; (view xy)
;; (use '[incanter.pdf])
;; (save-pdf xy "ex3.pdf")

;;;;;;;;;;;;;;;;;;

(def dt 0.01) ; version in Bogacz

;; all-level parameters
(def theta 1) ; i.e. pass value of gen(phi) through unchanged
(defn gen  [phi] (* phi phi))
(defn gen' [phi] (* phi 2))

;; bottom level params
(def u 2)       ; phi
(def error-u 0) ; epsilon
(def sigma-u 1)
(def next-bottom (lvl/make-next-bottom (constantly u)))

;; middle level params
(def v-p 3)    ; what phi is initialized to
(def error-p 0)
(def sigma-p 1)

(def init-bot
  (lvl/map->Level {:phi u
                  :epsilon error-u
                  :sigma sigma-u
                  :theta theta   ; preserves gen(phi)
                  :gen  nil ; unused at bottom since epsilon update uses higher gen
                  :gen' nil ; unused at bottom since phi comes from outside
                  :phi-dt dt
                  :epsilon-dt dt
                  :sigma-dt 0    ; sigma never changes
                  :theta-dt 0})) ; theta never changes

(def init-mid
  (lvl/map->Level {:phi v-p
                  :epsilon error-p
                  :sigma sigma-p
                  :theta theta       ; preserves gen(phi)
                  :gen  gen
                  :gen' gen'
                  :phi-dt dt
                  :epsilon-dt dt
                  :sigma-dt 0    ; sigma never changes
                  :theta-dt 0})) ; theta never changes

(def top (lvl/make-top-level v-p))

(def init-levels [init-bot init-mid top])

(defn make-stages []
  (iterate (partial lvl/next-levels next-bottom) init-levels))
