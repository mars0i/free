(ns free.quant)

(defprotocol Quant
  (m* [x] [x y])
  (e* [x] [x y])
  (e+ [x] [x y])
  (e- [x] [x y])
  (transpose [x]))

;; not bothering with arities > 2 at this point
(extend-protocol Quant
  java.lang.Number
  (m*
    ([x] x)
    ([x y] (* x y)))
  (e*
    ([x] x)
    ([x y] (* x y)))
  (e+
    ([x] x)
    ([x y] (+ x y)))
  (e-
    ([x] x)
    ([x y] (- x y)))
  (transpose
    [x] x))
