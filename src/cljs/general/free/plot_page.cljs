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
            [free.example-5 :as e5]
            [cljsjs.d3]       ; aliases unused but included
            [cljsjs.nvd3])) ; in case Clojurescript likes 'em

;; -------------------------
;; globals

;; Default simulation parameters
(defonce chart-params$ (r/atom {:timesteps 100000}))

(def svg-height 400)
(def svg-width 1000)
(def num-points 300) ; approx number of points to be sampled from data to be plotted

(def chart-svg-id "chart-svg")
(def default-input-color "#000000")
(def error-color   "#FF0000")

(def copyright-sym (gs/unescapeEntities "&copy;")) 
(def nbsp (gs/unescapeEntities "&nbsp;")) 

(def form-labels {:ready-label "re-run" 
                  :running-label "running..." 
                  :error-text [:text "One or more values in red are illegal." 
                                nbsp "See " [:em "Parameter ranges"] " on the More Information page"]})

(defonce default-chart-param-colors (zipmap (keys @chart-params$) 
                                            (repeat default-input-color)))

(defonce chart-param-colors$ (r/atom default-chart-param-colors))

(defonce no-error-text [:text])
(defonce error-text$ (r/atom no-error-text))

(def raw-data (e5/make-stages)) ; REPLACE THIS

;; -------------------------
;; spec

;; FIXME
(defn explain-data-problem-keys
  "Given the result of a call to spec/explain-data, returns the keys of 
  the tests that failed.  WARNING: This is for 
  Clojurescript 1.9.89/Clojure 1.9.0-alpha7.  It will have to be changed 
  to work with Clojure 1.9.0-alpha10."
  [data]
  (map first 
       (keys
         (:cljs.spec/problems data))))

(defn ge-le [inf sup] (s/and #(>= % inf) #(<= % sup)))
(defn ge-lt [inf sup] (s/and #(>= % inf) #(<  % sup)))
(defn gt-le [inf sup] (s/and #(>  % inf) #(<= % sup)))
(defn gt-lt [inf sup] (s/and #(>  % inf) #(<  % sup)))

;(s/def ::max-r (ge-le 0.0 1.0))
;(s/def ::s     (gt-le 0.0 1.0))
;(s/def ::h     (ge-le 0.0 1.0))
;(s/def ::x1    (ge-le 0.0 1.0))
;(s/def ::x2    (ge-le 0.0 1.0))
;(s/def ::x3    (ge-le 0.0 1.0))
;(s/def ::x-freqs #(<= % 1.0)) ; will be passed x1+x2+x3
;(s/def ::B-freqs #(> % 0.0))  ; will be passed x1+x3

(s/def ::timesteps (s/and integer? pos?))

;(s/def ::chart-params (s/keys :req-un [::max-r ::s ::h ::x1 ::x2 ::x3 ::x-freqs ::B-freqs]))
(s/def ::chart-params (s/keys :req-un [::timesteps]))

(defn prep-params-for-validation
  "assoc into params any additional entries needed for validation with spec."
  [params]
  (let [{:keys [x1 x2 x3]} params]
    (-> params
        (assoc :x-freqs (+ x1 x2 x3))
        (assoc :B-freqs (+ x1 x3)))))

;; -------------------------
;; run simulations, generate chart

(defn calc-every-nth
  [timesteps]
  (long (/ timesteps num-points)))


(defn sample-data
  "TEST VERSION"
  [raw-data timesteps every-nth]
  (map second
       (take-nth every-nth 
                 (take (+ every-nth timesteps) ; round up
                       raw-data))))

(defn for-nvd3
  [ys]
  (vec (map #(hash-map :x %1 :y %2)
            (range)
            ys)))

(defn make-chart-config
  [raw-data chart-params$ every-nth]
  "Make NVD3 chart configuration data object."
  (let [{:keys [timesteps]} @chart-params$
        sampled-data (sample-data raw-data timesteps every-nth)]
    (clj->js
      [{:values (for-nvd3 (map :phi sampled-data))    :key "phi"    :color "#000000" :area false :fillOpacity -1}
       {:values (for-nvd3 (map :err sampled-data))    :key "err"    :color "#ff0000" :area false :fillOpacity -1}
       {:values (for-nvd3 (map :sigma sampled-data))  :key "sigma"  :color "#00ff00" :area false :fillOpacity -1}
       {:values (for-nvd3 (map :gen-wt sampled-data)) :key "gen-wt" :color "#0000ff" :area false :fillOpacity -1}])))

(defn make-chart
  [raw-data svg-id chart-params$]
  "Create an NVD3 line chart with configuration parameters in @chart-params$
  and attach it to SVG object with id svg-id."
  (let [chart (.lineChart js/nv.models)
        every-nth (calc-every-nth (:timesteps @chart-params$))]
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
        (datum (make-chart-config raw-data chart-params$ every-nth))
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
                            (if-let [spec-data (s/explain-data ::chart-params (prep-params-for-validation @params$))] ; if bad inputs (nil if ok)
                              (do
                                (reset! error-text$ error-text)
                                (doseq [ki (explain-data-problem-keys spec-data)] ; NOTE this function must change with new Clojurescript release
                                  (let [ks (cond (= ki :x-freqs) [:x1 :x2 :x3] ; special case--need to highlight multiple fields
                                                 (= ki :B-freqs) [:x1 :x3]     ; ditto
                                                 :else [ki])] ; degenerate default: only one field to highlight this time
                                    (doseq [k ks]
                                      (swap! colors$ assoc k error-color)))))
                              (do
                                (reset! button-label$ running-label)
                                (js/setTimeout (fn [] ; allow DOM update b4 make-chart runs
                                                 (make-chart raw-data svg-id params$) 
                                                 (reset! button-label$ ready-label))
                                               100))))}
       @button-label$])))

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
  (let [float-width 6
        {:keys [x1 x2 x3]} @params$]  ; seems ok: entire form re-rendered(?)
    [:form 
     [float-input :s     params$ colors$ float-width "selection coeff"]
     [float-input :h     params$ colors$ float-width "heterozygote coeff"]
     [float-input :max-r params$ colors$ float-width "max recomb prob" [:em "r"]]
     [spaces 4]
     [chart-button svg-id params$ colors$ form-labels]
     [:br]
     [float-input :x1    params$ colors$ float-width "" [:em "x"] [:sub 1]]
     [float-input :x2    params$ colors$ float-width "" [:em "x"] [:sub 2]]
     [float-input :x3    params$ colors$ float-width "" [:em "x"] [:sub 3]]
     [spaces 3]
     [float-text (- 1 x1 x2 x3) [:em "x"] [:sub 4]] ; display x4
     [spaces 13]
     [float-text "initial neutral heterozygosity"]
     [:br]
     [:div {:id "error-text" 
            :style {:color error-color :font-size "16px" :font-weight "normal" :text-align "left"}} ; TODO move styles into css file?
       @error-text$]]))

(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:script {:type "text/javascript" :src "js/compiled/linkage.js"}]])

(defn home-render []
  "Set up main chart page (except for chart)."
  (head)
  [:div
   [:div {:id "chart-div"}
    [:svg {:id chart-svg-id :height (str svg-height "px")}]
    [chart-params-form (str "#" chart-svg-id) chart-params$ chart-param-colors$]]])

(defn home-did-mount [this]
  "Add initial chart to main page."
  (make-chart raw-data (str "#" chart-svg-id) chart-params$))

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
