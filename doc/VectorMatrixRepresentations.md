VectorMatrixRepresentations
===

In theory I could use raw core.matrix vectors, i.e. 1-D, length n
structures.  However, these will function as row or column matrices
depending on context, and so I would have to be very careful with them
(... to avoid bugs like those I encountered when I first tried this).

Instead, I decided to use nx1 matrices, i.e. true column vectors.
Column vectors are more awkward to write than 1xn row vectors, but
Bogacz used column vectors, not row vectors, and my code is closely
modeled on his math.  In theory I could rewrite levels.clj to make
row vectors the default, but I'd rather keep that code as much like
Bogacz's math as is reasonable.  That will help avoid unintentional
errors.

The new function `free.matrix-arithmetic/col-mat` makes it a little
easier to write column matrices.

Also note that the `*-dt` parameters all need scalars, not vectors or
matrices.
