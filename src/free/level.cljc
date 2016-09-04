;;; This software is copyright 2016 by Marshall Abrams, and
;;; is distributed under the Gnu General Public License version 3.0 as
;;; specified in the file LICENSE.

;; Based on Rafal Bogacz's, "A Tutorial on the Free-energy Framework 
;; for Modelling Perception and Learning", _Journal of Mathematical 
;; Psychology_ (online 2015), http://dx.doi.org/10.1016/j.jmp.2015.11.003

;; Conventions:
;; The derivative of x is called x' .
;; A value of x at the next level down is called -x.
;; A value of x at the next level up is called +x.
;; m*, m+, m- are either scalar or matrix *, +, and -, depending
;; on which namespace you load.  e* is *, or elementwise matrix
;; multiplication.

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
    ;[free.dists :as prob]
    [utils.string :as us]))

;; maybe move elsewhere so can be defined on command line?
(def ^:const use-core-matrix false)

(if use-core-matrix
  (require '[free.matrix-arithmetic :refer [e* m* m+ m- tr inv]])
  (require '[free.scalar-arithmetic :refer [e* m* m+ m- tr inv]]))

;; Q: Does e need to be in the record structure, or can it be local
;; to functions?  What initializes it?
(defrecord Level [phi eps sigma theta h h']) ; to add?: e for Hebbian sigma calculation
(us/add-to-docstr! ->Level
  "\n  A Level records values at one level of a prediction-error/free-energy
  minimization model.  phi and eps can be scalars, in which case
  theta and sigma are as well.  Or phi and eps can be vectors of
  length n, in which case sigma and theta are n x n square matrices.  h
  and h' are functions that can be applied to things with the form of
  phi.  These variables are defined in Bogacz's \"Tutorial\" paper and
  are used in one form or another throughout the paper (q.v.).  
  h and h' might be the same on every level.  Together with theta they 
  define the functions g and g' appearing in the paper (q.v.).

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


;;;;;;;;;;;;;;;;;;;;;
;; phi update

(defn phi-inc
  "Calculates slope/increment to the next 'hypothesis' phi from the 
  current phi.  See equations (44), (53) in Bogacz's \"Tutorial\"."
  [phi eps -eps theta h']
  (m+ (m- eps)
      (e* (h' phi)
          (m* (tr theta) -eps))))

(defn next-phi 
  "Accepts three subsequent levels, but only uses this one and the one below. 
  Calculates the the next-timestep 'hypothesis' phi."
  [-level level _]
  (let [{:keys [phi eps theta h']} level
        -eps (:eps -level)]
    (m+ phi 
        (phi-inc phi eps -eps theta h'))))


;;;;;;;;;;;;;;;;;;;;;
;; epsilon update

(defn eps-inc 
  "Calculates the slope/increment to the next 'error' epsilon from 
  the current epsilon.  See equation (54) in Bogacz's \"Tutorial\"."
  [eps phi +phi sigma theta h]
  (m- phi 
      (m* theta (h +phi))
      (m* sigma eps)))

(defn next-eps
  "Accepts three subsequent levels, but only uses this one and the one above. 
  Calculates the next-timestep 'error' epsilon."
  [_ level +level]
  (let [{:keys [eps phi sigma theta h]} level
        +phi (:phi +level)]
    (m+ eps
        (eps-inc eps phi +phi sigma theta h))))

;;;;;;;;;;;;;;;;;;;;;
;; theta update

(defn theta-inc
  "Calculates the slope/increment to the next theta component of the mean
  value function from the current theta.  See equation (56) in Bogacz's 
  \"Tutorial\"."
  [eps +phi h]
  (m* eps 
      (tr (h +phi))))

(defn next-theta
  "Accepts three subsequent levels, but only uses this one and the one above. 
  Calculates the next-timestep theta component of the mean value function."
  [_ level +level]
  (let [{:keys [eps theta h]} level
        +phi (:phi +level)]
    (m+ theta
        (theta-inc eps +phi h))))

;;;;;;;;;;;;;;;;;;;;;
;; sigma update

(defn sigma-inc
  "Calculates the slope/increment to the next sigma from the current sigma,
  i.e.  the variance or the covariance matrix of the distribution of inputs 
  at this level.  See equation (55) in Bogacz's \"Tutorial\".  (Note uses 
  matrix inversion for vector/matrix calcualtions, a non-Hebbian calculation,
  rather than the local update methods of section 5.)"
  [eps sigma]
  (* 0.5 (m- (m* eps (tr eps))
             (inv sigma))))

(defn next-sigma
  "Accepts three subsequent levels, but only uses this one, not the ones
  above or below.  Calculates the next-timestep sigma, i.e. the variance 
  or the covariance matrix of the distribution of inputs at this level."
  [_ level _]
  (let [{:keys [eps sigma]} level]
    (m+ sigma
        (sigma-inc eps sigma))))


;;;;;;;;;;;;;;;;;;;;;

;; from ex. 3
;(def v-p 3)
;(def sigma-p 1)
;(def sigma-u 1)
;(def u 2)
;(def dt 0.01)
;(def phi v-p)
;(def error-p 0)
;(def error-u 0)
