(ns free.matrix-arithmetic
  (:require [clojure.core.matrix :as mx :exclude [e]]))
;; There's also an e* in core.matrix, which is *almost* identical to mul.

(def e+ mx/add)  ; elementwise addition
(def e- mx/sub)  ; elementwise subtraction, or elementwise negation
(def m* mx/mmul) ; matrix multiplication and inner product
(def e* mx/mul)  ; elementwise (Hadamard) and scalar multiplication
