nils as params in Level 0
====

# Overview

In fall 2016, there was a period in which I thought that the in level 0,
phi, phi-dt, sigma-dt, and theta-dt (along with gen and gen') were never
referenced, so I assigned nil to them in model.cljs (later model.cljc).
This worked fine, but then when I got the Clojurescript working with
the aljabr core.matrix implementation, suddenly I got errors from the
nils.  Even worse, I discovered that changing the numeric values of
these parameters can change the output of the system!

# -dt parameters

It turns out that it's correct that those -dt params are in fact used.
(More on phi below.)  So why didn't I notice this before?

1. model.cljs was the *only* model in which I'd nil'ed out these
   parameters.  In example_*.cljc and exercise_3.cljc, I'd assigned 0.01 to
   phi-dt and 0 to sigma-dt and theta-dt.

2. The only update function at level 1 that uses level 0 parameters is
   phi-inc (via next-phi), which uses only epsilon and theta from level 0.
   So no level 1 calculation was affected by the nils at level 0.

3. Level 0 is updated by the next-bottom function created by
   make-next-bottom.  next-bottom ignores phi at level 0 and gives it a
   new numeric value for the next tick.  next-bottom also calls
   next-epsilon, next-sigma, and next-theta on level 0.  I'm still
   puzzled why these don't generate errors on the first tick, at least.
   epsilon-inc uses phi at level 0, and all three of these next-*
   functions multiply by their corresponding -dt parameters.  (Note that
   with scalar `*` and `+`, there's some tolerance of nils.  `(* nil)`
   is nil, for example.  I don't quite see how this explains the success
   of next-epsilon and next-theta run at level 0, because `(* nil
   anything-else)` generates an error.  Maybe this is part of the story,
   though?  Also note that in my macro implementations of `m*` and `e*`,
   I simply pass through the argument when there's only one argument.
   If something along these lines provides the explanation, then it's 
   not surprising that the problem was revealed only when I switched to
   using matrix operators.)

# phi

I believe that phi at level 0 needs to be non-nil only at the very
first tick.  After that, it's replaced by the output of the next-bottom
function.

