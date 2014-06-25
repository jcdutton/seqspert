# seqspert

A Clojure library that exposes the inner workings of Clojure sequence
implementations, giving the user a better understanding of their
workings and thus enabling him/her to write less memory-hungry and
more performant code.

## Usage

<pre>
user=> (use '[seqspert core all])
nil
user=> (use '[clojure.pprint])
nil
user=> ;; an array-map

user=> (decloak {:a 1 :b 2 :c 3 :d 4 :e 5 :f 6 :g 8 :h 9})
#seqspert.array_map.ArrayMap{:array [:e 5 :g 8 :c 3 :h 9 :b 2 :d 4 :f 6 :a 1]}
user=> 

user=> ;; a hash-map

user=> (decloak {:a 1 :b 2 :c 3 :d 4 :e 5 :f 6 :g 8 :h 9 :i 10})
#seqspert.hash_map.HashMap{:count 9, :root #seqspert.hash_map.BitmapIndexedNode{:bitmap "1100001010100100100000000000000", :array [:e 5 nil #seqspert.hash_map.BitmapIndexedNode{:bitmap "100000000010000000000000", :array [:g 8 :c 3 nil nil nil nil]} :h 9 :b 2 nil #seqspert.hash_map.BitmapIndexedNode{:bitmap "10000000000", :array [nil #seqspert.hash_map.BitmapIndexedNode{:bitmap "100000001", :array [:d 4 :f 6 nil nil nil nil]} nil nil nil nil nil nil]} :i 10 :a 1 nil nil]}}
user=> 

user=> (pprint (decloak {:a 1 :b 2 :c 3 :d 4 :e 5 :f 6 :g 8 :h 9 :i 10}))
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

user=> (decloak [:a :b :c :d :e :f :g :h])
#seqspert.vector.Vector{:cnt 8, :shift 5, :root #seqspert.vector.VectorNode{:array [nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil]}, :tail [:a :b :c :d :e :f :g :h]}
user=> 

user=> (pprint (decloak [:a :b :c :d :e :f :g :h]))
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