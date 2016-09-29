Tips for getting this to work in Clojurescript
===

## in-line require

In some cases I want to require a namespace *after* I do something,
so I tried to use `require` rather than `ns-with-`:require`.  This
doesn't work in Clojurescript.

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


## multiple ns statements

If you do the preceding, i.e. add a second `ns` statement in order to
require a namespace, this `ns` will shadow an earlier one.  So any
namespaces you want below it have to be `:require`d here (and not
in the first `ns`, or in both).
