On the role of the generative functions h (or g) in levels.cljc
===

### A puzzle

In the answer code for ex 3 in Bogacz, phi in the middle level is
initalized to v_p.  Then to update phi, Bogacz h', i.e.  2*phi.  This is
the derivative of h(phi)=phi^2.  But the error update procedure for the
mid level (_p) uses v_p as is.  i.e. not squared.  So that v_p is a mean
coming down from the top, but it's not passed through h().  h=phi^2 is,
however, used in the error update for the _u level, i.e.  the bottom
level. This could suggest that my treatment of the top is wrong.
However, in (53) and (54), for example, the variables are level-indexed,
but h and h' are not.  What you see there is that the upper phi is
always passed through h in the update of epsilon.  And my formula for
eps-inc in level.cljc is just read off of that.  So in order to run
exercise 3 using functions based on the later, final mathematical model,
the mean at the top should be sqrt(v_p), so that when it's run through
h, what will come out is v_p, i.e. 3.  But note that the middle phi is
still initialized to v_p in Bogacz's code.  This is why there are two
different v_p defs here.  Using sqrt(3) for the top-level phi does
produce a plot that looks like Bogacz's (fig. 2a).  If I use 3 for that
phi, I get a different plot.


### A more general question

Here's a more general question:

In (53) and (54), Bogacz uses h and h' at the same level.  

But in the answer to exercise 3, it appears that h' is used at the upper
level, to calculate the new phi, while the h is only used at the bottom
level, to calculate the error, epsilon.  h isn't used at the upper
level; there v_p is imply passed in as is.  So it looks like h is used
at a level below the level at which h' is used.

This would all make sense if the same h and h' were supposed to be used
at every level.  That would explain why (53) and (54) use the same
letter "h".

*But why should the same `h` function (or `g`) be used at every level?*
That doesn't make sense.  For example, in the illustration with which
the discussion begins in section 2, there is a function g(phi)=g(v)=v^2,
because the light reflected by a round object is a function of its area, 
and we can measure its are with the radius.  However, if there are
higher level functions whose parameters we want to infer, *why should
they also have the form g(v)=v^2*??

The question is, which `h` and `h'` are supposed to correspond?  The
ones within a level, or the `h` at level *n* and the `h'` at level
*n*+1?

### An answer

(53) and (54) suggests the first answer, while the answer to Ex. 3
suggests the second.  Note that `h'` is not even used at the bottom
level, ever, because that's only used to update `phi`, and at the bottom
level `phi`, i.e. `u`, comes from outside.

### A better answer (adopted)

Well, here's a different possible answer:

Since `h` is passed `phi` from the level above the `eps` it's used to
calculate (54), put that `h` *up at that level*--but use the value of
`h(phi)` at the level down from that, as now.  That is, `h` and `h'`
would be matched within the same level, but change `next-eps` so that it
gets the `h` from the level above, along with `phi`.  (And then the `h`
in `eps-inc` should be called `+h`.  *This would then be consistent both
with exercise 3 and (53) and (54).*

I'd need to make similar changes to `next-theta` and `theta-inc`.

This is what I did (9/2016).
