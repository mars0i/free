(ns free.example-1
  (:require [clojure.math.numeric-tower :as nt]
            [free.scalar-arithmetic :as ar]
            ;[free.matrix-arithmetic :as ar]
            [free.level :as lvl]
            [free.dists :as pd])) ; will be clj or cljs depending on dialect

;; all-level parameters

;; see level creation functions for dt's
;(def dt 0.01) ; for phi and eps
;(def sigma-dt 0.0)
;(def theta-dt 0.0)

;; This example uses a function h(phi)=phi^2 as its generative function.
;;
;; We are trying to infer a parameter that's the radius of a circle,
;; e.g. because the object is a circle, and the light from it tends
;; to be proportional to its area.  So phi up at the top counts as
;; this radius, but the data is the light, so that follows the square.
;; Thus when we infer the estimate for this phi, it should be the
;; square root of the light.

;; Generative function phi^2:
(defn h  [phi] (* phi phi)) ;; or: (lvl/m-square phi)
(defn h' [phi] (* phi 2.0))   ;; or: (ar/m* phi 2))

;; Alternative generative function phi^8:
;(defn h  [phi] (nt/expt phi 8))
;(defn h' [phi] (* 8.0 (nt/expt phi 7.0))) 

(def theta (ar/make-identity-obj 1)) ; i.e. initially pass value of h(phi) through unchanged

;; bottom level params
(def next-bottom (lvl/make-next-bottom #(pd/sample-normal 1 :mean 2 :sd 5)))

;; simple experiment to make data change over time (doesn't work?):
;(def next-bottom (lvl/make-next-bottom 
;                   (let [mean$ (atom 2)]
;                     (fn []
;                       (when (< (first (pd/sample-uniform 1)) 0.0005)
;                         (println "input mean is now"
;                                  (swap! mean$ #(+ % (rand 8) -4))))
;                       (pd/sample-normal 1 :mean @mean$ :sd 5)))))

(def sigma-u 2) ; controls degree of fluctuation in phi at level 1
(def error-u 0) ; eps
;; Note that the bottom-level phi needs to be an arbitrary number so that 
;; eps-inc doesn't NPE on the first tick, but the number doesn't matter, and 
;; it will immediately replaced when next-bottom is run.

;; middle level params
(def v-p 3) ; what phi is initialized to, and prior mean at top
(def sigma-p 2) ; controls how close to true value at level 1
(def error-p 0)

(def init-bot
  (lvl/map->Level {:phi 0 ; needs a number for eps-inc on 1st tick; immediately replaced by next-bottom
                  :eps error-u
                  :sigma sigma-u
                  :theta theta
                  :h  nil ; unused at bottom since eps update uses higher h
                  :h' nil ; unused at bottom since phi comes from outside
                  :phi-dt 0.001
                  :eps-dt 0.001
                  :sigma-dt 0.0
                  :theta-dt 0.0}))

(def init-mid
  (lvl/map->Level {:phi v-p
                  :eps error-p
                  :sigma sigma-p
                  :theta theta
                  :h  h  ; used to calc error at next level down, i.e. eps
                  :h' h' ; used to update phi at this level
                  :phi-dt 0.00001
                  :eps-dt 0.001
                  :sigma-dt 0.0001
                  :theta-dt 0.001}))

(def top (lvl/make-top-level v-p)) ; will have phi, and identity as :h ; other fields will be nil

(def init-levels [init-bot init-mid top])

(def stages (iterate (partial lvl/next-levels next-bottom) init-levels))
