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

### dt

The farther the initial hypothesis is from the mean of the inputs, the
smaller `dt` must be.

### sigma-dt

`sigma-dt` must be smaller than `dt`.

If the phi curve is too wavy, you can make it more stable by shrinking dt.

If sigma is going down to zero too quickly, you can slow it down by
making sigma-dt smaller.

### true value?

The value around which phi (at level 1) more or less settles is between
the true mean value and the original prior mean.  (Note that the "true
mean value" is the value such that if run through g(), i.e.  h() and
theta, will produce the mean of the input distribution.  That is, it's
the inverse of g applied to the mean of the inputs.)

Question: Why doesn't the adjustment of theta fix this problem?

### theta-dt

If theta-dt is too large (e.g. 0.1), weird things happen.

If theta-dt is too small, e.g. like this

    (def dt 0.001) ; for phi and eps
    (def sigma-dt 0.0001)
    (def theta-dt 0.00001)

or with theta-dt = 0, phi seems to be going to a sensible region, but
then it fluctates wildly and gooes much higher.  Meanwhile the variance
is going *up*, not down to zero, and the error remains far from zero.

If theta-dt is betweem 0.0001 in the above scenarios 0.001  to 0.000001,
under 10K steps, then things look OK *but* the smaller theta-dt is, the
lower is the value which phi overs around.  Which seems wierd.  Not sure
if this is relative to the simple dt, or to sigma-dt.

### re exercises 1, 2, 3

If you set sigma-dt and theta-dt to zero, i.e. keep sigma and theta
fixed at their initial values, then what you get from phi is pretty much
just what you get from exercise 3 (or 2), i.e. settling on a value near
1.6.   (See the note after exercise 1, which describes this as the
correct value, since it's got the highest probability.)  i.e. to get
this, you have to normalize the number of ticks, so that if dt=0.01, you
have 1000 ticks, if dt=0.001, 10k ticks, etc.  You are just dividing up
the same amount of time by smaller in subintervals.  This is all true
*even though we are using random inputs with mean 2, rather than a fixed
input 2 as in exercise 3.*  Whether you set dt to 0.01, 0.001, 0.0001
doesn't matter--it's a little smoother as you make the numbers smaller,
but the phi and eps curves are roughly the same in each case.  And if
you let it run longer, it seems to persist, modulo small fluctuations.

So *if we keep sigma and theta from evolving*, the system works perfectly.
At least.  If you allow sigma or theta to evolve, you get some of the
puzzling effects described above.
