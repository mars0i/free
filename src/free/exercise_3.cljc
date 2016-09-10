;; Exercise 3 in Bogacz

(ns free.exercise-3
  (:use [free.scalar-arithmetic])
  (:require [free.level :as lv]
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
;; (view xy)
;; (use '[incanter.pdf])
;; (save-pdf xy "ex3.pdf")

;;;;;;;;;;;;;;;;;;

(def dt 0.01) ; version in Bogacz

(def u 2)
(def error-u 0)
(def sigma-u 1)
(def next-bottom (lv/make-next-bottom (constantly u)))

(def v-p 3)
(def top-v-p 1.732) ; sqrt of 3 - see note below
;(def top-v-p 3)
(def error-p 0)
(def sigma-p 1)

;; additional defs needed:
(def I (make-identity-obj dims))
(defn h  [phi] (lv/m-square phi))
(defn h' [phi] (m* phi 2))

;; NOTE:
;; OK--wait a minute.  In the answer code for ex 3 in Bogacz, the phi
;; update (i.e. middle level) uses h', i.e. 2*phi, but the error update
;; for the mid level (_p) uses v_p as is.  i.e. not squared.  That's the
;; mean from the top, but it's not passed through h().
;; h=phi^2 is, however, used in the error update for the _u level, i.e.
;; the bottom level.
;;
;; This suggests that my treatment of the top is wrong.
;; 
;; However, in (53) and (54), for example, the variables are level-indexed,
;; but h and h' are not.  What you see there is that the upper phi is
;; always passed through h in the update of epsilon.  And my formula
;; for eps-inc in level.cljc is just read off of that.  Well maybe the
;; idea is that if incorporate the square already into v_p, i.e. the
;; mean at the top should be sqrt(v_p).  But note that the middle phi is
;; initialized to v_p.  Or I could just make h but not h' in middle leve
;; into identity.

(def init-bot
  (lv/map->Level {:phi u
                  :eps error-u
                  :sigma sigma-u
                  :theta I       ; preserves h(phi)
                  :h  h
                  :h' h'
                  :phi-dt   dt
                  :eps-dt   dt
                  :sigma-dt 0    ; sigma never changes
                  :theta-dt 0})) ; theta never changes

(def init-mid
  (lv/map->Level {:phi v-p
                  :eps error-p
                  :sigma sigma-p
                  :theta I       ; preserves h(phi)
                  :h  h
                  :h' h'
                  :phi-dt   dt
                  :eps-dt   dt
                  :sigma-dt 0    ; sigma never changes
                  :theta-dt 0})) ; theta never changes

(def top (lv/map->Level {:phi top-v-p})) ; other fields will be nil

(def init-levels [init-bot init-mid top])

(def stages (iterate (partial lv/next-levels next-bottom) init-levels))
