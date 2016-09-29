(defproject free "0.2.0-SNAPSHOT"
  :description "Experiments with free energy minimization"
  :url "https://github.com/mars0i/free"
  :license {:name "Gnu General Public License version 3.0"
            :url "http://www.gnu.org/copyleft/gpl.html"}
  :min-lein-version "2.6.1"
  :java-source-paths ["src/java"]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 ;[org.clojure/clojure "1.9.0-alpha12"]
                 [org.clojure/clojurescript "1.9.229"] ; very nitpicky spec tests
                 ;[org.clojure/clojurescript "1.9.225"]
                 [org.clojure/core.async "0.2.385" :exclusions [org.clojure/tools.reader]]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 [net.mikera/core.matrix "0.54.0"]
                 [net.mikera/vectorz-clj "0.45.0"]
                 [thinktopic/aljabr "0.1.1"] ; ignore [... "0.4.0-SNAPSHOT"] in README.md--wishful thinking
                 [clatrix "0.5.0"]
                 ;[org.clojars.ds923y/nd4clj "0.1.0-SNAPSHOT"] ; not ready for use
                 [criterium "0.4.4"] ; to use, e.g.: (use '[criterium.core :as c])
                 ;[org.jfree.chart "1.0.19"]
                 ;[uncomplicate/fluokitten "0.5.0"]
                 [incanter "1.5.7"]
                 [cljsjs/chance "0.7.3-0"]]

  :plugins [[lein-figwheel "0.5.4-7"]
            [lein-cljsbuild "1.1.3" :exclusions [[org.clojure/clojure]]]]

  :source-paths ["src"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src"]
                ;; The presence of a :figwheel configuration here will cause figwheel to inject the figwheel client into your build
                :figwheel {:on-jsload "free.core/on-js-reload"
                           ;; :open-urls will pop open your application
                           ;; in the default browser once Figwheel has
                           ;; started and complied your application.
                           ;; Comment this out once it no longer serves you.
                           :open-urls ["http://localhost:3449/index.html"]}
                :compiler {:main free.core
                           :asset-path "js/compiled/out"
                           :output-to "resources/public/js/compiled/free.js"
                           :output-dir "resources/public/js/compiled/out"
                           :pretty-print false
                           :optimizations :none
                           :source-map-timestamp true
                           ;; To console.log CLJS data-structures make sure you enable devtools in Chrome
                           ;; https://github.com/binaryage/cljs-devtools
                           :preloads [devtools.preload]}}
               ;; This next build is an compressed minified build for
               ;; production. You can build this with:
               ;; lein cljsbuild once min
               {:id "dist"
                :source-paths ["src"]
                :compiler {:asset-path "js/compiled/dist"
                           :output-to "resources/public/js/compiled/free.js"
                           :output-dir "resources/public/js/compiled/dist"
                           :main free.core
                           :optimizations :simple ; :none
                           :pretty-print true}}
               {:id "min"
                :source-paths ["src"]
                :compiler {:output-to "resources/public/js/compiled/free.js"
                           :main free.core
                           :optimizations :advanced
                           :pretty-print false}}]}

  :figwheel {;; :http-server-root "public" ;; default and assumes "resources"
             ;; :server-port 3449 ;; default
             ;; :server-ip "127.0.0.1"

             :css-dirs ["resources/public/css"] ;; watch and update CSS

             ;; Start an nREPL server into the running figwheel process
             ;; :nrepl-port 7888

             ;; Server Ring Handler (optional)
             ;; if you want to embed a ring handler into the figwheel http-kit
             ;; server, this is for simple ring servers, if this

             ;; doesn't work for you just run your own server (see lein-ring)

             ;; :ring-handler hello_world.server/handler

             ;; To be able to open files in your editor from the heads up display
             ;; you will need to put a script on your path.
             ;; that script will have to take a file path and a line number
             ;; ie. in  ~/bin/myfile-opener
             ;; #! /bin/sh
             ;; emacsclient -n +$2 $1
             ;;
             ;; :open-file-command "myfile-opener"

             ;; if you are using emacsclient you can just use
             ;; :open-file-command "emacsclient"

             ;; if you want to disable the REPL
             ;; :repl false

             ;; to configure a different figwheel logfile path
             ;; :server-logfile "tmp/logs/figwheel-logfile.log"
             }


  ;; setting up nREPL for Figwheel and ClojureScript dev
  ;; Please see:
  ;; https://github.com/bhauman/lein-figwheel/wiki/Using-the-Figwheel-REPL-within-NRepl


  :profiles {:dev {:dependencies [[binaryage/devtools "0.7.2"]
                                  [figwheel-sidecar "0.5.4-7"]
                                  [com.cemerick/piggieback "0.2.1"]]
                   ;; need to add dev source path here to get user.clj loaded
                   :source-paths ["src" "dev"]
                   ;; for CIDER
                   ;; :plugins [[cider/cider-nrepl "0.12.0"]]
                   :repl-options {; for nREPL dev you really need to limit output
                                  :init (set! *print-length* 50)
                                  :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}}

)
