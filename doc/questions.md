Questions
===

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

Well, it turns out that in practice, if phi-dt and sigma-dt at level 1
are significantly slower than phi-dt at level 0, you consistently get
behavior that's what you'd expect from the model.  Otherwise, it
depends.
