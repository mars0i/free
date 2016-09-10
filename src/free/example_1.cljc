(ns free.example-1
  (:use [free.scalar-arithmetic]
        ;[free.matrix-arithmetic]
        )
  (:require [clojure.math.numeric-tower :as nt]
            [free.level :as lv]
            [free.dists :as pd])) ; will be clj or cljs depending on dialect

;; How to plot data:
;; (def s500 (take 500 stages)) ; number of timesteps Bogacz uses: (/ 5 0.01)
;; (use '[incanter.charts])
;; (def xy (xy-plot (range) (map (comp :phi second) s500)))
;; (add-lines xy    (range) (map (comp :eps second) s500))
;; (add-lines xy    (range) (map (comp :eps first)  s500))
;; (use '[incanter.core])
;; (view xy)
;; (use '[incanter.pdf])
;; (save-pdf xy "ex3.pdf")

(def dt 0.01) ; version in Bogacz
(def slow-dt 0.00001)

;; all-level parameters
(def theta (make-identity-obj 1)) ; i.e. pass value of h(phi) through unchanged
(defn h  [phi] (lv/m-square phi))
(defn h' [phi] (m* phi 2))

;; bottom level params
(def u 2)       ; phi
(def error-u 0) ; eps
(def sigma-u 1)

(def next-bottom (lv/make-next-bottom #(pd/sample-normal 1 :mean 2 :sd (nt/sqrt 2))))

;; middle level params
(def init-phi-v-p 3)    ; what phi is initialized to
(def error-p 0)
(def sigma-p 1)

;; top level param
(def top-v-p (nt/sqrt 3))

(def init-bot
  (lv/map->Level {:phi u
                  :eps error-u
                  :sigma sigma-u
                  :theta theta       ; preserves h(phi)
                  :h  h
                  :h' h'
                  :phi-dt   dt
                  :eps-dt   dt
                  :sigma-dt slow-dt
                  :theta-dt 0}))

(def init-mid
  (lv/map->Level {:phi init-phi-v-p
                  :eps error-p
                  :sigma sigma-p
                  :theta theta       ; preserves h(phi)
                  :h  h
                  :h' h'
                  :phi-dt   dt
                  :eps-dt   dt
                  :sigma-dt slow-dt
                  :theta-dt 0}))

(def top (lv/map->Level {:phi top-v-p})) ; other fields will be nil

(def init-levels [init-bot init-mid top])

(def stages (iterate (partial lv/next-levels next-bottom) init-levels))
