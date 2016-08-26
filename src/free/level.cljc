;; Based on
;; Rafal Bogacz, "A Tutorial on the Free-energy Framework for Modelling
;; Perception and Learning", *Journal of Mathematical Psychology*,
;; http://dx.doi.org/10.1016/j.jmp.2015.11.003 .)

;; Conventions:
;; The derivative of x is called x' .
;; A value of x from the next level down is called lower-x.
;; A value of x from the next level up is called upper-x.

(ns free.level
  (:require
    [free.scalar-arithmetic :refer [e* m* e+ e-]]   ; use only one
    ;[free.matrix-arithmetic :refer [e* m* e+ e-]] ; of these (this seems to work with scalars!)
   ))


(defn g'-fn
  [h theta]
  (fn [phi]
    (e* theta (h phi))))

(defn phi-inc
  "Equation (53) in Bogacz's \"Tutoria\"."
  [phi eps lower-eps g']
  (e+ (e- eps)
      (m* (g' phi) lower-eps))) ; is this right?

(defn next-phi 
  "Usage e.g. (next-phi phi eps lower-eps (g'-fn h theta))."
  [phi eps lower-eps g']
  (e+ phi 
      (phi-inc phi eps lower-eps g')))


(defn g-fn
  [h theta]
  (fn [phi]
    (m* theta (h phi))))

(defn eps-inc 
  "Equation (54) in Bogacz's \"Tutoria\"."
  [eps phi upper-phi sigma g] 
  (e- phi 
      (g upper-phi)
      (m* sigma eps)))

(defn next-eps
  "Usage e.g. (next-eps eps phl upper-phi sigma (g-fn h theta))."
  [eps phi upper-phi sigma g]
  (+ eps 
     (eps-inc eps phi upper-phi sigma g)))


;; Ex. 3
(def v-p 3)
(def sigma-p 1)
(def sigma-u 1)
(def u 2)
(def dt 0.01)
(def phi v-p)
(def error-p 0)
(def error-u 0)


;(defn phi-inc [eps h-tick phi lower-theta lower-eps] 
;  "Equation (53) in Bogacz's \"Tutoria\"."
;  (e+ (e- eps)
;      (e* (h-tick phi)
;          (m* lower-theta lower-eps))))
;
;(defn eps-inc [eps h phi theta sigma upper-phi] 
;  "Equation (54) in Bogacz's \"Tutoria\"."
;  (e- phi 
;      (m* theta (h upper-phi))
;      (m* sigma eps)))
