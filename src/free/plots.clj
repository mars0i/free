(ns free.plots
  (use [incanter charts core]))

(defn plot-level
  "Plot phi, eps, sigma, and theta for all level records at level level-num
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
       (scatter-plot (range) (map :phi stages) :series-label "phi" :legend true)
       ;; For other vars, better to use lines (as in xy-plot):
       (add-lines (range) (map :eps   stages) :series-label "eps")
       (add-lines (range) (map :sigma stages) :series-label "sigma")
       (add-lines (range) (map :theta stages) :series-label "theta")
       (set-point-size 1) ; applies to points, not lines. apparently only applies to the first series.
       (set-stroke-color java.awt.Color/black :dataset 0) ; phi. For xy-plot and add-lines use :dataset instead of :series.This is a bug in Incanter. See issue #233 in Incanter repo.
       (set-stroke-color java.awt.Color/red   :dataset 1) ; eps. 
       (set-stroke-color java.awt.Color/green :dataset 2) ; sigma
       (set-stroke-color java.awt.Color/blue  :dataset 3) ; theta
       (view :width 800 :height 600))))

