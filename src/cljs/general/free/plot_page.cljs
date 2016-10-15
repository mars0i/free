;; This software is copyright 2016 by Marshall Abrams, and is distributed
;; under the Gnu General Public License version 3.0 as specified in the
;; the file LICENSE.

(ns free.plot-page
  (:require [cljs.pprint :as pp]
            [cljs.spec :as s]
            [reagent.core :as r]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [accountant.core :as accountant]
            [goog.string :as gs]
            [free.example-5 :as e]
            [cljsjs.d3]       ; aliases unused but included
            [cljsjs.nvd3])) ; in case Clojurescript likes 'em

;; -------------------------
;; globals

;; Default simulation parameters

(def svg-height 500)
(def svg-width 1100)
(def num-points 500) ; approx number of points to be sampled from data to be plotted

(def chart-svg-id "chart-svg")
(def default-input-color "#000000")
(def error-color   "#FF0000")

(def copyright-sym (gs/unescapeEntities "&copy;")) 
(def nbsp (gs/unescapeEntities "&nbsp;")) 

(def form-labels {:ready-label "re-run" 
                  :running-label "running..." 
                  :error-text [:text "One or more values in red are illegal." 
                                nbsp "See " [:em "parameters"] " on the information page"]})


;; THIS is intentionally not defonce or a ratom.  I want it to be
;; revisable by reloading to model file and this file.
(def raw-stages (e/make-stages))

(def num-levels (dec (count (first raw-stages)))) ; don't count top level as a level

(defonce chart-params$ (r/atom {:timesteps 100000
                                :levels-to-display (apply sorted-set 
                                                          (rest (range num-levels)))})) ; defaults to all levels but first

(defonce default-chart-param-colors (zipmap (keys @chart-params$) 
                                            (repeat default-input-color)))

(defonce chart-param-colors$ (r/atom default-chart-param-colors))

(defonce no-error-text [:text])
(defonce error-text$ (r/atom no-error-text))

;; -------------------------
;; spec

(defn explain-data-problem-keys
  "Given the result of a call to spec/explain-data, returns the keys of 
  the tests that failed."
  [data]
  (mapcat :path 
          (:cljs.spec/problems data)))

;(defn ge-le [inf sup] (s/and #(>= % inf) #(<= % sup)))
;(s/def ::max-r (ge-le 0.0 1.0))

(s/def ::timesteps (s/and integer? pos?))

;(s/def ::chart-params (s/keys :req-un [::max-r ::s ::h ::x1 ::x2 ::x3 ::x-freqs ::B-freqs]))
(s/def ::chart-params (s/keys :req-un [::timesteps])) ; require these keys (with single colon), and check that they conform

;; -------------------------
;; run simulations, generate chart

(defn calc-every-nth
  "Calculate how often to sample stages to generate a certain number of points."
  [timesteps]
  (long (/ timesteps num-points)))

;; transducer version
(defn sample-stages
  "samples stages from raw stages sequence."
  [raw-stages timesteps every-nth]
  (sequence (comp (take (+ every-nth timesteps)) ; rounds up
                  (take-nth every-nth))
                  raw-stages))

;; traditional version
;(defn sample-stages
;  [raw-stages timesteps every-nth]
;  (take-nth every-nth 
;            (take (+ every-nth timesteps) ; round up
;                  raw-stages)))

(defn xy-pairs
  [ys]
  (map #(hash-map :x %1 :y %2)
       (range)
       ys))

;; The first entry will be turned into dots rather than a line using voodoo CSS I stuck at the end of site.css.
;; Got that from http://stackoverflow.com/questions/27892806/css-styling-of-points-in-figure and a bunch of trial and error.
(defn make-bottom-chart-data
  [level-stages]
  [{:key "sensory input"   :values (xy-pairs (map :phi level-stages))     :color "#606060" :area false :fillOpacity -1}
   {:key "sensory epsilon" :values (xy-pairs (map :epsilon level-stages)) :color "#ffd0e0" :area false :fillOpacity -1}])

(defn make-level-chart-data
  [level-stages]
  [{:key "phi"     :values (xy-pairs (map :phi level-stages))     :color "#000000" :area false :fillOpacity -1}
   {:key "epsilon" :values (xy-pairs (map :epsilon level-stages)) :color "#ff0000" :area false :fillOpacity -1}
   {:key "sigma"   :values (xy-pairs (map :sigma level-stages))   :color "#00ff00" :area false :fillOpacity -1}
   {:key "theta"   :values (xy-pairs (map :theta  level-stages))   :color "#0000ff" :area false :fillOpacity -1}])

(defn make-chart-data
  "Make NVD3 chart configuration data object."
  [stages params$]
  (clj->js (vec (mapcat (fn [level-num] ((if (= level-num 0)      ; choose function to apply
                                           make-bottom-chart-data ; bottom level gets special handling
                                           make-level-chart-data) ; other levels are generic 
                                         (map #(nth % level-num) stages))) ; select data for this level
                        (:levels-to-display @params$)))))


(defn make-chart
  [raw-stages svg-id params$]
  "Create an NVD3 line chart with configuration parameters in @params$
  and attach it to SVG object with id svg-id."
  (let [chart (.lineChart js/nv.models)
        ;chart (.multiChart js/nv.models)
        timesteps (:timesteps @params$)
        every-nth (calc-every-nth timesteps)
        sampled-stages (sample-stages raw-stages timesteps every-nth)]
    ;; configure nvd3 chart:
    (-> chart
        (.height svg-height)
        (.width svg-width)
        ;(.margin {:left 100}) ; what does this do?
        (.useInteractiveGuideline true)
        (.duration 200) ; how long is gradual transition from old to new plot
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
        (datum (make-chart-data sampled-stages params$))
        (call chart))
    chart)) 


;; -------------------------
;; form: set chart parameters, re-run simulations and chart

(defn spaces 
  "Returns a text element containing n nbsp;'s."
  [n]
  (into [:text] (repeat n nbsp)))

;; a "form-2" component function: returns a function rather than hiccup (https://github.com/Day8/re-frame/wiki/Creating-Reagent-Components).
(defn chart-button
  "Create submit button runs validation tests on form inputs and changes 
  its appearance to indicate that the simulations are running.  svg-id is
  is of SVG object to which the chart will be attached.  params$ is an atom
  containing a chart parameter map.  colors$ is an atom containing the text
  colors for each of the inputs in the form.  labels is a map containing
  three labels for the button, indicating ready to run, running, or bad inputs."
  [svg-id params$ colors$ labels]
  (let [{:keys [ready-label running-label error-text]} labels
        button-label$ (r/atom ready-label)] ; runs only once
    (fn [svg-id params$ colors$ _]   ; called repeatedly. (already have labels from the let)
      [:button {:type "button" 
                :id "chart-button"
                :on-click (fn []
                            (reset! colors$ default-chart-param-colors) ; alway reset colors--even if persisting bad inputs, others may have been corrected
                            (reset! error-text$ no-error-text)
                            (if-let [spec-data (s/explain-data ::chart-params @params$)] ; if bad inputs (nil if ok)
                              (do
                                (reset! error-text$ error-text)
                                (doseq [k (explain-data-problem-keys spec-data)] ; NOTE this function must change with new Clojurescript release
                                  (swap! colors$ assoc k error-color)))
                              (do
                                (reset! button-label$ running-label)
                                (js/setTimeout (fn [] ; allow DOM update b4 make-chart runs
                                                 (make-chart raw-stages svg-id params$) 
                                                 (reset! button-label$ ready-label))
                                               100))))}
       @button-label$])))


(defn level-checkbox
  [level-num params$]
  (let [levels-to-display (:levels-to-display @params$)
        checked (boolean (levels-to-display level-num))] ; l-t-d is a set, btw
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


;; For comparison, in lescent, I used d3 to set the onchange of dropdowns to a function that set a single global var for each.
(defn float-input 
  "Create a text input that accepts numbers.  k is keyword to be used to extract
  a default value from params$, and to be passed to swap! assoc.  It will also 
  be converted to a string an set as the id and name properties of the input 
  element.  This string will also be used as the name of the variable in the label,
  unless var-label is present, in which it will be used for that purpose."
  ([k params$ colors$ size label] (float-input k params$ colors$ size label [:em (name k)]))
  ([k params$ colors$ size label & var-label]
   (let [id (name k)
         old-val (k @params$)]
     [:span {:id (str id "-span")}
      (vec (concat [:text label " "] var-label [" : "]))
      [:input {:id id
               :name id
               :type "text"
               :style {:color (k @colors$)}
               :size size
               :defaultValue old-val
               :on-change #(swap! params$ assoc k (js/parseFloat (-> % .-target .-value)))}]
      [spaces 4]])))

(defn float-text
  "Display a number with a label so that size is similar to float inputs."
  [n & label]
  (vec (concat [:text] label [": "]
               (list [:span {:style {:font-size "12px"}} 
                      (pp/cl-format nil "~,4f" n)]))))

(defn chart-params-form
  "Create form to allow changing model parameters and creating a new chart."
  [svg-id params$ colors$]
  (let [float-width 7
        int-width 10
        {:keys [x1 x2 x3]} @params$]  ; seems ok: entire form re-rendered(?)
    [:form 
     [chart-button svg-id params$ colors$ form-labels]
     [spaces 4]
     [float-input :timesteps params$ colors$ int-width ""]
     [spaces 4]
     [level-checkboxes params$]
     [:span {:id "error-text" 
            :style {:color error-color :font-size "16px" :font-weight "normal" :text-align "left"}} ; TODO move styles into css file?
       @error-text$]]))

(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:script {:type "text/javascript" :src "js/compiled/linkage.js"}]])

(defn home-render []
  "Set up main chart page (except for chart)."
  (head)
   [:div {:id "chart-div"}
    [:br]
    [chart-params-form (str "#" chart-svg-id) chart-params$ chart-param-colors$]
    [:svg {:id chart-svg-id :height (str svg-height "px")}]])

(defn home-did-mount [this]
  "Add initial chart to main page."
  (make-chart raw-stages (str "#" chart-svg-id) chart-params$))

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
