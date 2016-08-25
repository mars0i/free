;; Based on
;; Rafal Bogacz, "A Tutorial on the Free-energy Framework for Modelling
;; Perception and Learning", *Journal of Mathematical Psychology*,
;; http://dx.doi.org/10.1016/j.jmp.2015.11.003 .)

(ns free.level
  (:require
    [clojure.core.matrix :as mx :exclude [e*]])) ; e* isn't completely equiv to mul as it claims, maybe
  ;[free.scalar-arithmetic :refer [mul mmul add sub]] ; use only one
  ;[free.matrix-arithmetic :refer [mul mmul add sub]] ; of these (this seems to work with scalars!)
  ;; This may be a bad idea:
  ;(:refer-clojure :exclude [+ -])
  ;(:use [clojure.core.matrix :only [mmul mul add sub]])
  ;; It might be better to use core.matrix.operators for + and -.

;; Conventions:
;; The derivative of x is called x-tick.
;; A value of x from the next level down is called lower-x.
;; A value of x from the next level up is called upper-x.


;; It looks like the matrix operators all work on scalars.  
;; Not sure if this is guaranteed.
(def m* mx/mmul) ; matrix multiplication, inner product
(def e* mx/mul)  ; elementwise multiplication
(def e+ mx/add)  ; elementwise addition
(def e- mx/sub)  ; elementwise subtraction

(defn phi-inc [eps h-tick phi lower-theta lower-eps] 
  "Equation (53) in Bogacz's \"Tutoria\"."
  (e+ (e- eps)
      (e* (h-tick phi)
          (m* lower-theta lower-eps))))

(defn eps-inc [eps h phi theta sigma upper-phi] 
  "Equation (54) in Bogacz's \"Tutoria\"."
  (e- phi 
      (m* theta (h upper-phi))
      (m* sigma eps)))
