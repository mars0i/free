;; This software is copyright 2016 by Marshall Abrams, and is distributed
;; under the Gnu General Public License version 3.0 as specified in the
;; the file LICENSE.

(ns free.core
  (:require [free.plot-page :as page]))

;; The cljsbuild config in project.clj has a line expecting this to be here:
(defn on-js-reload [] (page/on-js-reload))
