General notes on level.cljc
===

`m*`, `m+`, `m-` are either scalar or matrix *, +, and -, depending on the
value of use-core-matrix, which determines which namespace is loaded.
`e*` is *, or elementwise matrix multiplication.  `tr` is matrix transpose,
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
