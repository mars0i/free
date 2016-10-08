;; This software is copyright 2016 by Marshall Abrams, and is distributed
;; under the Gnu General Public License version 3.0 as specified in the
;; the file LICENSE.

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
