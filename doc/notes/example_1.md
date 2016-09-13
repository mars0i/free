notes on example-1
====

Consider this, with the config as of 9/12, 9/13:

    (def dt 0.01) ; for phi and eps
    (def sigma-dt 0.0001)
    (def theta-dt 0.001)

This works OK.  phi at level 1 hovers around 1.7, which is between the
true mean wrt level 0, which is sqrt(2)=1.4, and the prior mean, which
is 3 (and not sqrt(3) (??)).

The eps hovers around zero, which is good.  The sigma gradually goes
down to zero.

However, when it gets down near zero, phi and eps at level 1 start
fluctuating more.  eps is not being scaled enough by sigma.  It really
should be higher.

By contrast, this looks better, at least at first:

    (def dt 0.001) ; for phi and eps
    (def sigma-dt 0.0001)
    (def theta-dt 0.001)

phi, eps, and theta settle down, and the variation stays small as
sigma goes toward zero.

On the other hand, phi is hovering around a very high value: 2.5.

Don't do this:

    (def dt 0.0001) ; for phi and eps
    (def sigma-dt 0.0001)
    (def theta-dt 0.0001)

This makes phi fluctuate in a large loping sine wave-style shape.
It appears that the sigma-dt has to be a lot smaller than that for phi
and eps.

## rules of thumb

Or rather, apparent or possible rules of thumb.

The farther the initial hypothesis is from the mean of the inputs, the
smaller `dt` must be.

`sigma-dt` must be smaller than `dt`.

If the phi curve is too wavy, you can make it more stable by shrinking dt.

If sigma is going down to zero too quickly, you can slow it down by
making sigma-dt smaller.

The value around which phi (at level 1) more or less settles is between
the true mean value and the original prior mean.  (Note that the "true
mean value" is the value such that if run through g(), i.e.  h() and
theta, will produce the mean of the input distribution.  That is, it's
the inverse of g applied to the mean of the inputs.)

Question: Why doesn't the adjustment of theta fix this problem?
