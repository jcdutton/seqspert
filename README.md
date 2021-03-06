# Seqspert

<i>["seekspert"]</i>

[![Continuous Integration status](https://secure.travis-ci.org/JulesGosnell/seqspert.png)](http://travis-ci.org/JulesGosnell/seqspert)

## Introduction

The Clojure collection library is built upon the abstraction of a
Sequence.

Whilst abstractions are a good thing in terms of getting useful work
done in simple terms, when it comes to raw performance, they sometimes
get in the way. e.g. Sequences present as a linear structure
supporting one-by-one element addition when perhaps the underlying
tree-based implementation of some sequences is better suited to
parallelism and more efficient bulk-updates.

Seqspert started life as a set of utils for examining and
understanding the underlying implementations and contents of various
Clojure Sequence types but is now growing into a library supporting a
number of specific high-performance, low-churn alternatives to common
Sequence-based operations.

Seqspert contains both Java and Clojure code which is thoroughly unit
tested on every
[build](http://ouroboros.dyndns-free.com/ci/job/seqspert/).

I have just put a recent snapshot up on Clojars. It should be run on
java-7/8 and clojure-1.7.0-alpha4. Any feedback would be most
appreciated.

## Either: Lein

[seqspert "1.7.0-alpha4.1.0-SNAPSHOT"]

e.g.

just put this in a project.clj file and in the same dir type 'lein
repl'. Then cut and paste in some of the examples below :

```clojure
(defproject my-stuff "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0-alpha4"]
                 [seqspert "1.7.0-alpha4.1.0-SNAPSHOT"]]
  :main ^:skip-aot my-stuff.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
```
## Or: Build/Install

- git clone https://github.com/JulesGosnell/seqspert.git
- cd seqspert
- lein install

## Overview

Seqspert provides a number of high-performance Sequence related
functions:

### "splicing" hash maps

Traditionally the merging of two hash-maps is done via the Sequence
abstraction, reading every key-value-pair from a right hand side map
and assoc-ing each one into a left hand side map. Unfortunately, this
means that all the work done to reduce a set of keys and values into
the right hand side is thrown away and has to be redone on the left
hand side.

Seqspert's splice-hash-maps function creates a new hash-trie
(underlying representation of a Clojure hash-map) directly from the
overlaying of the right hand side on top of the left hand side in a
single operation, reusing as much of the structure of both maps as
possible and avoiding work such as re-calling of hash() on keys.

Since hash-tries are a form of tree, Seqspert can go a step further by
doing the splicing in parallel, each subtree being handed off to a
different thread and then the results being gathered back into a
single hash-trie. This can yield substantial performance benefits.

As much of the structure of the maps involved is reused and a lot of
the code that implements the Sequence abstraction is bypassed, heap
churn is also reduced to a large extent.

![Alt text](https://raw.github.com/JulesGosnell/seqspert/master/images/splice-hash-maps.gif)

```clojure
user=> (def m1 (apply hash-map (range 0 2000000)))  ;; create a map with 1M entries
#'user/m1
user=> (def m2 (apply hash-map (range 1000000 3000000))) ;; create an intersecting map
#'user/m2
user=> (time (def m3 (merge m1 m2))) ;; traditional
"Elapsed time: 398.815137 msecs"
#'user/m3
user=> (use '[seqspert hash-map])
nil
user=> (time (def m4 (sequential-splice-hash-maps m1 m2))) ;; seqspert
"Elapsed time: 188.270844 msecs"
#'user/m4
user=> (time (def m5 (parallel-splice-hash-maps m1 m2))) ;;  seqspert
"Elapsed time: 25.401196 msecs"
#'user/m5
user=> (= m3 m4 m5) ;; verify results
true
```

### "splicing" hash sets

Clojure hash-sets are implemented using an underlying hash-map in
which each set element is both the key and the value in map
entry. This means that Seqspert can leverage all the work done on
splicing hash-maps to splice hash-sets as well:

![Alt text](https://raw.github.com/JulesGosnell/seqspert/master/images/splice-hash-sets.gif)

```clojure
user=> (def s1 (apply hash-set (range 0 1000000)))  ;; create a set with 1M entries
#'user/s1
user=> (def s2 (apply hash-set (range 500000 1500000))) ;; create an intersecting set
#'user/s2
user=> (use '[clojure set])
nil
user=> (time (def s3 (union s1 s2))) ;; traditional
"Elapsed time: 337.050528 msecs"
#'user/s3
user=> (use '[seqspert hash-set])
nil
user=> (time (def s4 (sequential-splice-hash-sets s1 s2))) ;; seqspert
"Elapsed time: 158.255666 msecs"
#'user/s4
user=> (time (def s5 (parallel-splice-hash-sets s1 s2))) ;; seqspert
"Elapsed time: 28.837984 msecs"
#'user/s5
user=> (= s3 s4 s5) ;; verify results
true
```

### vector to vector function mapping 

If you are trying to write performant code in Clojure, vectors are a
good thing.

* vector related functions are generally eager (mapv) rather than
lazy (map) - laziness involves thread coordination which can be an
unwelcome overhead when you don't need it. It also makes it more
difficult to work out on which thread the work is actually being done.

* a vector's internal structure is more compact than e.g. a
linked-list, meaning less churn and maybe better [mechanical
sympathy](http://mechanical-sympathy.blogspot.co.uk/).

* a vector's api supports random access so it can be cut into smaller
pieces for processing in parallel whereas e.g. a linked-list does not
and therefore cannot.

Traditionally a vector is mapv-ed into another vector via the Sequence
abstraction. Each element of the input vector has the function applied
and is then conj-ed onto the output vector.

Seqspert works directly on the underlying structure of the vector, a
tree. vmap walks the tree without having to expend any cycles making
it look like a vector, calling the function on all its leaves and
efficiently building an output tree of exactly the same dimensions as
the original with no need to resize repeatedly as it is built in a
single operation.

fjvmap does the same thing but in parallel, passing each subtree to a
fork-join pool then finally reconstituting them into a vector, thus
not only the function application but also the building of the output
vector is done in parallel.:

![Alt text](https://raw.github.com/JulesGosnell/seqspert/master/images/map-vector.gif)

```clojure
user=> (def v1 (vec (range 10000000)))
#'user/v1
user=> (def v2 (time (mapv identity v1)))
"Elapsed time: 166.884981 msecs"
#'user/v2
user=> (use '[seqspert.vector])
nil
user=> (def v3 (time (vmap identity v1)))
"Elapsed time: 106.545886 msecs"
#'user/v3
user=> (def v4 (time (fjvmap identity v1)))
"Elapsed time: 22.778568 msecs"
#'user/v4
user=> (= v1 v2 v3 v4)
true
user=>
```
### vector to array copy

vector-to-array hands off subtrees and array offsets to different
threads allowing a vector to be copied into an array in parallel.

![Alt text](https://raw.github.com/JulesGosnell/seqspert/master/images/vector-to-array.gif)

```clojure
user=> (def v1 (vec (range 10000000)))
#'user/v1
user=> (time (def a1 (into-array Object v1))) ;; traditional
"Elapsed time: 715.439909 msecs"
#'user/a1
user=> (use '[seqspert.vector])
nil
user=> (time (def a2 (vector-to-array v1))) ;; seqspert
"Elapsed time: 17.8768 msecs"
#'user/a2
user=> (= (seq a1)(seq a2))
true
user=>
```

### array to vector copy

array-to-vector does the same thing in reverse. As with fjvmap, not
only the copying but also the building of the output vector is done in
parallel.

![Alt text](https://raw.github.com/JulesGosnell/seqspert/master/images/array-to-vector.gif)

If you are performing large vector/array/vector copies then you might
like to benchmark these functions.

```clojure
user=> (def a1 (into-array Object (range 100000000)))
#'user/a1
user=> (def v1 (time (vec a1))) ;; traditional
"Elapsed time: 513.214568 msecs"
#'user/v1
user=> (use '[seqspert.vector])
nil
user=> (def v2 (time (array-to-vector a1))) ;; seqspert
"Elapsed time: 182.745605 msecs"
#'user/v2
user=> (= v1 v2)
true
user=>
```
### data-structure inspection

Seqspert also provides an "inspect" method for transforming the
underlying implementation of a number of Clojure Sequences into a
corresponding Clojure data structure which may then be
print()-ed. This aids comprehension of exactly what is going on under
the covers. Understanding this is helpful in debugging Seqspert and
learning to use Clojure's collections in an efficient and performant
way.

```clojure
user=> (use '[seqspert core all])
nil
user=> 

user=> (inspect (array-map :a 1 :b 2 :c 3))
#seqspert.array_map.ArrayMap{:array [:a 1 :b 2 :c 3]}
user=> 

user=> (inspect (hash-map :a 1 :b 2 :c 3))
#seqspert.hash_map.HashMap{:count 3, :root #seqspert.hash_map.BitmapIndexedNode{:bitmap "1000000010000100000000000000000", :array [:c 3 :b 2 :a 1 nil nil]}}
user=> 

user=> (inspect (sorted-map :a 1 :b 2 :c 3))
#seqspert.tree_map.TreeMap{:tree #seqspert.tree_map.TreeMapBlackBranchVal{:key :b, :val 2, :left #seqspert.tree_map.TreeMapBlackVal{:key :a, :val 1}, :right #seqspert.tree_map.TreeMapBlackVal{:key :c, :val 3}}, :_count 3}
user=> 

user=> (inspect (hash-set :a :b :c))
#seqspert.hash_set.HashSet{:impl #seqspert.hash_map.HashMap{:count 3, :root #seqspert.hash_map.BitmapIndexedNode{:bitmap "1000000010000100000000000000000", :array [:c :c :b :b :a :a nil nil]}}}
user=> 

user=> (inspect (sorted-set :a :b :c))
#seqspert.tree_set.TreeSet{:impl #seqspert.tree_map.TreeMap{:tree #seqspert.tree_map.TreeMapBlackBranchVal{:key :b, :val :b, :left #seqspert.tree_map.TreeMapBlackVal{:key :a, :val :a}, :right #seqspert.tree_map.TreeMapBlackVal{:key :c, :val :c}}, :_count 3}}
user=> 

user=> (inspect (vector :a :b :c :d))
#seqspert.vector.Vector{:cnt 4, :shift 5, :root #seqspert.vector.VectorNode{:array [nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil]}, :tail [:a :b :c :d]}
user=> 

user=> (inspect (subvec (vector :a :b :c :d) 1 2))
#seqspert.vector.SubVector{:v #seqspert.vector.Vector{:cnt 4, :shift 5, :root #seqspert.vector.VectorNode{:array [nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil]}, :tail [:a :b :c :d]}, :start 1, :end 2}
```

## Disclaimer

Your mileage may vary !

The results detailed above were collected on 16x 3.30ghz,
linux-3.11.0-26, Oracle 1.8.0-ea-b119 and clojure-1.7.0-alpha3 and are
not indicative of anything else. It is unlikely that this is exactly
the same combination of h/w, s/w and data that constitute your
production platform. ALWAYS test and test again until you are
satisfied that, in your particular usecase, a seqspert function
provides you with a significant performance win before adopting it.

## Further Reading

- hash maps / sets - [Phil Bagwell's "Ideal Hash Trees"](http://lampwww.epfl.ch/papers/idealhashtrees.pdf)

- vectors - I can't find a good reference, but ["RRB-Trees: Efficient Immutable Vectors"](http://infoscience.epfl.ch/record/169879/files/RMTrees.pdf) presents a similar but enhanced data-structure available for Clojure as [core.rrb-vector](https://github.com/clojure/core.rrb-vector).

## License

Copyright © 2014 Julian Gosnell

Distributed under the Eclipse Public License, the same as Clojure.
