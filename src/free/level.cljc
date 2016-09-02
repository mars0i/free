;;; This software is copyright 2016 by Marshall Abrams, and
;;; is distributed under the Gnu General Public License version 3.0 as
;;; specified in the file LICENSE.

;; Based on Rafal Bogacz's, "A Tutorial on the Free-energy Framework 
;; for Modelling Perception and Learning", _Journal of Mathematical 
;; Psychology_ (online 2015), http://dx.doi.org/10.1016/j.jmp.2015.11.003

;; Conventions:
;; The derivative of x is called x' .
;; A value of x at the next level down is called x-.
;; A value of x at the next level up is called x+.
;; However, + and - are also used in matrix/scalar operators
;; m+ (addition) and m- (subtraction).

;; TODO:
;; Write update functions for sigma (using e) and theta.
;; Alternatively we could calculate sigma directly using matrix inversion.

;; This version doesn't use function g, but assumes that g is a product
;; of theta with another function h, as in Bogacz's examples.
;; (See older commits for g defs.)

;; On p.2 Bogacz uses g(v) = v^2 as his example.
;; i.e. h(phi) = phi^2, theta = 1, h'(phi) = 2*phi.

(ns free.level
  (:require 
    [free.dists :as prob]
    [utils.string :as us]))

;; maybe move elsewhere so can be defined on command line?
(def ^:const use-core-matrix false)

(if use-core-matrix
  (require '[free.matrix-arithmetic :refer [e* m* m+ m- trans]])
  (require '[free.scalar-arithmetic :refer [e* m* m+ m- trans]]))

;; Q: Does e need to be in the record structure, or can it be local
;; to functions?  What initializes it?
(defrecord Level [phi eps sigma theta h h' e])
(us/add-to-docstr! ->Level
  "\n  A Level records values at one level of a prediction-error/free-energy
  minimization model.  phi, eps, and e can be scalars, in which case
  theta and sigma are as well.  Or phi, eps, and e can be vectors of
  length n, in which case sigma and theta are n x n square matrices.  h
  and h' are functions that can be applied to things with the form of
  phi.  These variables are defined in Bogacz's \"Tutorial\" paper and
  are used in one form or another throughout the paper (q.v.).  e is a
  helper variable used to represent additional nodes used to update
  sigma; see section 5 of the paper.  h and h' are usually the same on
  every level.  Together with theta they define the functions g and g'
  appearing in the paper (q.v.).

  The state of a network consists of a sequence of three or more levels:
  A first and last level, and one or more inner levels.  It's only the
  inner levels that should be updated according to central equations in
  Bogacz such as (53) and (54).  The first level captures sensory
  input-- i.e. it records the prediction error eps, which is calculated
  from sensory input phi at that level, along with a function theta h of
  the next level phi.  i.e. at this level, phi is simply provided by the
  system outside of the levels, and is not calculated from lower level
  prediction errors as in (53). The last level simply provides a phi,
  which is the mean of a prior distribution at that level.  This phi
  typically never changes. (It's genetically or developmentally
  determined.) The other terms at this top level can be ignored.
  Note that Bogacz's examples typically use two inner levels; his
  representation captures what's called the first and last levels
  here using individual parameters such as u and v_p.")


;;; phi update

(defn phi-inc
  "Calculates slope/increment to the next 'hypothesis' phi from the 
  current phi.  See equations (44), (53) in Bogacz's \"Tutorial\"."
  [phi eps eps- theta h']
  (m+ (m- eps)
      (e* (h' phi)
          (m* (trans theta) eps-))))

(defn next-phi 
  "Accepts two levels, this one and the one below, and calculates the
  the next-timestep 'hypothesis' phi."
  [level- level]
  (let [{:keys [phi eps theta h']} level
        eps- (:eps level-)]
    (m+ phi 
        (phi-inc phi eps eps- theta h'))))


;;; epsilon update

;; Note per (73), (58), sigma is supposed to be:
;    (let [g-phi+ (m* theta (h phi+)) ; g(phi+)
;          d (m- phi g-phi+)]         ; phi - g(phi+)
;      (E (m* d (trans d))))          ; expectation of square of d
;; where E is the expectation operator (over the empirical
;; distribution of values?).

(defn eps-inc 
  "Calculates the slope/increment to the next 'error' epsilon from 
  the current epsilon.  See equation (54) in Bogacz's \"Tutorial\"."
  [eps phi phi+ sigma theta h]
  (m- phi 
      (m* theta (h phi+))
      (m* sigma eps)))

(defn next-eps
  "Accepts two levels, this one and the one above, and calculates the
  the next-timestep 'error' epsilon."
  [level level+]
  (let [{:keys [eps phi sigma theta h]} level
        phi+ (:phi level+)]
    (m+ eps
        (eps-inc eps phi phi+ sigma theta h))))


;; from ex. 3
(def v-p 3)
(def sigma-p 1)
(def sigma-u 1)
(def u 2)
(def dt 0.01)
(def phi v-p)
(def error-p 0)
(def error-u 0)
