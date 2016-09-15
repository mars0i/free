
;; By Carlos/"carlitos" from 
;; https://groups.google.com/forum/#!msg/incanter/kx-fQ9heB80/Zj_W41KzPMcJ

(ns utils.viewclose
  (require [incanter.core])
  (import [org.jfree.chart.ChartPanel]
          [java.awt.BorderLayout]
          [javax.swing JPanel BoxLayout JFrame]))

(defn chart? [obj]
  (= (class obj) org.jfree.chart.JFreeChart))

(def windows-charts (atom #{}))
 
(def windows-datasets (atom #{}))

(defn view*
  "Equivalent to view, but a reference to charts and datasets is kept 
  so they can be closed easily using close-charts/close-datasets/close-all.
  For charts, if a second parameter is given the chart is added to that panel."
  ([obj pane]
   (if (chart? obj)
     (do (.add pane (ChartPanel. obj))
         (.revalidate (.getParent pane)))
     (view* obj)))
  ([obj]
   (let [window (incanter.core/view obj)]
     (cond  
       (chart? obj) (swap! windows-charts conj window)
       (incanter.core/dataset? obj) (swap! windows-datasets conj window))
     window)))

(defn make-panel
  "Create a strip, horizontal or vertical, where charts can be added using view*."
  ([] (make-panel false))
  ([vertical?]
   (let [pane (JPanel.)
         layout (BoxLayout. pane (if vertical?
                                   BoxLayout/Y_AXIS
                                   BoxLayout/X_AXIS))]
     (.setLayout pane layout)
     (swap! windows-charts conj 
            (doto (JFrame.)
              (.setVisible true)
              (.resize 400 400)
              (.add pane BorderLayout/CENTER)))
     pane)))

(defn close-charts []
  (doseq [window @windows-charts] (.dispose window))
  (reset! windows-charts #{}))

(defn close-datasets []
  (doseq [window @windows-datasets] (.dispose window))
  (reset! windows-datasets #{}))

(defn close-all []
  (close-charts)
  (close-datasets))
