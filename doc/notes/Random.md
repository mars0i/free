Notes on random number generators, etc.
===

Agentscript uses an RNG from a Stackoverflow question.  Apart from the
fact that I wouldn't trust an RNG that someone just cooked up or found
somewhere on the Internet without showing any knowledge of the
relevant issues, this particular RNG was criticized in comments in SO.

JavascriptV8, which is apparently used in recent versions of Chrome (and
Opera?), Firefox, and Safari, has a new RNG as of 12/2015 that sounds
like it's high quality, but it's not seedable.  Too bad.  I'd bet it's
fast.  Maybe it's written in something lower-level than Javascript.

Javascript Mersenne Twisters are available from the main Mersenne
Twister site (two, one by Okuda, the other by Uno), from a gist by
McCullough (banksean) in github, and from chancejs.com, by Quinn.
Note that these don't get compiled into anything but more Javascript,
and maybe don't have the optimizations of Sean Rice's
MersenneTwisterFast.java.  chancejs has a minified version, though.  On
the other hand, it does all sorts of stuff I don't need, so what's being
minified is a lotta extra stuff.

http://www.math.sci.hiroshima-u.ac.jp/~m-mat/MT/VERSIONS/JAVASCRIPT/java-script.html
https://gist.github.com/banksean/300494
http://chancejs.com

None of the above RNGs provide distributions other than uniform.

Nope, wrong: Turns out that chance.js is in cljsjs, uses seedable
Mersenne Twisters, *and* it has a Gaussian random numbers function.

This provides lots of distributions:
http://www.statisticsblog.com/2015/10/random-samples-in-js-using-r-functions
It uses a lib called 'crypto'.  Don't know where this is from.
Is it in the browsers?  I don't think this is seedable.


Also cf
https://github.com/jstat/jstat
