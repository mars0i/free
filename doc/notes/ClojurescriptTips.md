Tips for getting this to work in Clojurescript
===

### in-line require

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

You can do this at the repl, too, but `:reload-all` seems needed:

    (ns cljs.user (:require [free.example-2 :as e2] :reload-all))


### multiple ns statements

If you do the preceding, i.e. add a second `ns` statement in order to
require a namespace, this `ns` will shadow an earlier one.  So any
namespaces you want below it have to be `:require`d here (and not
in the first `ns`, or in both).


### compiler warning crud

It appears that if you have a clean setup, i.e. after doing `lein
clean`, I get all sorts of wierd spurious compiler warnings about
arity and undefined vars, e.g. for things in core.matrix.  But then if
you exit out and do `lein figwheel` again, everything's fine.


### compiling

It seems as if this is a working procedure:

	lein clean
	lein figwheel
	(exist out of the repl)
	lein figwheel
	(again)



### loading a core.matrix implementation

You can't use `set-current-implementation` because it causes a
standalone `require` statement to run in order to load the
implementation (see above).

A list of all namespaces of implementations in KNOWN-IMPLEMENTATIONS is in
https://github.com/mikera/core.matrix/blob/develop/src/main/clojure/clojure/core/matrix/implementations.cljc


