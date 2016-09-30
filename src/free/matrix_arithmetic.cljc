#?(:clj  (ns free.matrix-arithmetic
	   (:require [clojure.core.matrix :as mx]))
   :cljs (ns free.matrix-arithmetic
	    (:require [clojure.core.matrix :as mx])))
                      ;[thinktopic.aljabr.core :as imp])))

#?(:clj  (mx/set-current-implementation :vectorz)
   :cljs (mx/set-current-implementation :persistent-vector)) ; won't load it, but set default for e.g. mx/matrix
   ;:cljs (mx/set-current-implementation :aljabr)) ; won't load it, but set default for e.g. mx/matrix

;; List of all namespaces of implementations in KNOWN-IMPLEMENTATIONS in
;; https://github.com/mikera/core.matrix/blob/develop/src/main/clojure/clojure/core/matrix/implementations.cljc

;; Clojurescript options:
;; [clojure.core.matrix.impl.ndarray-object :as imp] ;; (why did I think this worked in Clojurescript?)
;; [thinktopic.aljabr.core :as imp]

;; Clojure options:
;; (mx/set-current-implementation :ndarray)
;; (mx/set-current-implementation :aljabr)
;; (mx/set-current-implementation :vectorz)
;; (mx/set-current-implementation :clatrix)
;; (mx/set-current-implementation :nd4clj)

(println "Loading core.matrix operators.  Matrix implementation:" (mx/current-implementation))


;; Note that these are functions, but in free.scalar-arithmetic, I define 
;; them as macros for the sake of performance.  So don't e.g. map the functions 
;; below over a sequence, if you want to preserve substitutability with their 
;; scalar analogues.
(def m* mx/mmul) ; matrix multiplication and inner product
(def e* mx/mul)  ; elementwise (Hadamard) and scalar multiplication
(def m+ mx/add)  ; elementwise addition
(def m- mx/sub)  ; elementwise subtraction, or elementwise negation
(def tr mx/transpose)
(def inv mx/inverse)
(def make-identity-obj mx/identity-matrix)
(def pm mx/pm)

;; btw There's also an e* in core.matrix (which is *almost* identical to mul)
;; but by qualifying core.matrix with mx, it doesn't matter.

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; These have no (and need no) equivalents in scalar-arithmetic:

(defn col-mat
  "Turns a sequence of numbers xs into a column vector."
  [xs]
  (mx/matrix (map vector xs)))

(defn row-mat
  "Turns a sequence of numbers xs into a row vector."
  [xs]
  (mx/matrix (vector xs)))
