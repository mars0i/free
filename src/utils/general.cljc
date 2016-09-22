(ns free.general)

(defn domap
  "Like doseq, but eats sequences like map rather than like for."
  ([f coll] (doseq [e coll] (f e)))
  ([f coll1 & colls] (mapv f (cons coll1 colls)) nil))

