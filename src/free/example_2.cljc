(ns free.example-2
  (:require [clojure.math.numeric-tower :as nt]
            [free.scalar-arithmetic :as ar]
            ;[free.matrix-arithmetic :as ar]
            [free.level :as lvl]
            [free.dists :as pd])) ; will be clj or cljs depending on dialect

;; Generative function phi^2:
(defn h  [phi] (* phi phi)) ;; or: (lvl/m-square phi)
(defn h' [phi] (* phi 2.0))   ;; or: (ar/m* phi 2))

;; Alternative generative function phi^8:
;(defn h  [phi] (nt/expt phi 8))
;(defn h' [phi] (* 8.0 (nt/expt phi 7.0))) 

(def init-theta (ar/make-identity-obj 1)) ; i.e. initially pass value of h(phi) through unchanged

;; simple next-bottom function
;(def next-bottom (lvl/make-next-bottom #(pd/next-gaussian 2 5)))

;; experimental next-bottom function
(def ticks-between 2000)
(def top-tick 50000)
(def tick$ (atom 0))
;; Note that since the generative function is exponential, it's
;; potentially problematic to make the mean negative.
;; TODO I really ought to get rid of this atom stuff and do it instead
;; with args passed along.  Maybe.
(def next-bottom (lvl/make-next-bottom 
                   (let [mean$ (atom 2)
                         sd$ (atom 5)]
                     (fn []
                       (swap! tick$ inc)
                       (when (and
                               (< @tick$ top-tick)
                               (= 0 (mod @tick$ ticks-between)))
                         (println (swap! mean$ #(+ % (* 10 (pd/next-double))))))
                       (pd/next-gaussian @mean$ @sd$)))))

(def sigma-u 2) ; controls degree of fluctuation in phi at level 1
(def error-u 0) ; eps
;; Note that the bottom-level phi needs to be an arbitrary number so that 
;; eps-inc doesn't NPE on the first tick, but the number doesn't matter, and 
;; it will immediately replaced when next-bottom is run.

;; middle level params
(def v-p 3) ; what phi is initialized to, and prior mean at top
(def sigma-p 2) ; controls how close to true value at level 1
(def error-p 0)

(def bot-map {:phi 0 ; needs a number for eps-inc on 1st tick; immediately replaced by next-bottom
              :eps error-u
              :sigma sigma-u
              :theta init-theta
              :h  nil ; unused at bottom since eps update uses higher h
              :h' nil ; unused at bottom since phi comes from outside
              :phi-dt 0.001
              :eps-dt 0.001
              :sigma-dt 0.0
              :theta-dt 0.0})

(def mid-map {:phi v-p
              :eps error-p
              :sigma sigma-p
              :theta init-theta
              :h  h  ; used to calc error at next level down, i.e. eps
              :h' h' ; used to update phi at this level
              :phi-dt 0.00001
              :eps-dt 0.001
              :sigma-dt 0.0001
              :theta-dt 0.0})

(def init-bot (lvl/map->Level bot-map))
;; mid-level state with adjustable theta:
(def init-mid (lvl/map->Level mid-map))
;; alt mid-level state with fixed theta:
(def init-mid-fixed-theta (lvl/map->Level (assoc mid-map :theta-dt 0.0)))
(def top (lvl/make-top-level v-p)) ; will have phi, and identity as :h ; other fields will be nil

(def stages (iterate (partial lvl/next-levels next-bottom) [init-bot init-mid top]))

;; FIXME NOT WORKING RIGHT:
;(reset! tick$ 0)
;(def flux-theta-2  (iterate (partial lvl/next-levels next-bottom)
;                            [init-bot init-mid top]))
;(reset! tick$ 0)
;(def flux-theta-3  (iterate (partial lvl/next-levels next-bottom)
;                            [init-bot init-mid top]))
;(reset! tick$ 0)
;(def fixed-theta (iterate (partial lvl/next-levels next-bottom)
;                          [init-bot init-mid-fixed-theta top]))
