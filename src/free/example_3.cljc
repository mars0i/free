;; simple vector/matrix example

(ns free.example-3
  (:require [free.config :as conf]
            [free.matrix-arithmetic :as ar]
            [clojure.core.matrix :as mx]
            [free.dists :as pd])) ; clj or cljs depending on dialect

;; Use the matrix version of free.level:
(reset! conf/use-core-matrix$ true)
(require '[free.level :as lvl])

;; FIXME not right.  These should return vectors.
(defn gen  [phi] (lvl/m-square phi))
(defn gen' [phi] (ar/m* phi 2.0))
(def init-gen-wt (ar/make-identity-obj 2)) ; i.e. initially pass value of gen(phi) through unchanged

(def next-bottom (lvl/make-next-bottom 
                   #(mx/matrix [(pd/next-gaussian 2 5)
                                (pd/next-gaussian -1 3)])))

;; FIXME
;; WHY ARE phi and err turning into 2D matrices?? s/b a vector.
;; MAYBE MAKE THE VECTORS INTO 1x2 ROW OR COLUMN MATRICES (which?)
;; (Does this make aljabr happy, too?)

; what phi is initialized to, and prior mean at top:
(def v-p (mx/matrix [3.0 3.0]))

(def bot-map {:phi [0 0] ; immediately replaced by next-bottom
              :err   (mx/matrix [0.0 0.0])
              :sigma (mx/matrix [[2.0  0.25]
                                 [0.25 2.0]])
              :gen-wt init-gen-wt
              :gen  nil
              :gen' nil
              :phi-dt    (mx/matrix [0.01 0.01])
              :err-dt    (mx/matrix [0.01 0.01])
              :sigma-dt  (mx/matrix [0.0 0.0])
              :gen-wt-dt (mx/matrix [0.0 0.0])})

(def mid-map {:phi v-p
              :err   (mx/matrix [0.0 0.0])
              :sigma (mx/matrix [[2.0  0.25]
                                 [0.25 2.0]])
              :gen-wt init-gen-wt
              :gen  gen
              :gen' gen'
              :phi-dt    (mx/matrix [0.0001 0.0001])
              :err-dt    (mx/matrix [0.01   0.01])
              :sigma-dt  (mx/matrix [0.0001 0.0001])
              :gen-wt-dt (mx/matrix [0.01   0.01])})

(def init-bot (lvl/map->Level bot-map))
(def init-mid (lvl/map->Level mid-map))
(def top      (lvl/make-top-level v-p))

(defn make-stages [] (iterate (partial lvl/next-levels next-bottom)
                              [init-bot init-mid top]))
