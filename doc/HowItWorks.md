How it works
===

Q: How is that the system in levels.cljc can compute averages from
noisy data?

A: Each input pushes the values in some direction--but only a little
bit, because of the `-dt` paramters.  If you get a lot of points that
that are probabilistically distributed, the little pushes end up
adding up to values close to an average.

This also clarifies why smaller `dt` parameters give smoother
approaches to the average.
