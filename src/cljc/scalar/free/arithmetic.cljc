;; This software is copyright 2016 by Marshall Abrams, and is distributed
;; under the Gnu General Public License version 3.0 as specified in the
;; the file LICENSE.

;; fast scalar arithmetic
;; there's another version of this namespace for matrix arithmetic

;; Note that for Clojurescript, this is a macro namespace, which means that it only runs
;; in Clojure, before Clojurescript compilation.  So there's no reason to use any reader macros
;; except to prevent something from happening during Clojurescript pre-compilation.

(ns free.arithmetic)

(def sigma-min 1.0)

;; My cheesey macros only accept three arguments

(defmacro m* 
  "Scalar analogue of matrix multiplication and inner product, i.e. scalar
  multiplication."
  ([x] x) ; is this right?
  ([x y] `(* ~x ~y))
  ([x y z] `(* ~x ~y ~z)))

(defmacro e* 
  "Scalar analogue of elementwise (Hadamard) multiplication, i.e. scalar 
  multiplication."
  ([x] x)
  ([x y] `(* ~x ~y))
  ([x y z] `(* ~x ~y ~z)))

(defmacro m+ 
  "Scalar analogue of elementwise addition, i.e. scalar addition."
  ([x] x)
  ([x y] `(+ ~x ~y))
  ([x y z] `(+ ~x ~y ~z)))

(defmacro m- 
  "Scalar analogue of elementwise subtraction, i.e. scalar subtraction."
  ([x] `(- ~x))
  ([x y] `(- ~x ~y))
  ([x y z] `(- ~x ~y ~z)))

(defmacro tr
  "Scalar analogue of transposition; returns the argument unchanged."
  [x]
  x) ; just substitute the value into context, to be evaluated.

(defmacro inv
  "Scalar analogue of matrix inversion, i.e. scalar reciprocal, divide into 1.0."
  [x]
  `(/ 1.0 ~x))

(defmacro make-identity-obj
  "Returns 1, the identity operator for scalar multiplication.  Throws exception
  if anything other than 1 is passed as dims."
  [dims]
  (when (not (== dims 1))  ;; Should get evaluated at compile time:
    (throw 
      #?(:clj  (Exception. (str "The value " dims " was passed as dims, but this version of function is defined only for dims = 1"))
         :cljs (js/Error.  (str "The value " dims " was passed as dims, but this version of function is defined only for dims = 1")))))
  1) ; 1 is self-evaluating; no need for `()

;; see Bogacz end of sect 2.4
;; make it a macro simply because the others are (hack for Clojurescript)
(defmacro limit-sigma
  [sigma]
  `(if (< ~sigma ~sigma-min)
     ~sigma-min
     ~sigma))
