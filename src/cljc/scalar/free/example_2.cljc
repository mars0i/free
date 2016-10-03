;; scalar example with fluctuations

(ns free.example-2
  (:require [free.level :as lvl]
            [free.random :as ran])) ; will be clj or cljs depending on dialect

#?(:clj (require '[clojure.math.numeric-tower :as nt]))

;; Generative function phi^2:
(defn gen  [phi] (* phi phi)) ;; or: (lvl/m-square phi)
(defn gen' [phi] (* phi 2.0))   ;; or: (ar/m* phi 2))

;; Alternative generative function phi^8:
;(defn gen  [phi] (nt/expt phi 8))
;(defn gen' [phi] (* 8.0 (nt/expt phi 7.0))) 

(def init-gen-wt 1) ; i.e. initially pass value of gen(phi) through unchanged

;; simple next-bottom function
;(def next-bottom (lvl/make-next-bottom #(ran/next-gaussian 2 5)))

;; experimental next-bottom function
(def ticks-between 50000)
(def top-tick 50000)
(def tick$ (atom 0))
(def mean$ (atom 6))
(def sd$ (atom 3))
;; Note that since the generative function is exponential, it's
;; potentially problematic to make the mean negative.
;; TODO I really ought to get rid of this atom stuff and do it instead
;; with args passed along.  Maybe.
(def next-bottom (lvl/make-next-bottom 
                   (fn []
                     (swap! tick$ inc)
                     (when (and
                             ;(< @tick$ top-tick)
                             (= 0 (mod @tick$ ticks-between)))
                       ;(println (swap! mean$ #(+ % (+ -1.5 (* 3 (ran/next-double))))))
                       (println 
                         (swap! mean$ 
                                #(let [new-mean (+ % ; symmetric random walk
                                                   (if (< (ran/next-double) 0.5)
                                                     1
                                                     -1))]
                                   (if (pos? new-mean) new-mean 1))))) ; clipped below 1
                     (ran/next-gaussian @mean$ @sd$))))

(def sigma-u 2) ; controls degree of fluctuation in phi at level 1
(def error-u 0) ; err
;; Note that the bottom-level phi needs to be an arbitrary number so that 
;; err-inc doesn't NPE on the first tick, but the number doesn't matter, and 
;; it will immediately replaced when next-bottom is run.

;; middle level params
(def v-p 3) ; what phi is initialized to, and prior mean at top
(def sigma-p 2) ; controls how close to true value at level 1
(def error-p 0)

(def bot-map {:phi 0 ; needs a number for err-inc on 1st tick; immediately replaced by next-bottom
              :err error-u
              :sigma sigma-u
              :gen-wt init-gen-wt
              :gen  nil ; unused at bottom since err update uses higher gen
              :gen' nil ; unused at bottom since phi comes from outside
              :phi-dt 0.01
              :err-dt 0.01
              :sigma-dt 0.0
              :gen-wt-dt 0.0})

(def mid-map {:phi v-p
              :err error-p
              :sigma sigma-p
              :gen-wt init-gen-wt
              :gen  gen  ; used to calc error at next level down, i.e. err
              :gen' gen' ; used to update phi at this level
              :phi-dt 0.0001
              :err-dt 0.01
              :sigma-dt 0.0001
              :gen-wt-dt 0.000001})

(def init-bot (lvl/map->Level bot-map))
;; mid-level state with adjustable gen-wt:
(def init-mid (lvl/map->Level mid-map))
;; alt mid-level state with fixed gen-wt:
(def init-mid-fixed-gen-wt (lvl/map->Level (assoc mid-map :gen-wt-dt 0.0)))
(def top (lvl/make-top-level v-p)) ; will have phi, and identity as :gen ; other fields will be nil

;(def stages (iterate (partial lvl/next-levels next-bottom) [init-bot init-mid top]))
(defn make-stages [] (iterate (partial lvl/next-levels next-bottom)
                              [init-bot init-mid top]))

;; e.g.
;;(plot-level (make-stages)) 1 1000000 1000) ; uses regular sequence ops
;; with transducer:
;;(plot-level (sequence (comp (take 1000000) (take-nth 1000)) (make-stages)) 1)
;;(plot-level (into []  (comp (take 1000000) (take-nth 1000)) (make-stages)) 1)
;; or even more efficient:
;; (plot-level (sequence (comp (take 1000000000) (take-nth 10000) (map #(nth % 1))) (make-stages)))
;; (plot-level (into []  (comp (take 1000000000) (take-nth 10000) (map #(nth % 1))) (make-stages)))
;; Note however that at present plot-level has to realize the entire sequence passed in.
