(ns seqspert.hash-set
  (:import [java.lang.reflect Field]
           [clojure.lang
            PersistentHashSet
            APersistentSet]
           [clojure.lang Seqspert])
  (:require [clojure.core [reducers :as r]])
  (:use [seqspert core]))

(let [^Field f (unlock-field APersistentSet "impl")]  (defn hash-set-impl  [n] (.get f n)))

(defrecord HashSet [impl])

(defmethod inspect PersistentHashSet [^PersistentHashSet s]
  (HashSet. (inspect (hash-set-impl s))))

(defn splice-hash-sets
  "merge two hash-sets resulting in a third equivalent to the first
  with ever element from the second conj-ed into it."
  [l r]
  (Seqspert/spliceHashSets l r))

(defn into-hash-set
  "parallel fold a sequence into a hash-set"
  ([values]
     (r/fold (r/monoid splice-hash-sets hash-set) conj values))
  ([parallelism values]
     (r/fold parallelism (r/monoid splice-hash-sets hash-set) conj values)))
