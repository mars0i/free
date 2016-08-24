(ns free.level
  (:require
     [free.scalar-arithmetic :refer [mul mmul add sub]] ; use only one
     ;[free.matrix-arithmetic :refer [mul mmul add sub]] ; of these
  ))

;; Based on
;; Rafal Bogacz, "A Tutorial on the Free-energy Framework for Modelling
;; Perception and Learning", *Journal of Mathematical Psychology*,
;; http://dx.doi.org/10.1016/j.jmp.2015.11.003 .)

;; Conventions:
;; The derivative of x is called x-tick.
;; A value of x from the next level down is called lower-x.
;; A value of x from the next level up is called upper-x.

(defn phi-inc [eps h-tick phi lower-theta lower-eps] 
  "Equation (53) in Bogacz's \"Tutoria\"."
  (add (sub eps)
       (mul (h-tick phi)
            (mmul lower-theta lower-eps))))

(defn eps-inc [eps h phi theta sigma upper-phi] 
  "Equation (54) in Bogacz's \"Tutoria\"."
  (sub phi 
       (mmul theta (h upper-phi))
       (mmul sigma eps)))
