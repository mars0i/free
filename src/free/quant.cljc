(ns free.quant
  (:require [clojure.core.matrix :as m]
            [clojure.core.matrix.impl.ndarray_object]))

(defprotocol Quant
  (m* [x] [x y])
  (e* [x] [x y])
  (e+ [x] [x y])
  (e- [x] [x y])
  (tran [x]))

;; Since there doesn't seem to be an overarching core.matrix class,
;; we have to specify this protocol for each core.matrix implementation 
;; separately, but each will use the same core.matrix methods, and
;; I'll let core.matrix figure out how to apply the implementation-specific
;; methods.  extend-protocol and extend-type won't let us reuse our
;; definitions, but the lower-level extend will, if we define a map
;; containing the function definitions we want.
(def matrix-method-map
  {:m* (fn ([x] x) ([x y] (m/mmul x y)))
   :e* (fn ([x] x) ([x y] (m/mul  x y)))
   :e+ (fn ([x] x) ([x y] (m/add  x y)))
   :e- (fn ([x] x) ([x y] (m/sub  x y)))
   :tran (fn [x] (m/transpose x))})

(extend clojure.core.matrix.impl.ndarray_object.NDArray
  Quant matrix-method-map)

;; OK this is harder for vectorz, aljabr, and clatrix because they don't use
;; the same leaf class for each object.  Have to investigate whether
;; there's a top-level class.  Also have to make sure it always
;; works for ndarray.


;; Might as well use extend-type to define methods for numbers:
(extend-type java.lang.Number
  Quant
  (m* ([x] x) ([x y] (* x y))) ; i.e. clojure.core/*, etc.
  (e* ([x] x) ([x y] (* x y)))
  (e+ ([x] x) ([x y] (+ x y)))
  (e- ([x] x) ([x y] (- x y)))
  (tran [x] x))
