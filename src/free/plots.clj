(ns free.plots
  (use [incanter charts core]))

(defn plot-level
  "Plot phi, eps, sigma, and theta for all level records at level level-num
  in (finite) stages sequence.  Level 0 is the first level.  Returns the plot 
  object."
  [level-num stages]
  (let [level-stages (map #(nth % level-num) stages)]
    (doto 
      ;; Need scatter plot for phi for the sake of level 0, where inputs are noisy
      (scatter-plot (range) (map :phi level-stages) :series-label "phi" :legend true)
      ;; For other vars, better to use lines (as in xy-plot):
      (add-lines (range) (map :eps   level-stages) :series-label "eps")
      (add-lines (range) (map :sigma level-stages) :series-label "sigma")
      (add-lines (range) (map :theta level-stages) :series-label "theta")
      (set-point-size 1) ; applies to points, not lines. apparently only applies to the first series.
      (set-stroke-color java.awt.Color/black :dataset 0) ; phi. For xy-plot and add-lines use :dataset instead of :series.This is a bug in Incanter. See issue #233 in Incanter repo.
      (set-stroke-color java.awt.Color/red   :dataset 1) ; eps. 
      (set-stroke-color java.awt.Color/green :dataset 2) ; sigma
      (set-stroke-color java.awt.Color/blue  :dataset 3) ; theta
      (view))))

