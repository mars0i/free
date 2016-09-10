Notes on level.cljc
===

## Notation

`m*`, `m+`, `m-` are either scalar or matrix `*`, `+`, and `-`, depending on the
value of `use-core-matrix`, which determines which namespace is loaded.
`e*` is `*`, or elementwise matrix multiplication.  `tr` is matrix transpose,
or the identity function for scalars.  `inv` is reciprocal, for scalars,
or matrix inverse, for matrices.  `(make-identity-obj n)` returns an n by n 
identity matrix, or 1 for scalars.

The derivative of function `f` is called `f'` .  A value of `foo` at the next
level down is called `-foo`.  A value of `foo` at the next level up is called
`+foo`.  Also see the docstrings for ``Level`` and other functions.

The functions `next-foo` calculate the next value of `foo`.  Each `next-foo`
function accepts three `Level` structures as values: The next `Level` down,
the current `Level`, and the next `Level` up.  However, some of these `Level`s
might not be used for a given calculation, in which case the parameter
for that level will be `_` , indicating that it will be ignored.

This version of level.cljc doesn't use a function g, but assumes that g is
a product of theta with another function `h`, as in many Bogacz's
examples.


## Overview

Please see the docstring for `->Level` or `map->Level` (at the repl or
in level.cljc) for information about the fields in the `Level` record
definition.  Note that each of the data fields also has a corresponding
`-dt` field that holds the scaling factor that determines how fast the
field will be updated.  I chose to include these in each Level record to
allow for the possibility of different speeds at different levesl.

The state of a network consists of a sequence of three or more levels:
A first (zeroth) and last level, and one or more inner levels.  It's
only the inner levels that should be updated according to central
equations in Bogacz such as (53) and (54).  The first level captures
sensory input--i.e. it records the prediction error eps, which is
calculated from sensory input phi at that level, along with a function
theta h of the next level phi.  i.e. at this level, `phi` is simply
provided by the system outside of the levels, and is not calculated
from lower level prediction errors as in (53). The last level simply
provides a `phi`, which is the mean of a prior distribution at that
level.  This `phi` typically never changes. (It's genetically or
developmentally determined.) The other terms at this top level can be
ignored. Note that Bogacz's examples typically use two inner levels;
his representation captures what's called the first and last levels
here using individual parameters such as `u` and `v_p`.

See src/free/exercise_3.cljc for a simple illustration of use of this
system.  A comment there also shows how to plot output using Incanter.


## Level dependencies

* `phi` at level *n* depends on `eps` at level *n* - 1.

* `eps` at level *n* depends on `phi` at level *n* + 1.

* `theta` at level *n* depends on `phi` at level *n* + 1.

* `sigma` at level *n* depends only on level *n*.


## next-level

There's an asymmetry between the bottom and top levels.  

### Bottom level: 

It's only the next `phi` that depends on the next level down from the
perspective of other levels.  since the bottom level has no next level
down, `phi` must be treated specially.  So the bottom level needs to
provide `phi`, which should usually vary as a function of things outside
the levels system.  Since it's natural to have `phi` at each level be a
variable, not the result of a function, we need to provide a special
function to generate new `phi`s on every timestep.  This is the
`next-bottom` that must be provided to `next-level`.

### Top level:

At the top of the stack, `phi` is the only variable from above that's
needed by any of the `next-` functions; it's used by `next-eps` and
`next-theta`.  If `phi` never changes (cf. Bogacz's use of `v_p` in
his answer to exercise 3), then we can just make the very top level
be a special one that contains a `phi` (and probably only `phi`) and
that isn't updated by `next-phi` or by the other `next-` functions.


## Questions

On p. 7 col 2, end of section 3, Bogacz says:
"Thus on each trial we need to modify the model parameters a little bit
(rather than until minimum of free energy is reached as was the case for
phi)."  i.e., I think, this means that updating sigma and theta
should be done more gradually, i.e. over more timesteps, i.e. based
on more sensory input filtering up, than updating phi.  What about
epsilon?  I think that should go at the speed of phi, right?

cf. end of section 5, where he says that the Hebbian Sigma update 
methods (which I'm not using, initially) introduced there depend on phi 
changing more slowly.  But isn't that the opposite of what I just said??

Also, should the higher levels also go more slowly??  i.e. as you go
higher, you update less often?  Or not?
