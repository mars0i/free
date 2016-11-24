aljabr notes
====

### IndexSeq's meta arg

I was getting an error from aljabr about a missing argument for
IndexedSeq.  This is in the first several lines of aljabr.core.
It occurs because in cljs 1.8.34, a 'meta' arg was added to
the deftype IndexedSeq in cljs.core.  aljabr 0.1.1's project.clj
uses a Clojurescript 1.7 version.  However, I don't want to use
that version because I've got spec stuff in some of my files, so
I need a 1.9.

Here's the Clojurescript changelog line:

CLJS-1569: IndexedSeq doesn't implement IWithMeta / IMeta

Indeed if you scroll down from the top of the IndexedSeq def in
cljs.core, you see it implements these protocols, also defined in
cljs.core.  If you look at these defs, they are there to store and
return metadata of an object.  So it seems it's safe to just pass nil
as meta, as several examples in cljs.core do.

So in my personal snapshot of aljbar, I've added a third arg, nil to
the IndexedSeq constructor in aljbar.core.
