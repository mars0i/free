# free

Experiments with free energy minimization, i.e. prediction error
minimization, i.e. predictive processing--in the neuroscience sense.

(My starting point is this excellent article:
Rafal Bogacz, "A tutorial on the free-energy framework for modelling
perception and learning", *Journal of Mathematical Psychology*,
Available online 14 December 2015, ISSN 0022-2496,
http://dx.doi.org/10.1016/j.jmp.2015.11.003 .)

The documentation is a work in progress.  Some of the code is, too.

There is a running demo version of this software here:
http://members.logical.net/~marshall/free/ .

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

In Clojure, you can run it either in scalar
mode or matrix mode.  The first is designed for learning and
processing with single scalar values.  The second allows you to use
vectors and matrices as values; that is, it allows multi-dimensional
learning.  (You can use matrix mode for "scalars" by using 1x1
vectors/matrices, but it will be a lot slower, which might matter in
some situations.)  At present, the Clojurescript version only supports
scalar mode.

### To run in Clojure:

You can use `lein repl` but you need additional arguments to specify
scalar or matrix mode.  There are unix shell scripts in the directory
src/scripts that collect the relevant arguments, if you're using OS X or
Linux or another unix.  For Windows, take a look at the contents of the
scripts and execute a script's last line.  In a unix:

#### scalar mode

    ./src/scripts/matclj

#### matrix mode

    ./src/scripts/scalclj


### To run in Clojurescript:

If you're in Windows, see the note above.  In OS X, Linux, or another
unix, run this script:

    ./src/scripts/scalcljs

If you get an error about rlwrap, either install rlwrap or execute use
the last line of the script in a shell without the word "rlwrap".
rlwrap provides command history for the Clojure repl prompt, so it's
not essential.

To build a version that can be installed on the web, for example, use:

    ./src/scripts/scalcljs-build

This will put the files that make up the application in
resources/public and resources/public/css.

As noted above, there is also a constrained Clojurescript version of the
model ready to run on the web here: https://github.com/mars0i/free .
