(ns free.scalar-arithmetic)

;; This file contains distasteful kludge since using another name for basic 
;; scalar operators slows them down.
;; i.e. (def m* *) makes multiplication order of magnitude slower.  Solution for
;; scalars is to use a macro--ugh--which preserves speed.  But if we need
;; matrices, then core.matrix will be available, and just alias the core.matrix 
;; operators.

(println "Loading scalar operators.")

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
  x) ; just substitute the value into context, evaluated.

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
      (Exception. 
        (str "The value " dims " was passed as dims, but this version of function is defined only for dims = 1"))))
  1) ; 1 is self-evaluating; no need for `()

