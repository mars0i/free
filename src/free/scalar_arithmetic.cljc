(ns free.scalar-arithmetic)

;; This file contains distasteful kludge since using another name for basic 
;; scalar operators slows them down.
;; i.e. (def m* *) makes multiplication order of magnitude slower.  Solution for
;; scalars is to use a macro--ugh--which preserves speed.  But if we need
;; matrices, then core.matrix will be available, and just alias the core.matrix 
;; operators.

(defmacro m* 
 "Scalar analogue of matrix multiplication and inner product, i.e. scalar
 multiplication."
 [x y] 
 `(* ~x ~y))

(defmacro e* 
 "Scalar analogue of elementwise (Hadamard) multiplication, i.e. scalar 
 multiplication."
 [x y] 
 `(* ~x ~y)) 

(defmacro m+ 
 "Scalar analogue of elementwise addition, i.e. scalar addition."
 [x y] `(+ ~x ~y))

(defmacro m- 
 "Scalar analogue of elementwise subtraction, i.e. scalar subtraction."
 [x y] 
 `(- ~x ~y))

(defmacro neg  ; multi-arity macros are messy. easier to use two definitions.
 "Switches sign of argument."
 [x] 
 `(- ~x)) 

(defmacro trans
 "Scalar analogoue of transposition; returns the argument unchanged."
 [x] `(identity ~x))
