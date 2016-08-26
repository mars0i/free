(ns free.matrix-arithmetic
  (:require [clojure.core.matrix :as mx :exclude [e]]))
;; There's also an e* in core.matrix, which is *almost* identical to mul.

;; these work with Clojurescript as well as Clojure:
;(set-current-implementation :ndarray)
(set-current-implementation :aljabr)

;; Clojure only:
;(set-current-implementation :vectorz)

;; These are Clojure only, but unlikely to be optimal for this application at this time:
;(set-current-implementation :clatrix)
;(set-current-implementation :nd4clj)


(def e+ mx/add)  ; elementwise addition
(def e- mx/sub)  ; elementwise subtraction, or elementwise negation
(def m* mx/mmul) ; matrix multiplication and inner product
(def e* mx/mul)  ; elementwise (Hadamard) and scalar multiplication
