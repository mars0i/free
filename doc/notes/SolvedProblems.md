solved problems
===

### a problem

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
working.  Even though `require`ing it at the repl, even with
`:reload-all` help.

You can tell when you've set things right because the random seed
prints out; i.e. free.random successfully runs.

I don't think this is relevant:
http://dev.clojure.org/jira/browse/CLJS-1479

The problem goes away completely in free.exercise-3, which doesn't use
any of the random namespaces.

The problem was using a `println` in free.random without first calling
`enable-console-print!`.
