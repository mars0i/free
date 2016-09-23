(ns free.plots
  (require [clojure.core.matrix :as mx]
           [incanter.charts :as ch]
           [incanter.core :as co]))

(defn plot-level
  "Plot phi, err, sigma, and gen-wt for all level records at level level-num
  in sequence stages.  If n is provided, take only n elements from stages.
  If every is also provided, use only the elements that are every steps apart.
  Level 0 is the first level.  If level-num is not provided, assumes that stages
  is a sequence consisting only of entities at that level.  
  NOTE: Assumes all values are scalars.  Returns the plot object."
  ([stages level-num n every]
   (plot-level (take-nth every (take n stages)) level-num))
  ([stages level-num n]
   (plot-level (take n stages) level-num))
  ([stages level-num]
   (plot-level (map #(nth % level-num) stages)))
  ([stages]
     (doto 
       ;; Need scatter plot for phi for the sake of level 0, where inputs are noisy
       (ch/scatter-plot (range) (map :phi stages) :series-label "phi" :legend true)
       ;; For other vars, better to use lines (as in xy-plot):
       (ch/add-lines (range) (map :err   stages) :series-label "err")
       (ch/add-lines (range) (map :sigma stages) :series-label "sigma")
       (ch/add-lines (range) (map :gen-wt stages) :series-label "gen-wt")
       (ch/set-point-size 1) ; applies to points, not lines. apparently only applies to the first series.
       (ch/set-stroke-color java.awt.Color/black :dataset 0) ; phi. For xy-plot and add-lines use :dataset instead of :series.This is a bug in Incanter. See issue #233 in Incanter repo.
       (ch/set-stroke-color java.awt.Color/red   :dataset 1) ; err. 
       (ch/set-stroke-color java.awt.Color/green :dataset 2) ; sigma
       (ch/set-stroke-color java.awt.Color/blue  :dataset 3) ; gen-wt
       (co/view :width 800 :height 600))))


(def phi-base-color    (java.awt.Color. 0   0   0))
(def err-base-color    (java.awt.Color. 255 0   0))
(def sigma-base-color  (java.awt.Color. 0   0   255))
(def gen-wt-base-color (java.awt.Color. 0   255 0))
;; Can use java.awt.Color.brighter() and .darker() to get 5-10 variations:

;; wrap Java methods in functions so they can be passed:
(defn brighter [color] (.brighter color))
(defn darker   [color] (.darker color))


(defn plot-param-stages
  "Plot the stages for a single parameter--phi, err, etc."
  [chart base-color color-inc first-line-num plot-fn param-stages]
  (let [idxs-seq (mx/index-seq (first param-stages))
        num-idxs (count idxs-seq)
        last-line-num (+ first-line-num num-idxs)]
    (doseq [[idxs color line-num] (map vector 
                                       idxs-seq 
                                       (iterate color-inc base-color) ; seq of similar but diff colors
                                       (range first-line-num last-line-num))]
      (plot-fn chart (range) (map #(apply mx/mget % idxs) param-stages))
      (when color
        (ch/set-stroke-color chart color :dataset line-num))
      (ch/set-point-size chart 1 :dataset line-num)) ; used only for points; ignored for lines
    last-line-num))


;; TODO add series-labels and fix colors

(defn plot-level*
  "plot-level for vectors and matrices."
  ([stages level-num n every]
   (plot-level (take-nth every (take n stages)) level-num))
  ([stages level-num n]
   (plot-level (take n stages) level-num))
  ([stages level-num]
   ;; Uses undocumented "*" function versions of Incanter chart macros:
   (let [stages-level (map #(nth % level-num) stages)
         chart (ch/scatter-plot)
         first-plot-fn  (if (== 0 level-num) ch/add-points* ch/add-lines*) ; level-0 phi is sensory data, need points since less regular
         phi-color      (if (== 0 level-num) nil phi-base-color) ; let Incanter set different color for each dataset
         ;; Using identity to not adjust colors within category, but might later:
         line-num (plot-param-stages chart phi-color         identity 1        first-plot-fn (map :phi stages-level)) ; plot line numbering is 1-based
         line-num (plot-param-stages chart err-base-color    identity line-num ch/add-lines* (map :err stages-level))
         line-num (plot-param-stages chart sigma-base-color  identity line-num ch/add-lines* (map :sigma stages-level))]
     (plot-param-stages              chart gen-wt-base-color identity line-num ch/add-lines* (map :gen-wt stages-level))
     (co/view chart :width 800 :height 600)
     chart)))
