Tips for getting this to work in Clojurescript
===

### problem

plot-pages and example-5 won't run in a new browser unles you do
something more, e.g.  load free.random at the repl, with `:reload`.
However, loading utils.random at the repl, i.e. what free.random loads,
doesn't help.  What's special about utils.random is that it uses
cljsjs/chance, but since loading utils.random that doesn't help, it
seems as if the issue concerns free.random.  What's special about it?
That it has atoms?  If you load it without `:reload` at the repl,
`next-gaussian`, which is mentioned in an error, is nil.
Note that a seed isn't printed when I first load the whole system,
which suggests that free.random didn't run.

If I load free.random at the repl without `:reload`, then `session-id`
gets defined, but `rng$` is nil.  Two things slightly interesting happen
in defining `rng$`: It's an atom, and the content of the atom is made
with `make-rng`, which calls `Chance`.  `make-rng` also calls the
flush routine on the new Mersenne Twister.

But if you re-save utils/random.cljs, everything immediately starts
working.

I don't think this is relevant:
http://dev.clojure.org/jira/browse/CLJS-1479

### notes on aljabr

As of early October 2016, aljabr doesn't have inverse.  inverse lives
in PMatrixOps, which aljabr doesn't implement.  This is because the
original Javascript ndarray lib doesn't implement inverse.

*And*, core.matrix provides a default implementation of inverse by
*converting the matrix to vectorz*, then calling vectorz's inverse, and
then converting it back to whatever the input implementation was. i.e.
if there's no implementation of inverse.  (This is in
clojure.core.matrix.impl.defaults.) e.g. This seems to be how
core.matrix inverts persistent-vector *or ndarray* matrices.  Clatrix
has its own inverter.  (You can see in action this if you modify
project.clj or pom.xmlto disable vectorz.)

So aljabr can't just use core.matrix's default implementation of
inverse.

So that's it.  It looks like I simply can't use a Clojurescript matrix
version of free at this time (10/2016), i.e. not until there is a
Clojurescript matrix implementation that supports inversion.  Wow.

(Hmm.  But there are several other Javascript matrix libraries, and at
least some of them provide inverse.  If the license is right, could I
just copy the code into the Javascript ndarry and then add it to
aljabr?  Well for now don't do that.  Other fish.)
Sylvester	


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

There's some kind of order of loading issue or something.  I had to load
`free.example-3` two or three times before the defs in it had non-nil
values.

### scalar vs matrix arithmetic substitution

In Clojure, I can do this: I have an atom `use-core-matrix$` in
`free.config` whose value specifies whether I want scalar or matrix
arithmetic.  (This is initialized to false.)  Then in level.cljc, I load
either `free.scalar-arithmetic` or `free.matrix-arithmetic` depending on
the value of the variable.  If I want an example source file to load the
matrix operators, I set `use-core-matrix$` to `true`, *and then* require
`free.level`, and it will load the right version of arithmetic
operators.

This is a little bit trickier in Clojurescript because you can't have
standalone `require`s.  

However, it turns out it doesn't work anyway, I think because when
`free.level` requires `free.config`, the initialization code runs again
if I use `:reload-all`, so it gets initialized to `false` and has that
value at the time that `free.level` starts up, and if I don't use
`:reload-all`, then it was already initialied to `false` the previous
time it was loaded, so the same thing happens.

I'm thinking that maybe I should put the scalar and matrix operators in
different directories, and then have different profiles in core.matrix
that will load one or the other directory.  And don't have an atom
controlling this difference at all.  Note this would then have to be
done the same way in Clojure.

### Misc tips

Don't stick a println into a Clojurescript source file (at least not
a macro file).  It will get inserted into the javascript output, raw,
and cause mysterious errors.
