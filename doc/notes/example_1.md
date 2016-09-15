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

### different dt's for different levels

Look at the results for this configuration, which takes makes theta
constant, sigma constant at the bottom level, and makes sigma change
faster than phi does at level 1 (as one of Bogacz's remarks might
suggest).

Level 0:

* phi-dt 0.01
* eps-dt 0.01
* sigma-dt 0.0
* theta-dt 0.0

Level 1:

* phi-dt 0.00001
* eps-dt 0.01
* sigma-dt 0.0001
* theta-dt 0.0

At level 1, sigma goes high (around 2.25), and phi fluctuates around the
region between 1.4 and 1.5.  This configuration is apparently stable.  I
took it out to 200,000 iterations.  The error eps is also stable (at
about -0.67).

Lowering eps at level 1 doesn't change the situation, despite sigma
dropping below zero, temporarily, early on.

Shrinking both phi-dt and sigma-dt at level 1 doesn't change things.

It also works to set level 1 phi-dt = sigma-dt = 0.0001.

(If you make phi-dt even smaller, 0.000001, it looks like it moves toward
a similar end result but it takes a large number of iterations.  I
didn't let this finish--holding onto too big a sequence, maybe.  But
note this is still with sigma-dt at 0.0001.)

So this pattern is fairly robust over variation in the dt's at level 1
level.  The significant thing seems to be making level 1 slower than
level 0.  And letting sigma be constant or changing very slowly at the
bottom.

(One of the questions I had had was whether upper levels need to be
slower than lower levels.  This supports the claim that they should be.)

This seems to work OK with prior means above and below 3.  You will get
rough convergence of phi to somewhere near phi.  Maybe a little higher
or lower if you start with a high or low mean.  However, I sometimes
need to make phi-dt and sigma-dt in level 1 smaller, and then run for
many more ticks.  (And the prior mean seems to have to be > 0.  Maybe
because the target function is to square?)

### theta again

Using one of the configurations alluded to above, I tried enabling
theta again:

(def v-p 1)

Level 0:
    :phi-dt 0.001
    :eps-dt 0.001
    :sigma-dt 0.0
    :theta-dt 0.0}))

Level    1:
    :phi-dt 0.00001
    :eps-dt 0.001
    :sigma-dt 0.00001
    :theta-dt 0.001}))

Then:

    (plot-level stages 1 100000 100)

Very interesting.  theta just tracks phi very closely.  i.e. roughly
theta = phi.  And the curve looks a lot like the one when theta-dt = 0.
The difference is that:

* With theta update disabled (theta-dt = 0), error eps gets higher and higher.

* With theta updating (theta-dt = 0.001), error eps stays around zero.

Ah, by trying other initial v-p's, i.e. prior means, I see that what
theta is doing is converging to a value such that (theta * v-p) = the
value to which phi converges (1.4 or 1.5, approximately).  This behavior
is consistent, and it's also consistent that error remains near zero.
This is what theta was supposed to do (but didn't do, often, when I was
using the same dt's for both levels).

It doesn't matter much what value theta-dt has at level 1.  It will get
there faster or slower, depending, but fast enough, in any event.  When
it's slow, error eps might depart from zero while it's catching up.

It's best, however, if eps-dt is pretty high.  0.01 or 0.001, even 0.1,
are good.  Smaller than 0.001, and theta can become periodic around its
target value.  Apparently it's very sensitive to fluctuations in eps.

Note that it's probably crucial that theta-dt is 0 at the bottom level.
Earlier I had trouble with the level 1 theta update, and I suspect it
was because of enabling either/both sigma and theta update at the
bottom level.  (And this makes sense.  The point of theta is to set a
new prior mean, so to speak.  It doesn't make sense to have this
happening at the bottom level (though it might be appropriate at level
2 and up, if I had a real level 2).

### slow theta?

Maybe theta-dt should be smaller than phi-dt, though.  The point is to
adjust it so that when things vary a lot, you can use as your starting
point a new prior mean.  The point is not to constantly track phi.

It appears though that if theta is slower than (already slow) phi,
sigma also needs to be slow, to avoid it hitting zero before theta can
catch up with phi.
