Notes on Feldman and Friston 2010
===

1. F&F use a more general framework than Bogacz's which requires that
the params theta to the generative function at the second level be a
linear multiplier, rather than possibling something more complex.

2. F&F's addition of random distributions to generative functions, in
effect, is easy to add in a model file, if what this means is just
adding noise whenever a value is generated from `phi` as input.  Even at
the top level, `phi` goes through a generative function in `epsilon-inc`
and `theta-inc`; `phi` isn't used on its own.

3. If the hidden values *x* are just additional values at the second
level, i.e. part of a vector, then I just need to encode their update
in the generative function, I think.

4. On making `sigma` depend on "hidden" values: This requires replacing
`sigma-inc`, I
   think, but mainly only that, I think.  `next-sigma` and `next-level`
   might have to be modified to take additional level arguments, since
   right now it only expects the current level.  (Or I could just always
   pass all three levels whether they're used or not, but then it's less
   clear what's going on (though using `_` as some of the parameters
   would help).)
