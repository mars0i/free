Tips for getting this to work in Clojurescript
===


At http://stackoverflow.com/questions/12879027/cannot-use-in-clojurescript-repl
Luke VanderHart writes:

> Because ClojureScript namespaces are implemented completely
differently than Clojure, ClojureScript does not support the use or
require forms directly.

> Instead, you must use the ns macro. To use clojure.zip in the
cljs.user namespace, then, just do the following:

    (ns cljs.user (:use [clojure.zip :only [insert-child]]))

> Note that the forms supported in the ClojureScript version of ns are a
subset of those supported in Clojure; specifically, :use clauses must
specify an :only form, and the :require clause must specify an :as form.



