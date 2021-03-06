;; This software is copyright 2016 by Marshall Abrams, and is distributed
;; under the Gnu General Public License version 3.0 as specified in the
;; the file LICENSE.

(ns free.plot-page
  (:require [cljs.pprint :as pp]
            [cljs.spec :as s]
            [cljs.reader :as cr]
            [reagent.core :as r]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [accountant.core :as accountant]
            [goog.string :as gs]
            [free.model :as m]
            [free.level :as l] ; for spec
            [cljsjs.d3]       ; aliases unused but included
            [cljsjs.nvd3])) ; in case Clojurescript likes 'em

;; -------------------------
;; globals

;; Default simulation parameters

(def initial-height 500)
(def initial-width 1200)
(def initial-timesteps 6000)
(def initial-num-points 1000) ; approx number of points to be sampled from data to be plotted

(def chart-svg-id "chart-svg")
(def default-input-color "#000000")
(def error-color   "#FF0000")

(def copyright-sym (gs/unescapeEntities "&copy;")) 
(def nbsp (gs/unescapeEntities "&nbsp;")) 

(def plot-button-labels {:ready-label "re-plot" 
                         :running-label "plotting ..." 
                         :error-text [:text "Values in red are illegal." ]})

(def run-button-labels {:ready-label "re-run" 
                        :running-label "running ..." 
                        :error-text [:text "Values in red are illegal." ]})

(def num-levels (dec (count m/first-stage))) ; don't count top level as a level

(defonce chart-params$ (r/atom {:height initial-height
                                :width  initial-width
                                :timesteps initial-timesteps
                                :num-points initial-num-points
                                :levels-to-display (apply sorted-set 
                                                          (rest (range num-levels)))})) ; defaults to all levels but first
;; NOTE code in make-chart assumes that if 0 is in levels-display, indicating
;; that level 0 should be displayed, the 0 must be first so that its points
;; will be plotted first (in which case nvd3 will name its group "nv-series-0".

(defonce level-params (map r/atom m/first-stage)) ; no $ on end because it's not a ratom; it's a sequence of ratoms
;; NOTE we also use m/other-model-params below

(defonce default-chart-param-colors (zipmap (keys @chart-params$) 
                                            (repeat default-input-color)))

(defonce chart-param-colors$ (r/atom default-chart-param-colors))

(defonce no-error-text [:text])
(defonce error-text$ (r/atom no-error-text))

;; THIS is intentionally not defonce.  I want it to be
;; revisable by reloading to model file and this file.
(def raw-stages$ (r/atom 
		   (m/make-stages (map deref level-params)
		                  (m/make-next-bottom m/other-model-params))))

;; -------------------------
;; spec

(defn explain-data-problem-keys
  "Given the result of a call to spec/explain-data, returns the keys of 
  the tests that failed."
  [data]
  (mapcat :path 
          (:cljs.spec/problems data)))

(s/def ::height pos-int?)
(s/def ::width pos-int?)
(s/def ::num-points pos-int?)
(s/def ::timesteps pos-int?)

;; spec tests to be run by plot button
(s/def ::plot-params (s/keys :req-un [::height ; require these keys (with single colon), and check that they conform
                                       ::width
                                       ::num-points
                                       ::timesteps]))

;; spec tests to be run by run button
(s/def ::run-params (s/merge ::plot-params :l/level-params :m/other-params))

;; -------------------------
;; run simulations and construct data for chart

(defn calc-every-nth
  "Calculate how often to sample stages to generate a certain number of points."
  [params$]
  (let [{:keys [num-points timesteps]} @params$]
    (max 1 (long (/ timesteps num-points)))))

;; transducer version
(defn sample-stages
  "samples stages from raw stages sequence."
  [raw-stages$ timesteps every-nth]
  (sequence (comp (take (+ every-nth timesteps)) ; rounds up
                  (take-nth every-nth))
                  @raw-stages$))

;; traditional version
;(defn sample-stages
;  [raw-stages$ timesteps every-nth]
;  (take-nth every-nth 
;            (take (+ every-nth timesteps) ; round up
;                  raw-stages$)))

(defn xy-pairs
  [ys]
  (map #(hash-map :x %1 :y %2)
       (range)
       ys))

;; -------------------------
;; build chart

;; NOTE code in make-chart assumes that if level 0 exists, it comes first 
;; in the output of this function, and its phi data is first in that.
(defn make-level-chart-data
  [stages level-num]
  (let [level-stages (map #(nth % level-num) stages)]
    (if (= level-num 0) ;; level 0, the sensory input level, get special handling
      [{:key "sensory input"   :values (xy-pairs (map :phi level-stages))     :color "#606060"}
       {:key "sensory epsilon" :values (xy-pairs (map :epsilon level-stages)) :color "#ffc0d0"}]
      [{:key (str "phi "     level-num) :values (xy-pairs (map :phi level-stages))     :color "#000000"}
       {:key (str "epsilon " level-num) :values (xy-pairs (map :epsilon level-stages)) :color "#ff0000"}
       {:key (str "sigma "   level-num) :values (xy-pairs (map :sigma level-stages))   :color "#00ff00"}
       {:key (str "theta "   level-num) :values (xy-pairs (map :theta  level-stages))  :color "#0000ff"}])))


;; NOTE code in make-chart assumes that if level 0 exists, it comes first 
;; in the output of this function, and its phi data is first in that.
(defn make-chart-data
  "Make NVD3 chart configuration data object."
  [stages params$]
  (clj->js (vec (mapcat (partial make-level-chart-data stages)
                        (:levels-to-display @params$)))))

(defn make-chart
  [raw-stages$ svg-id params$]
  "Create an NVD3 line chart with configuration parameters in @params$
  and attach it to SVG object with id svg-id."
  (let [chart (.lineChart js/nv.models)
        every-nth (calc-every-nth params$)
        sampled-stages (sample-stages raw-stages$ (:timesteps @params$) every-nth)]
    ;; configure nvd3 chart:
    (-> chart
        (.height (:height @params$))
        (.width (:width @params$))
        ;(.margin {:left 100}) ; what does this do?
        (.useInteractiveGuideline true)
        (.duration 500) ; how long is gradual transition from old to new plot
        (.pointSize 1)
        (.showLegend true)
        (.showXAxis true)
        (.showYAxis true)) ; force y-axis to go to 1 even if data doesn't
    (-> chart.xAxis
        (.axisLabel "timesteps")
        (.tickFormat (fn [d] (pp/cl-format nil "~:d" (* every-nth d)))))
    (-> chart.yAxis
        (.tickFormat (fn [d] (pp/cl-format nil "~,3f" d))))
    ;; add chart to dom using d3:
    (.. js/d3
        (select svg-id)
        (style "height" (:height @params$)) ; because I've disabled Reagent control of svg height in home-render
        (datum (make-chart-data sampled-stages params$))
        (call chart))
    ;; If we are displaying level 0, then its phi should be the first set of 
    ;; points displayed, which nvd3 puts in a group called "nv-series-0".
    ;; In this case, turn off the line and just leave its points.  This depends
    ;; on some CSS set in site.css in addition to this CSS here (because I can't
    ;; get it to work here--not sure why).
    (when (= 0 (first (:levels-to-display @params$)))
      (.. js/d3
          (select "#chart-svg g.nv-series-0 path.nv-line")
          (style (clj->js {:stroke-opacity 0}))))
    chart)) 


;; -------------------------
;; re-run model and remake chart

(defn run-model
  [stages$ svg-id params$]
  (reset! stages$ (m/make-stages (map deref level-params)
		                  (m/make-next-bottom m/other-model-params)))
  (make-chart stages$ svg-id params$))


;; -------------------------
;; general-purpose form elements

(defn spaces 
  "Returns a text element containing n nbsp;'s."
  [n]
  (into [:text] (repeat n nbsp)))

;; a "form-2" component function: returns a function rather than hiccup (https://github.com/Day8/re-frame/wiki/Creating-Reagent-Components).
(defn button
  "Create submit button runs validation tests on form inputs and changes 
  its appearance to indicate that the simulations are running.  svg-id is
  is of SVG object to which the chart will be attached.  params$ is an atom
  containing a chart parameter map.  colors$ is an atom containing the text
  colors for each of the inputs in the form.  labels is a map containing
  three labels for the button, indicating ready to run, running, or bad inputs."
  [svg-id params$ colors$ run-fn labels]
  (let [{:keys [ready-label running-label error-text]} labels
        button-label$ (r/atom ready-label)] ; runs only once
    (fn [svg-id params$ colors$ _]   ; called repeatedly. (already have labels from the let)
      [:button {:type "button" 
                :on-click (fn []
                            (reset! colors$ default-chart-param-colors) ; alway reset colors--even if persisting bad inputs, others may have been corrected
                            (reset! error-text$ no-error-text)
                            (if-let [spec-data (s/explain-data ::plot-params @params$)] ; if bad inputs (nil if ok)
                              (do
                                (reset! error-text$ error-text)
                                (doseq [k (explain-data-problem-keys spec-data)]
                                  (swap! colors$ assoc k error-color)))
                              (do
                                (reset! button-label$ running-label)
                                (js/setTimeout (fn [] ; allow DOM update b4 make-chart runs so I can change the button to show things are in progress
                                                 (run-fn raw-stages$ svg-id params$) 
                                                 (reset! button-label$ ready-label))
                                               100))))}
       @button-label$])))

(defn float-text
  "Display a number with a label so that size is similar to float inputs."
  [n & label]
  (vec (concat [:text] label [": "]
               (list [:span {:style {:font-size "12px"}} 
                      (pp/cl-format nil "~,4f" n)]))))

(defn input-fn-maker
  "Returns a function that will display and accept inputs, parsing them using parse-fn.
  k is keyword to be used to extract a default value from params$, and to be passed to 
  swap! and assoc.  It will also be converted to a string an set as the id and name 
  properties of the input element.  This string will also be used as the name of the 
  variable in the label, unless var-label is present, in which it will be used for 
  that purpose."
  [in-fn out-fn]
  (letfn [(input-fn
            ([params$ colors$ size k label] (input-fn params$ colors$ size k label [:em (name k)]))
            ([params$ colors$ size k label & var-label]
             (let [id (name k)
                   old-val (k @params$)]
               [:span {:id (str id "-span")}
                (vec (concat [:text label " "] var-label [" : "]))
                [:input {:id id
                         :name id
                         :type "text"
                         :style {:color (k @colors$)}
                         :size size
                         :defaultValue (out-fn old-val)
                         :on-change #(swap! params$ assoc k (in-fn (-> % .-target .-value)))}]
                [spaces 4]])))]
    input-fn))

(def float-input 
  "Create a text input that accepts numbers."
  (input-fn-maker js/parseFloat identity))

(defn param-float-input
  "Generates a form input for numeric data in a table data element."
  [colors$ params$ size k]
  (if (k @params$) 
    [:td (float-input params$ colors$ size k "")]
    [:td]))

;(def seq-input
;  "Create a text input that accepts vectors."
;  (input-fn-maker cr/read-string str))

(def seq-input
  "Create a text input that accepts vectors."
  (input-fn-maker #(cr/read-string (str "[" % "]")) ; wrap string with multiple numbers in brackets and make a vector
                  #(apply str (interpose \ %))))    ; stringify the space-separated elements of a collection

(defn param-seq-input
  "Generates a form input for Clojure sequential data in a table data element."
  [colors$ params$ size k]
  (if (k @params$) 
    [:td (seq-input params$ colors$ size k "")]
    [:td]))

(defn some-kind-of-input
 "Generates a form input whose properties depend on the type of the value of
 the key k in params$"
 [colors$ params$ size k]
 (when-not (= k :description) ; description labels the whole line; it's not an input value
  (let [val (k @params$)]
   (cond (number? val) (param-float-input colors$ params$ size k)
    :else (param-seq-input colors$ params$ size k)))))


;; -------------------------
;; chart-control portion of form

(defn level-checkbox
  [level-num params$]
  (let [checked (boolean ((:levels-to-display @params$) level-num))] ; l-t-d is a set, btw
    [[:text (str " " level-num ": ")]
     [:input {:type "checkbox"
              :id (str "level-" level-num)
              :checked checked
              :on-change #(swap! params$ 
                                 update :levels-to-display 
                                 (if checked disj conj) ; i.e. if checked, now unchecked, so remove level from set; else it's now checked, so add level
                                 level-num)}]]))

(defn level-checkboxes
  [params$]
  (vec 
    (concat
      [:span {:id "level-checkboxes"}
       [:text "Levels to display: "]]
      (mapcat 
        #(level-checkbox % params$)
        (range num-levels)))))

(defn chart-form-component
  "Create form elements for parameters controlling the appearance of the chart."
  [svg-id chart-params$ colors$]
  (let [int-width 10]
    [:span
     [button svg-id chart-params$ colors$ make-chart plot-button-labels]
     [:span {:id "error-text" :style {:color error-color :font-size "16px" :font-weight "normal" :text-align "left"}} nbsp nbsp @error-text$]
     [:br]
     [level-checkboxes chart-params$]
     [spaces 4]
     [float-input chart-params$ colors$ int-width :timesteps ""]
     [float-input chart-params$ colors$ int-width :width ""]
     [float-input chart-params$ colors$ int-width :height ""]
     [float-input chart-params$ colors$ int-width :num-points ""]]))


;; -------------------------
;; Level data part of form

(defn level-form-elems
  "Produces HTML table rows for a single free Level."
  [colors$ params$ other-params$ label]
  (let [float-width 7
        seq-width 12]
    [(when other-params$
       (conj 
         (into [:tr [:td (:description @other-params$)]] (map (partial some-kind-of-input colors$ other-params$ seq-width)
                                                         (keys @other-params$)))))
     (conj
       (into [:tr [:td label]]
             (map (partial param-float-input colors$ params$ float-width) 
                  [:phi :epsilon :sigma :theta :phi-dt :epsilon-dt :sigma-dt :theta-dt]))
       [:td {:col-span "20"}]) ; add filler td to match however many td's there are in other rows
     [:tr {:class "bottom-border"} [:td {:col-span "20"}]]])) ; add filler td to match however many td's there are in other rows; use colspan larger than any number of columns we'd want

(defn model-form-component
  "Produces an HTML table containing form elements for a series of levels, 
  with params containing a sequence of ratoms containing level parameters,
  and other-params containing a sequence of ratoms (or nils)  containing
  other parameters to be used at that level.  (Note that the result will be
  form elements for parameters with the same names different levels, and 
  maybe the same HTML id's.  This works because input-fn-maker associates
  each field with particular ratom; different levels have different ratoms--
  from the sequences just mentioned--so by the time you click a button to
  use the data you've entered, Reagent has already updated the value in
  the relevant ratom, and the button just causes that data to be used
  in a particular way.)"
  [svg-id params other-params colors$]
  [:span
   [button svg-id chart-params$ colors$ run-model run-button-labels]
   [:br]
   [:table (into [:tbody [:tr [:td {:col-span "20"}]]] ; use colspan larger than any number of columns we'd want
                 (mapcat (partial level-form-elems colors$) params other-params 
                         (conj ; make level labels, but call the top level "priors":
                           (vec (map #(str "level " % ":")
                                     (range (dec (count params)))))
                           "priors:")))]])

;; -------------------------
;; build form

(defn params-form
  "Create form to allow changing model parameters and creating a new chart."
  [svg-id chart-params$ level-params other-params colors$]
    [:form 
     [chart-form-component svg-id chart-params$ colors$]               ; part of the form that controls chart appearance
     [:hr {:class "align-left" :width (:width @chart-params$)}]        ; divide the parts
     [model-form-component svg-id level-params other-params colors$]]) ; part of the form that controls the simulation

;; -------------------------
;; build page

(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:script {:type "text/javascript" :src "js/compiled/linkage.js"}]])

;; a "form-2" component function: returns a function rather than hiccup (https://github.com/Day8/re-frame/wiki/Creating-Reagent-Components).
;; Only reason to do this here is so that svg height set only once here--not controlled by reagent, which makes size change while editing field.
;; This means have to resize svg object by hand in make-chart.
(defn home-render []
  "Set up main chart page (except for chart)."
  (head)
  (let [svg-height (str (:height @chart-params$) "px")] ; store initial svg height permanently (until changed elsewhere)
    (fn []
      [:div {:id "chart-div"}
       [:svg {:id chart-svg-id :height svg-height}]
       [params-form (str "#" chart-svg-id)
                          chart-params$
                          level-params
                          m/other-model-params 
                          chart-param-colors$]])))


(defn home-did-mount [this]
  "Add initial chart to main page."
  (make-chart raw-stages$ (str "#" chart-svg-id) chart-params$))

(defn home-page []
  (r/create-class {:reagent-render home-render
                   :component-did-mount home-did-mount}))

;; -------------------------
;; Initialize app

(defn mount-root []
  (r/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (accountant/configure-navigation!
    {:nav-handler
     (fn [path]
       (secretary/dispatch! path))
     :path-exists?
     (fn [path]
       (secretary/locate-route path))})
  (accountant/dispatch-current!)
  (mount-root))

(init!)

;; ----------------------------

;; From simple figwheel template:
;; optionally touch your app-state to force rerendering depending on
;; your application
;; (swap! app-state update-in [:__figwheel_counter] inc)
(defn on-js-reload [])
