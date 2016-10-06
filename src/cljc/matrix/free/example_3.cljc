;; simple vector/matrix example

(ns free.example-3
  (:require [free.level :as lvl]
            [clojure.core.matrix :as mx]
	    ;[thinktopic.aljabr.core :as imp] ;; IS THIS NEEDED IN THIS FILE??  IF SO WRAP IN #?()
            [free.random :as ran]) ; clj or cljs depending on dialect
  #?(:clj  (:require [free.arithmetic :as ar])
     :cljs (:require-macros [free.arithmetic :as ar])))

;(mx/set-current-implementation :aljabr) ;; IS THIS NEEDED IN THIS FILE??  IF SO WRAP in #?()

;; Since these next three functions run on every tick, maybe slightly
;; faster not to use ar/col-mat:

(defn gen [phi] (let [x1 (mx/mget phi 0 0)
                      x2 (mx/mget phi 1 0)]
                  (mx/matrix [[(* x1 x1 x2)]
                              [(* x2 x2 x1)]])))

(defn gen' [phi] (let [x1 (mx/mget phi 0 0)
                       x2 (mx/mget phi 1 0)]
                   (mx/matrix [[(* x2 2.0 x1)]
                               [(* x1 2.0 x2)]])))

(def next-bottom (lvl/make-next-bottom 
                   #(mx/matrix [[(ran/next-gaussian  2 5)]
                                [(ran/next-gaussian -1 3)]])))

(def init-gen-wt (ar/make-identity-obj 2)) ; i.e. initially pass value of gen(phi) through unchanged

; what phi is initialized to, and prior mean at top:
(def v-p (ar/col-mat [3.0 3.0]))

(def bot-map {:phi   (ar/col-mat [0.0 0.0]) ; immediately replaced by next-bottom
              :err   (ar/col-mat [0.0 0.0])
              :sigma (mx/matrix [[2.0  0.25]  ; it's a covariance matrix, so
                                 [0.25 2.0]]) ; should be symmetric
              :gen-wt init-gen-wt
              :gen  nil
              :gen' nil
              :phi-dt    0.01
              :err-dt    0.01
              :sigma-dt  0.0
              :gen-wt-dt 0.0})

(def mid-map {:phi v-p
              :err   (ar/col-mat [0.0 0.0])
              :sigma (mx/matrix [[2.0  0.25]
                                 [0.25 2.0]])
              :gen-wt init-gen-wt
              :gen  gen
              :gen' gen'
              :phi-dt    0.0001
              :err-dt    0.01
              :sigma-dt  0.0001
              :gen-wt-dt 0.01})

(def init-bot (lvl/map->Level bot-map))
(def init-mid (lvl/map->Level mid-map))
(def top      (lvl/make-top-level v-p))

(defn make-stages [] (iterate (partial lvl/next-levels next-bottom)
                              [init-bot init-mid top]))
