General notes on level.cljc
===

## Notation

`m*`, `m+`, `m-` are either scalar or matrix `*`, `+`, and `-`, depending on the
value of `use-core-matrix`, which determines which namespace is loaded.
`e*` is `*`, or elementwise matrix multiplication.  `tr` is matrix transpose,
or the identity function for scalars.  `inv` is reciprocal, for scalars,
or matrix inverse, for matrices.  `(id n)` returns an n by n identity matrix,
or 1 for scalars.

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

## `next-level`

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
`next-theta`.  However, at each level, the `phi` from the level above is
*only* used as an argument to the `h` function.  It's `(h +phi)` that
plays a role, never `+phi` alone.  This means that we can simply provide
for the effect of a top level by using a special `h` function that
provides a mean value as if there were some `phi` in the level above it
that it was operating on--when in fact the `h` is just generating an
appropriate value.  So we don't need a special `next-top` function.  The
magic can be (and must be) embedded in the top level.  

*No that doesn't make sense.  Because `h` only operates on a higher-level `phi`.
Oh so maybe just have a special h in the top operating level?  But then what `phi`
do you give to its `h`?  `next-level` will want a level up.  So maybe I do need a
`next-top`??*

*But look at the answer to exercise 3.  Maybe the idea is that you start
with a prior at the top level but then it gets revised.  So maybe I
start with a `phi` at the top level, but update it normally.  And then
the interesting thing is how to deal with `eps` and `theta` at the top
level.  Because updating `phi` at that level will need them.*




Usage examples:
````
(def my-next-levels 
     (partial next-levels 
              my-sensory-input-fn           ; inputs from outside
              (constantly my-prior-means))) ; unchanging priors

(def my-initial-state [bottom middle-1 middle-2 top]) ; four levels

(def states (iterate my-next-levels my-intial-state))

(take 5 states)

(nth states 50)
````

