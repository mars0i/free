# free

Experiments with free energy minimization, i.e. prediction error
minimization, i.e. predictive processing--in the neuroscience sense.

(My starting point is this excellent article:
Rafal Bogacz, "A tutorial on the free-energy framework for modelling
perception and learning", *Journal of Mathematical Psychology*,
Available online 14 December 2015, ISSN 0022-2496,
http://dx.doi.org/10.1016/j.jmp.2015.11.003 .)

## License

This software is copyright 2016 by [Marshall
Abrams](http://members.logical.net/~marshall/), and is distributed under
the [Gnu General Public License version
3.0](http://www.gnu.org/copyleft/gpl.html) as specified in the file
LICENSE, except where noted, or where code has been included that was
released under a different license.

## Running

You should be able to run free either in Clojure or Clojurescript.  I
describe some ways to do this below (in development).

In either Clojure or Clojurescript, you can run it either in scalar
mode or matrix mode.  The first is designed for learning and
processing with single scalar values.  The second allows you to use
vectors and matrices as values; that is, it allows multi-dimensional
learning.  (You can use matrix mode for "scalars" by using 1x1
vectors/matrices, but it will be a lot slower, which might matter in
some situations.)

### To run in Clojure:

You can use `lein repl` but you need additional arguments to specify
scalar or matrix mode.  Note that the plus signs in the commands below
are essential.  They tell Clojure to merge the default profile into
the specified profile, "dev-scalar" or "dev-matrix", which are
specified after the `:profiles` keyword in project.clj.

#### scalar mode

    lein with-profile +dev-scalar repl

#### matrix mode

    lein with-profile +dev-matrix repl


### To run in Clojurescript:

You an delete `rlwrap` below if you don't have that utility installed.
(rlwrap gives you history in the repl prompt.)

You may have to execute one of these once, and then exit out and run
it again to flush out some odd warning.

These might not be working right yet.

#### scalar mode

    rlwrap lein with-profile dev-scalar figwheel

#### matrix mode

    rlwrap lein with-profile dev-matrix figwheel

