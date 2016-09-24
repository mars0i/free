(defproject free "0.1.0-SNAPSHOT"
  :description "Experiments with free energy minimization"
  :url "https://github.com/mars0i/free"
  :license {:name "Gnu General Public License version 3.0"
            :url "http://www.gnu.org/copyleft/gpl.html"}
  :source-paths ["src"]
  :java-source-paths ["src/java"]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 ;[org.clojure/clojure "1.9.0-alpha12"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 [net.mikera/core.matrix "0.54.0"]
                 [net.mikera/vectorz-clj "0.45.0"]
                 [thinktopic/aljabr "0.1.1"] ; ignore [... "0.4.0-SNAPSHOT"] in README.md--wishful thinking
                 [clatrix "0.5.0"]
                 ;[org.clojars.ds923y/nd4clj "0.1.0-SNAPSHOT"] ; not ready for use
                 [criterium "0.4.4"] ; to use, e.g.: (use '[criterium.core :as c])
                 ;[org.jfree.chart "1.0.19"]
                 ;[uncomplicate/fluokitten "0.5.0"]
                 [incanter "1.5.7"]])
