Notes on code in level.cljc
===

## Overview

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

Note that the code in levels.cljc is supposed to be able to work with
any number of middle levels and with either vectors and matrices, or
with scalars.  If `use-core-matrix` is true, the system will be
configured for vectors and matrices.  It should work with scalars, too,
but it will be slower than if `use-core-matrix` is false.  Currently
(9/2016), the value of `use-core-matrix` must be set in levels.cljc
before that file is loaded, but this will probably change in the future.

## The Level data structure

Please see the docstring for `->Level` or `map->Level` (at the repl or
in level.cljc) for information about the fields in the `Level` record
definition.  Note that each of the data fields also has a corresponding
`-dt` field that holds the scaling factor that determines how fast the
field will be updated.  I chose to include these in each Level record to
allow for the possibility of different speeds at different levesl.


## Functional programming and laziness

levels.cljc contains the main algorithms for updating the state of a
model.  It's written in the kind of "functional programming" style for
which Clojure is designed.  Variables and data structures are never
updated or modified per se.  Rather, each time the system runs, it
generates a new data structure representing the state of the system at a
time.  One can forget the old versions of the data structures, allowing
them to be "garbage collected" by the underlying system (the JVM or
Javascript), or one can store a series of data structures at different
timesteps for later use.

The main top-level function that's defined in level.cljc is called `next-levels`.
Given a sequence of `Level` data structures for a timestep, it produces a
sequence of levels at the next timestep.

In my *free* models (e.g. src/cljs/scalar/free/model.cljs, I often
define a function called `make-stages` or a variable `stages` that
starts from an initial set of data structures, and then uses
`next-levels` with Clojure's `iterate` function to generate a "lazy
sequence" of timestep data structures.  This is a sequence with no fixed
length; you can think of it as infinite.  Initially, the contents of the
sequence are "unrealized"; they don't actually exist. When your code
asks to use elements of the sequence, they are automatially generated as
needed.  For example, when the result of `make-stages` is passed
to plotting code (`plot-level` in the Clojure versions of *free*, or the
plotting code in "plot_pages.cljs", which defines the `free.plot-pages`
namespace or module for the Clojurescript version), as
many timesteps are calculated as needed to plot the number of
timesteps requested by a user.


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
