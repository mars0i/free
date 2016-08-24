(ns free.matrix-arithmetic
  (:require [clojure.core.matrix :as mx]))

(def mul  mx/mul)  ; elementwise (Hadamard) and scalar multiplication
(def mmul mx/mmul) ; matrix multiplication and inner product
(def add  mx/add)  ; elementwise addition
(def sub  mx/sub)  ; elementwise subtraction, or elementwise negation
