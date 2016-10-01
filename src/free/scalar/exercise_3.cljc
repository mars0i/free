;; Exercise 3 in Bogacz

(ns free.scalar.exercise-3
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
;; err from middle is err_p and err from bottom is err_u.
;; Here's one way to do this:
;; 
;; (def s500 (take 500 stages)) ; number of timesterr Bogacz uses: (/ 5 0.01)
;; (use '[incanter.charts])
;; (def xy (xy-plot (range) (map (comp :phi second) s500)))
;; (add-lines xy    (range) (map (comp :err second) s500))
;; (add-lines xy    (range) (map (comp :err first)  s500))
;; (use '[incanter.core])
;; (view xy)
;; (use '[incanter.pdf])
;; (save-pdf xy "ex3.pdf")

;;;;;;;;;;;;;;;;;;

(def dt 0.01) ; version in Bogacz

;; all-level parameters
(def gen-wt 1) ; i.e. pass value of gen(phi) through unchanged
(defn gen  [phi] (* phi phi))
(defn gen' [phi] (* phi 2))

;; bottom level params
(def u 2)       ; phi
(def error-u 0) ; err
(def sigma-u 1)
(def next-bottom (lvl/make-next-bottom (constantly u)))

;; middle level params
(def v-p 3)    ; what phi is initialized to
(def error-p 0)
(def sigma-p 1)

(def init-bot
  (lvl/map->Level {:phi u
                  :err error-u
                  :sigma sigma-u
                  :gen-wt gen-wt   ; preserves gen(phi)
                  :gen  nil ; unused at bottom since err update uses higher gen
                  :gen' nil ; unused at bottom since phi comes from outside
                  :phi-dt dt
                  :err-dt dt
                  :sigma-dt 0    ; sigma never changes
                  :gen-wt-dt 0})) ; gen-wt never changes

(def init-mid
  (lvl/map->Level {:phi v-p
                  :err error-p
                  :sigma sigma-p
                  :gen-wt gen-wt       ; preserves gen(phi)
                  :gen  gen
                  :gen' gen'
                  :phi-dt dt
                  :err-dt dt
                  :sigma-dt 0    ; sigma never changes
                  :gen-wt-dt 0})) ; gen-wt never changes

(def top (lvl/make-top-level v-p))

(def init-levels [init-bot init-mid top])

(def stages (iterate (partial lvl/next-levels next-bottom) init-levels))
