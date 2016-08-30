(ns free.matrix-arithmetic
  (:require [clojure.core.matrix :as mx :exclude [e*]]))
;; There's also an e* in core.matrix (which is *almost* identical to mul)

;; Note set-current-implementation has a global effect--not just in this namespace.
;; These work with Clojurescript as well as Clojure:
(mx/set-current-implementation :ndarray)
;(set-current-implementation :aljabr)
;; Clojure only:
;(set-current-implementation :vectorz)
;; These are Clojure only, but unlikely to be optimal for this application at this time:
;(set-current-implementation :clatrix)
;(set-current-implementation :nd4clj)


;; Note that these are functions, but in free.scalar-arithmetic, I define 
;; them as macros for the sake of performance.  So don't e.g. map the functions 
;; below over a sequence, if you want to preserve substitutability with their 
;; scalar analogues.
(def m* mx/mmul) ; matrix multiplication and inner product
(def e* mx/mul)  ; elementwise (Hadamard) and scalar multiplication
(def m+ mx/add)  ; elementwise addition
(def m- mx/sub)  ; elementwise subtraction, or elementwise negation
(def neg mx/sub) ; elementwise sign change--for use with single argument
(def trans mx/transpose)

;(defmacro m* 
; "Scalar analogue of matrix multiplication and inner product, i.e. scalar
; multiplication."
; [x y] 
; `(mx/mmul ~x ~y))
;
;(defmacro e* 
; "Scalar analogue of elementwise (Hadamard) multiplication, i.e. scalar 
; multiplication."
; [x y] 
; `(mx/mul ~x ~y)) 
;
;(defmacro e+ 
; "Scalar analogue of elementwise addition, i.e. scalar addition."
; [x y] `(mx/add ~x ~y))
;
;(defmacro e- 
; "Scalar analogue of elementwise subtraction, i.e. scalar subtraction."
; [x y] 
; `(mx/sub ~x ~y))
;
;(defmacro neg  ; multi-arity macros are messy. easier to use two definitions.
; "Switches sign of argument."
; [x] 
; `(mx/sub ~x)) 
;
;(defmacro trans
; "Scalar analogoue of transposition; returns the argument unchanged."
; [x] `(mx/transpose ~x))
