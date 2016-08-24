(ns free.nets
  (:require [clojure.core.matrix :as mx]))

;; use one of these (should be moved to core.clj or some other place):
;(mx/set-current-implementation :vectorz)
(mx/set-current-implementation :ndarray)

;; DO I REALLY NEED all of this protocol/record boilerplate?
;; It's a very simple model.  Maybe I should just use the vectors
;; and matrices raw.

(defprotocol NodesP
  "Protocol for Nodes."
  (eps-v [nodes] "Simulus error.")
  (eps-p [nodes] "Prior dist error.")
  (v     [nodes] "Stimulus. v at level 1 is another name for u.") 
  (phi   [nodes] "Hypothesis."))

(defrecord Nodes [mat]
  NodesP
  (eps-v [nodes] (mx/mget mat 0))
  (eps-p [nodes] (mx/mget mat 1))
  (v     [nodes] (mx/mget mat 2)) 
  (phi   [nodes] (mx/mget mat 3)))

(defn make-nodes 
  "Make a Nodes object, initializing its vector with the arguments, 
  along with 1 as the last, constant element.  Note that the vector
  is a plain single-dimensional vector that can be treated as column
  or row depending on context."
  [eps-v eps-p v phi]
  (Nodes. (mx/matrix [eps-v eps-p v phi 1])))

(defprotocol UpdateP
  "Protocol for Update matrices."
  (sigma-v [update])
  (sigma-p [update])
  (v-p     [update]))

(defrecord Update [mat]
  UpdateP
  (sigma-v [update] (mx/mget mat 0))
  (sigma-p [update] (mx/mget mat 1))
  (v-p     [update] (mx/mget mat 2)))

(defn update-nodes 
  "Multiply the matrix in update (or the result of f applied to update, 
  if f is provided) and the vector in nodes, and return a new Nodes 
  containing the result."
  ([f update nodes] (update-nodes (f update) nodes))
  ([update nodes] 
   (Nodes. (mx/mmul (:mat update) (:mat nodes)))))




;(defprotocol UpdateP
;  "Protocol for Nodes update matrix."
;  (sigma-v [update] "Variance of v.")
;  (sigma-p [update] "Variance of prior dist.")
;  (v-p [update] "Mean of prior dist.")
;  (g [update] "Function for chosing mean of likelihood."))
