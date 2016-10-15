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

;; The first entry is supposed to be turned into dots rather than a line using voodoo CSS I stuck at the end of site.css.
;; Got that from http://stackoverflow.com/questions/27892806/css-styling-of-points-in-figure and a bunch of trial and error.
;; BUT THE VOODOO ONLY WORKS FOR SIMPLE CASES--broken and erratic for variable point display.  Will need to understand the voodo.
;; The paths seem to be under the first g inside chart-svg.  No I think those are the legend objects.
;; it's under nv-focus inside nv-linesWrap.
;; #chart-svg > g > g > g.nv-focus > g.nv-linesWrap.nvd3-svg > g > g > g.nv-groups > g.nv-group.nv-series-0
;; <g class=" nv-group nv-series-0" style="stroke-opacity: 1; stroke-width: 1.5; fill-opacity: -1; fill: rgb(96, 96, 96); stroke: rgb(96, 96, 96);"><path class="nv-line" d="M0,354.36095213153703L2.04,115.53436260669373L4.08,123.14802099072087L6.12,92.00073005465936L8.16,76.75191278184143L10.200000000000001,132.75281451302106L12.24,100.34999654190037L14.280000000000001,135.93047281250918L16.32,62.223382482360876L18.36,63.58523052384037L20.400000000000002,156.38175572409375L22.439999999999998,98.20513549914912L24.48,76.18580118582312L26.52,139.78210969472886L28.560000000000002,89.73495658624422L30.599999999999998,100.39029175460031L32.64,103.86342684509846L34.68,115.95013123297974L36.72,119.77386989451675L38.76,132.82488246627742L40.800000000000004,76.80340997084025L42.84,114.03509313400957L44.879999999999995,101.42642428537657L46.92,114.0412513724828L48.96,102.12342487869849L51,86.21054660767446L53.04,79.29425985423983L55.08,117.02923730797221L57.120000000000005,138.32425609715528L59.160000000000004,95.32572323670612L61.199999999999996,132.76109143013247L63.24,81.80061301010863L65.28,73.7767318346237L67.32000000000001,97.53740550507088L69.36,87.88284630265204L71.4,133.838898369712L73.44,33.75550519231484L75.47999999999999,136.1977190585723L77.52,133.45704477195056L79.56,121.55312070479972L81.60000000000001,66.92780608594364L83.64,89.14109422230199L85.68,117.73875525547103L87.72,339.5792808873381L89.75999999999999,336.50077019745663L91.8,275.45347570743996L93.84,355.63618756558026L95.88,292.2856327344044L97.92,294.1777038546009L99.96000000000001,339.39295050166044L102,351.21985473796343L104.03999999999999,353.70758446515765L106.08,323.0374129561427L108.11999999999999,345.52452817790265L110.16,412.9325874854783L112.2,306.0539692210173L114.24000000000001,350.77154520379975L116.28,341.7458630003873L118.32000000000001,356.9446021510427L120.36,366.70886514351315L122.39999999999999,338.86993913530534L124.44,309.61941635044514L126.48,349.12515233410085L128.52,352.3586977408678L130.56,317.313598883517L132.6,347.5551436755479L134.64000000000001,322.8001392135808L136.68,289.40390278131923L138.72,409.087313257629L140.76000000000002,355.4850867367154L142.8,306.2250785827529L144.83999999999997,361.49781112988L146.88,386.2565765725167L148.92,308.7737932735945L150.95999999999998,385.8897194300722L153,310.29467539959705L155.04,381.04548531830005L157.07999999999998,347.6577634160789L159.12,371.1723839686117L161.16,312.8089575849483L163.20000000000002,315.91927530900665L165.24,279.83883907033294L167.28,291.5159179389579L169.32000000000002,274.19060931603684L171.36,391.25665839564266L173.4,354.0083512321092L175.44,287.70062461750456L177.48,383.9193133935702L179.51999999999998,350.4395326954019L181.56,369.45463713393576L183.6,335.20774073061494L185.64,353.3385882113621L187.68,340.9689165150396L189.72,50.965937656572315L191.76,139.65478548579628L193.8,188.1325412140356L195.84,163.19035320254002L197.88,71.51123653461403L199.92000000000002,128.87424090110872L201.96,57.3453425760436L204,87.27038015790757L206.04000000000002,100.89984918641676L208.07999999999998,115.7611197771229L210.11999999999998,67.7532889363977L212.16,95.24620011451094L214.2,108.08990940029611L216.23999999999998,87.20251696041497L218.28,92.60668696785088L220.32,152.53106796997608L222.35999999999999,123.89684445196437L224.4,116.65496597227482L226.44,83.9948245287344L228.48000000000002,60.08479955144289L230.52,58.2567353849812L232.56,81.75179155290604L234.60000000000002,77.0455649028787L236.64000000000001,125.12791725983786L238.68,123.84522123398655L240.72,110.94958442991893L242.76,39.91764945311565L244.79999999999998,113.4320225291562L246.84,101.1924765880337L248.88,89.51107956511811L250.92,138.8936456420388L252.96,120.84285802904051L255,59.13810600051239L257.04,102.96948101313373L259.08,131.87523394134254L261.12,119.37314580498195L263.16,201.99767703498873L265.2,65.68159213569315L267.24,83.72637311431716L269.28000000000003,125.30301469501785L271.32,135.35972690683096L273.36,113.79197784393219L275.40000000000003,66.74433099740128L277.44,123.61931254810106L279.48,115.84679872880798L281.52000000000004,83.86339725191395L283.56,154.2722350109232L285.6,80.76397137697435L287.64,137.6312227630829L289.67999999999995,88.23216709806805L291.71999999999997,365.61595534695516L293.76,315.8831733425851L295.79999999999995,363.4733189104418L297.84,328.2172844937801L299.88,362.94286697204086L301.91999999999996,290.1316930848891L303.96,347.39488298434156L306,364.39424659430506L308.03999999999996,334.12677915355835L310.08,384.2731121213091L312.12,375.6090194589455L314.15999999999997,292.0972044506346L316.2,390.37110028334547L318.24,385.4201815757494L320.28000000000003,315.5157343288444L322.32,333.96519435744744L324.36,344.90712027144707L326.40000000000003,341.8606521630198L328.44,342.21997361283894L330.48,343.97495359863495L332.52000000000004,308.47058359827065L334.56,340.4245804985016L336.6,294.8190876851309L338.64000000000004,313.24825997430844L340.68,337.5761278742997L342.72,298.5651127451408L344.76000000000005,288.00781949639975L346.8,316.83955160005746L348.84000000000003,369.4543465011911L350.88,343.8812248159473L352.91999999999996,413.89592253077853L354.96,357.5588447432466L357,357.19850879366953L359.03999999999996,313.63950026679873L361.08,348.6851538897149L363.12,291.79865835866207L365.15999999999997,321.14439125556993L367.2,309.55581959991434L369.24,315.92769665615305L371.28,370.35371103653864L373.32,373.7674632631245L375.36,283.9465635284876L377.4,284.25770752051505L379.44,335.5761491185743L381.48,289.02672896456977L383.52,296.45352978754994L385.56,391.5079611692964L387.6,323.87277818525985L389.64,350.2045916414788L391.68,353.5918548757691L393.72,68.65407382209878L395.76,116.00409538940461L397.8,102.85752043642647L399.84000000000003,106.62386511511099L401.88,95.8119831978626L403.92,95.2722286252432L405.96000000000004,83.1632003800869L408,99.28566420675875L410.04,103.3312548733271L412.08000000000004,45.91733794225655L414.12,62.265746137542294L416.15999999999997,133.99144579946585L418.2,76.24700652921334L420.23999999999995,153.51966171999553L422.28,139.34393203645251L424.32,76.28648675445622L426.35999999999996,53.636037639096685L428.4,97.08002637913123L430.44,66.54108727610193L432.47999999999996,93.87078726256232L434.52,129.25098408857536L436.56,98.07896657342003L438.59999999999997,115.07143429899915L440.64,97.96836782891371L442.68,75.11448640086888L444.71999999999997,114.72762430906974L446.76,108.5848217126837L448.8,78.92929002365L450.84000000000003,98.10930891149808L452.88,45.67433307387286L454.92,120.81345559939011L456.96000000000004,92.80442301239265L459,146.17925505099848L461.04,123.07435008965463L463.08000000000004,130.9290135964838L465.12,130.16156694016075L467.16,110.97285161005632L469.20000000000005,193.00759788191363L471.24,130.48016942244212L473.28000000000003,36.39206533515921L475.32000000000005,103.65495681165324L477.36,128.18623930067645L479.4,173.13490723544638L481.44,100.60215096608705L483.47999999999996,109.35454458412602L485.52,88.62213903170466L487.56,105.25581708788684L489.59999999999997,111.85053877264356L491.64,84.96855914021278L493.68,129.10496664065388L495.71999999999997,372.8865265912371L497.76,334.9264017668062L499.8,358.6116805002861L501.84,302.5175938737933L503.88,372.59826439817107L505.92,361.3324858799973L507.96,376.76976335168L510,369.16973521873456L512.04,341.3367083072418L514.08,305.0408275399394L516.12,337.61871669599L518.16,337.9485484127788L520.2,318.3491600864514L522.24,386.3096343555886L524.28,380.0250864981915L526.32,361.6270739232701L528.36,341.6941446025111L530.4,287.7611664325395L532.44,337.407617041885L534.48,363.94150845294496L536.52,400.47865406899024L538.5600000000001,351.925283395226L540.6,325.8168085169978L542.64,289.1373498418361L544.6800000000001,342.4649593107809L546.72,388.93437918893324L548.76,388.74307462625706L550.8000000000001,398.99239089850107L552.84,285.60488138457964L554.88,293.3991982934723L556.9200000000001,368.5277797487306L558.96,280.61484885304725L561,337.6608998429035L563.0400000000001,310.3003893126255L565.08,333.69522472372597L567.12,332.0823791166116L569.1600000000001,337.73828898968276L571.2,317.5286691698051L573.24,357.7594528936299L575.28,346.5823203298771L577.3199999999999,354.94364598857754L579.3599999999999,325.3485276741835L581.4,331.49737531297063L583.4399999999999,365.68366726787286L585.4799999999999,325.64224439581534L587.52,334.22730259885105L589.56,393.07970192202924L591.5999999999999,349.72183977924664L593.64,317.3007400767879L595.68,338.4514857561687L597.7199999999999,125.10763706607419L599.76,104.49139817084254L601.8,25.068530805544583L603.8399999999999,72.26029361362329L605.88,118.08483377704688L607.92,138.08848525764157L609.9599999999999,132.1617234754727L612,129.38507008547523L614.04,131.7010704938672L616.0799999999999,129.10109496257687L618.12,171.77728737366337L620.16,117.70088139780194L622.1999999999999,111.09421680406393L624.24,96.1651723317189L626.28,75.5925216449176L628.3199999999999,43.31809462534094L630.36,99.08420018879872L632.4,138.43449884223514L634.4399999999999,156.50604570538175L636.48,75.29012668296711L638.52,71.76317628463497L640.5600000000001,116.89348257786472L642.6,56.23836914773874L644.64,117.91543805713229L646.6800000000001,138.91578784956616L648.72,73.30883275926054L650.76,109.21887437591542L652.8000000000001,145.8644526318177L654.84,75.74959870805102L656.88,77.48372948154932L658.9200000000001,102.4715873708486L660.96,159.93339469734764L663,49.64915644404813L665.0400000000001,108.18411434774497L667.08,123.36829191923715L669.12,148.66884334527109L671.1600000000001,107.00359884263109L673.2,138.5474791030446L675.24,113.39918313564488L677.2800000000001,110.44216329328435L679.32,80.47529932390046L681.36,114.79708292690391L683.4000000000001,83.68100725481037L685.44,105.7889054781312L687.48,107.37146485358763L689.5200000000001,132.83103936753977L691.5600000000001,72.19458697106809L693.6,119.58863532700549L695.6400000000001,105.6982005695942L697.6800000000001,125.31372003842372L699.72,347.85154771485367L701.76,337.69086339450416L703.8,321.87059064704584L705.8399999999999,358.79647916153687L707.88,365.26697534180056L709.92,288.0583252601366L711.9599999999999,351.46120785288485L714,371.710180972591L716.04,361.0766451899346L718.0799999999999,372.7869820438399L720.12,309.76271277502985L722.16,370.8074649520689L724.1999999999999,385.9174134928843L726.24,373.2940858124621L728.28,321.8776201357347L730.3199999999999,391.5715292345199L732.36,255.6509255597878L734.4,348.68985379918445L736.4399999999999,339.7568871969576L738.48,361.37212163757346L740.52,349.7931016397596L742.56,356.7000348203081L744.6,405.1739920930107L746.64,339.81507799048177L748.68,330.44652135958387L750.72,390.1901800197168L752.76,289.9219643903243L754.8,336.837489267628L756.84,348.4116860741456L758.88,346.979433814983L760.92,310.99623876695813L762.96,298.0253622898827L765,343.0267461967031L767.04,320.48677518517394L769.08,358.99604847102955L771.12,303.03561347019956L773.16,371.86346473733033L775.2,357.6313904110554L777.24,390.59802776594984L779.28,356.90978820929604L781.32,327.2618822031852L783.36,333.972811520531L785.4,298.842693238405L787.44,415.2472514094256L789.48,311.2239986635734L791.52,313.13728285715865L793.5600000000001,394.3843401955816L795.6,339.9634446060726L797.64,310.4116391806255L799.6800000000001,393.51271085565975L801.72,77.79846368525163L803.76,99.65601518591039L805.8000000000001,109.41606159495089L807.84,120.49511713996903L809.88,67.96573889826027L811.9200000000001,115.29766419903312L813.96,119.90237060157595L816,99.40336763346968L818.0400000000001,167.3159009793373L820.08,120.85278250495L822.12,138.51784729801787L824.1600000000001,80.15760046113543L826.2,115.28079459935792L828.24,15.380317901779005L830.28,85.83286991839422L832.3199999999999,89.31724961491356L834.3599999999999,120.0570788852288L836.4,87.78842398614294L838.4399999999999,120.89188016461114L840.4799999999999,42.247296231000874L842.52,74.3540907988346L844.56,133.85570558526973L846.5999999999999,108.15458459127385L848.64,165.3190123847264L850.68,0L852.7199999999999,44.99886997570266L854.76,55.234297993103795L856.8,101.82767776185214L858.8399999999999,20.097457643204457L860.88,85.29964048446715L862.92,78.48972355942323L864.9599999999999,102.87728742153352L867,114.65031754627898L869.04,104.98090093149092L871.0799999999999,86.74971871856776L873.12,66.93266610505792L875.16,93.66460502758268L877.1999999999999,131.22386863313508L879.24,131.22850075911373L881.28,173.76525147293233L883.3199999999999,153.57795950685852L885.36,141.30840409098454L887.4,119.6355339847335L889.4399999999999,152.60431736495502L891.48,139.52188346773139L893.52,128.95225484838875L895.5600000000001,179.504608012163L897.6,62.25524250422028L899.64,77.81188994022163L901.6800000000001,116.64186479956918L903.72,362.2388888190503L905.76,336.0042167744451L907.8000000000001,387.5897843198984L909.84,351.439700443538L911.88,380.58076221992275L913.9200000000001,324.43030337606876L915.96,353.7312249720596L918,356.3631041600158L920.0400000000001,338.2604261466875L922.08,377.80488523875937L924.12,320.9712059951447L926.1600000000001,276.90878649651023L928.2,327.70402915129813L930.24,381.3773344540852L932.2800000000001,419.1548829132963L934.32,303.94247568942603L936.36,401.72704122986715L938.4000000000001,381.32262264436656L940.44,352.2461071997614L942.48,297.97889233798566L944.5200000000001,345.4820849099663L946.5600000000001,337.07064396866974L948.6,382.53727493826415L950.6400000000001,360.47893920891426L952.6800000000001,333.9587200089901L954.72,351.2621629847219L956.76,347.86207008142924L958.8,342.8390468825142L960.8399999999999,391.3431217811651L962.88,342.09342876385114L964.92,354.2160500423026L966.9599999999999,336.43673763837046L969,296.83591623916874L971.04,344.95594780269914L973.0799999999999,371.90369473956395L975.12,341.3050354651641L977.16,388.4647281182913L979.1999999999999,334.29716484459664L981.24,341.841962087938L983.28,317.85209950291335L985.3199999999999,262.972031720369L987.36,346.8204040840269L989.4,321.0794090314849L991.4399999999999,292.4070434481909L993.48,366.5606272058312L995.52,379.91146110415076L997.56,375.148011957261L999.6,319.20453064348715L1001.64,331.15605966828167L1003.68,330.45938460909025L1005.72,103.60144374152988L1007.76,160.22533858226694L1009.8,136.24011601210682L1011.84,139.13923741798408L1013.88,101.74429088814814L1015.92,148.54481519482414L1017.96,119.13506381381627L1020,93.80077471505699"></path></g>
;; I think maybe it works like this.  each data set has an nv-point and nv-line set of properties.
;; the former displays points, circles, whatever.  the latter displays a line between them.  if you set the points to opacity 1
;; and the lines to opacity 0, you get only the points (and normally if the points are small, you won't see them anyway).
;; note nv-line is a class of the path inside the group, whereas nv-series-0 is on the group.
;; I don't think nv-scatter is relevant for me.  I deleted it.
;; so the issue is just to find the right nv-series-#, which now depends on order of eval or something.
;; or maybe I can force the order.  reset the chart or something.
;; oh maybe this is kind of a reagent issue. i.e. it's only updating what needs to be updated, and that
;; affects the order.
;; r/force-update or r/force-update-all might be useful.
;; Maybe I need to clear the data from the chart.  can this help?:
;; http://stackoverflow.com/questions/22452112/nvd3-clear-svg-before-loading-new-chart
;; something like this?  (.remove (.select js/d3 "nv-series-0"))
(defn make-bottom-chart-data
  [level-stages]
  [{:id "sensory" :key "sensory input"   :values (xy-pairs (map :phi level-stages))     :color "#606060" :area false :fillOpacity -1}
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
                                (doseq [k (explain-data-problem-keys spec-data)]
                                  (swap! colors$ assoc k error-color)))
                              (do
                                (reset! button-label$ running-label)
                                (js/setTimeout (fn [] ; allow DOM update b4 make-chart runs so I can change the button to show things are in progress
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
