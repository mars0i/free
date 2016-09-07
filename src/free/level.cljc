;;; This software is copyright 2016 by Marshall Abrams, and
;;; is distributed under the Gnu General Public License version 3.0 as
;;; specified in the file LICENSE.

;; Based on Rafal Bogacz's, "A Tutorial on the Free-energy Framework 
;; for Modelling Perception and Learning", _Journal of Mathematical 
;; Psychology_ (online 2015), http://dx.doi.org/10.1016/j.jmp.2015.11.003

;; SEE doc/level.md for documentation on general features of the code below.


;; TODO NOTE: On p7c2, end of section 3, Bogacz says:
;; "Thus on each trial we need to modify the model parameters a little bit
;; (rather than until minimum of free energy is reached as was the case for
;; phi)."  i.e., I think, this means that updating sigma and theta
;; should be done more gradually, i.e. over more timesteps, i.e. based
;; on more sensory input filtering up, than updating phi.  What about
;; epsilon?  I think that should go at the speed of phi, right?
;; 
;; cf. end of section 5, where he says that the Hebbian Sigma update 
;; methods (which I'm not using, initially) introduced there depend on phi 
;; changing more slowly.  But isn't that the opposite of what I just said??
;; 
;; Also, should the higher levels also go more slowly??  i.e. as you go
;; higher, you update less often?  Or not?


(ns free.level
  (:require 
    ;[free.dists :as prob]
    [utils.string :as us]))

;; maybe move elsewhere so can be defined on command line?
(def ^:const use-core-matrix false)

;; dimensions of vectors, or 1 for scalars
;; redefine this to the vector length if you use vectors/matrices
(def dims 1)

(if use-core-matrix
  (require '[free.matrix-arithmetic :refer [e* m* m+ m- tr inv id]])
  (require '[free.scalar-arithmetic :refer [e* m* m+ m- tr inv id]]))

(defrecord Level [phi eps sigma theta h h']) ; to add?: e for Hebbian sigma calculation
(us/add-to-docstr! ->Level
  "\n  A Level records values at one level of a prediction-error/free-energy
  minimization model.  
  phi:   Current value of input at this level.
  eps:   Epsilon--the error at this level.
  sigma: Covariance matrix or variance of assumed distribution over inputs 
         at this level.  Variance should usually be >= 1 (p. 5 col 2).
  theta: When theta is multiplied by result of h(phi), the result is the 
         current estimated mean of the assumed distrubtion.  
         i.e. g(phi) = theta * h(phi), where '*' here is scalar or matrix 
         multiplication as appropriate.
  h, h': See theta; h' is the derivative of h.  These never change.

  All of these notations are defined in Bogacz's \"Tutorial\" paper.
  phi and eps can be scalars, in which case theta and sigma are as well.  
  Or phi and eps can be vectors of length n, in which case sigma and theta
  are n x n square matrices.  h and h' are functions that can be applied to 
  phi.  

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
(declare phi-inc   next-phi 
         eps-inc   next-eps 
         sigma-inc next-sigma
         theta-inc next-theta)

;;;;;;;;;;;;;;;;;;;;;
;; Utility functions

(defn m-square
  [x]
  (m* x (tr x)))

;;;;;;;;;;;;;;;;;;;;;
;; Functions to calculate next state of system

(defn next-level
  "Returns the value of this level for the next timestep."
  [[-level level +level]]
  (->Level (next-phi   -level level +level)
           (next-eps   -level level +level)
           (next-sigma -level level +level)
           (next-theta -level level +level)
           (:h  level)
           (:h' level)))

(defn next-levels
  "Given a functions for updating h, h', a bottom level, and a top level, along
  with a sequence of levels at one timestep, returns a vector of levels at the 
  next timestep."
  [next-bottom next-top levels]
  (concat [(next-bottom (first levels))] ; Bottom level is special case.
          (map next-level                ; Each middle level depends on levels
               (partition 3 1 levels))   ;  immediately below and above it.
          [(next-top (last levels))]))   ; Top level is special case.

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
;; sigma update

(defn sigma-inc
  "Calculates the slope/increment to the next sigma from the current sigma,
  i.e.  the variance or the covariance matrix of the distribution of inputs 
  at this level.  See equation (55) in Bogacz's \"Tutorial\".  (Note uses 
  matrix inversion for vector/matrix calcualtions, a non-Hebbian calculation,
  rather than the local update methods of section 5.)"
  [eps sigma]
  (* 0.5 (m- (m-square eps)
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
;; These define the function g that Bogacz offers as an example on p. 2.
;; i.e. for g(phi) = theta * h(phi), where g just squares its argument.

(def example-theta (id dims))
(defn example-h [phi] (m-square phi))
(defn example-h' [phi] (m* phi 2))

;; from ex. 3
;(def v-p 3)
;(def sigma-p 1)
;(def sigma-u 1)
;(def u 2)
;(def dt 0.01)
;(def phi v-p)
;(def error-p 0)
;(def error-u 0)
