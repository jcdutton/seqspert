# Seqspert

<i>["seekspert"]</i>

## Introduction

The Clojure collection library is built upon the abstraction of a
Sequence.

Whilst abstractions are a good thing in terms of getting useful work
done in simple terms, when it comes to raw performance, they may get
in the way.

Seqspert started life as a set of utils for examining the underlying
implementations and contents of various Clojure Sequence types but is
now growing into a library supporting a number of specific
high-performance, low-churn alternatives to common Sequence-based
operations.

## Build/Install

- git clone https://github.com/JulesGosnell/seqspert.git
- cd seqspert
- lein install

## Overview

Seqspert provides an "inspect" method for transforming the underlying
implementation of a number of Clojure Sequences into a corresponding
Clojure data structure which may then be printed.This aids
comprehension of exactly what is going on under the covers, hopefully
leading to the writing of tighter code.

Seqspert also provides a number of high-performace Sequence related
functions:

<pre>
user=> (use '[seqspert core all])
nil
user=> (use '[clojure.pprint])
nil
</pre>

array-map:
- inspect
<pre>
user=> (pprint (inspect {:a 1 :b 2 :c 3 :d 4 :e 5 :f 6 :g 8 :h 9}))
{:array [:e 5 :g 8 :c 3 :h 9 :b 2 :d 4 :f 6 :a 1]}
nil
user=>
</pre>

hash-map:
- inspect
<pre>
user=> (pprint (inspect {:a 1 :b 2 :c 3 :d 4 :e 5 :f 6 :g 8 :h 9 :i 10}))
{:count 9,
 :root
 {:bitmap "1100001010100100100000000000000",
  :array
  [:e
   5
   nil
   {:bitmap "100000000010000000000000",
    :array [:g 8 :c 3 nil nil nil nil]}
   :h
   9
   :b
   2
   nil
   {:bitmap "10000000000",
    :array
    [nil
     {:bitmap "100000001", :array [:d 4 :f 6 nil nil nil nil]}
     nil
     nil
     nil
     nil
     nil
     nil]}
   :i
   10
   :a
   1
   nil
   nil]}}
nil
user=>
</pre>
- splice-hash-maps

Traditionally the merging of two hash-maps is done via the Sequence
abstraction, reading every key-value-pair from the right hand side and
assoc-ing each one to the left hand side. Unfortunately, this means
that all the work done to reduce a set of keys and values into the
right hand side is thrown away and has to be redone on the left hand
side.

Seqspert's splice-hash-maps function creates a new hash-trie
(underlying representation of a Clojure hash-map) directly from the
overlaying of the right hand side on top of the left hand side in a
single operation, reusing as much of the structure of both maps as
possible and avoiding most associated churn and re-calling of hash()
on keys.

<pre>
user=> (use '[seqspert hash-map])
nil
user=> (def m1 (apply hash-map (range 0 2000000)))
#'user/m1
user=> (def m2 (apply hash-map (range 1000000 3000000)))
#'user/m2
user=> (time (def m3 (merge m1 m2)))
"Elapsed time: 786.300978 msecs"
#'user/m3
user=> (time (def m4 (splice-hash-maps m1 m2)))
"Elapsed time: 180.571396 msecs"
#'user/m4
user=> (= m3 m4)
true
user=> 
</pre>
- into-hash-map

hash-set:
- splice-hash-sets
- into-hash-set

tree-map:
- inspect

tree-set:
inspect

vector:
- inspect
- vector-to-array
- array-to-vector
- vmap
- fjvmap

## Usage

<pre>
user=> ;; an array-map

user=> (inspect {:a 1 :b 2 :c 3 :d 4 :e 5 :f 6 :g 8 :h 9})
#seqspert.array_map.ArrayMap{:array [:e 5 :g 8 :c 3 :h 9 :b 2 :d 4 :f 6 :a 1]}
user=> 

user=> ;; a hash-map

user=> (inspect {:a 1 :b 2 :c 3 :d 4 :e 5 :f 6 :g 8 :h 9 :i 10})
#seqspert.hash_map.HashMap{:count 9, :root #seqspert.hash_map.BitmapIndexedNode{:bitmap "1100001010100100100000000000000", :array [:e 5 nil #seqspert.hash_map.BitmapIndexedNode{:bitmap "100000000010000000000000", :array [:g 8 :c 3 nil nil nil nil]} :h 9 :b 2 nil #seqspert.hash_map.BitmapIndexedNode{:bitmap "10000000000", :array [nil #seqspert.hash_map.BitmapIndexedNode{:bitmap "100000001", :array [:d 4 :f 6 nil nil nil nil]} nil nil nil nil nil nil]} :i 10 :a 1 nil nil]}}
user=> 

user=> (pprint (inspect {:a 1 :b 2 :c 3 :d 4 :e 5 :f 6 :g 8 :h 9 :i 10}))
{:count 9,
 :root
 {:bitmap "1100001010100100100000000000000",
  :array
  [:e
   5
   nil
   {:bitmap "100000000010000000000000",
    :array [:g 8 :c 3 nil nil nil nil]}
   :h
   9
   :b
   2
   nil
   {:bitmap "10000000000",
    :array
    [nil
     {:bitmap "100000001", :array [:d 4 :f 6 nil nil nil nil]}
     nil
     nil
     nil
     nil
     nil
     nil]}
   :i
   10
   :a
   1
   nil
   nil]}}
nil
user=> ;; a vector

user=> (inspect [:a :b :c :d :e :f :g :h])
#seqspert.vector.Vector{:cnt 8, :shift 5, :root #seqspert.vector.VectorNode{:array [nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil]}, :tail [:a :b :c :d :e :f :g :h]}
user=> 

user=> (pprint (inspect [:a :b :c :d :e :f :g :h]))
{:cnt 8,
 :shift 5,
 :root
 {:array
  [nil
   nil
   nil
   nil
   nil
   nil
   nil
   nil
   nil
   nil
   nil
   nil
   nil
   nil
   nil
   nil
   nil
   nil
   nil
   nil
   nil
   nil
   nil
   nil
   nil
   nil
   nil
   nil
   nil
   nil
   nil
   nil]},
 :tail [:a :b :c :d :e :f :g :h]}
nil
user=> 

user=> 
</pre>

## License

Copyright © 2014 Julian Gosnell

Distributed under the Eclipse Public License, the same as Clojure.
