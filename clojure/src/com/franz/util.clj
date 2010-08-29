;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Copyright (c) 2008-2010 Franz Inc.
;; All rights reserved. This program and the accompanying materials
;; are made available under the terms of the Eclipse Public License v1.0
;; which accompanies this distribution, and is available at
;; http://www.eclipse.org/legal/epl-v10.html
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns com.franz.util
  "Utility functions."
  (:refer-clojure :exclude [name with-open])
  (:require [clojure.stacktrace :as st]))

(alter-meta! *ns* assoc :author "Franz Inc <www.franz.com>, Mike Hinchey <mhinchey@franz.com>")

(defmulti name
  "Shadows clojure.core/name to make it an extensible method."
  type)

(defmethod name :default [x] (.getName x))

;; same as the clojure.core/name fn
(defmethod name clojure.lang.Named [#^clojure.lang.Named x] (clojure.core/name x))

(defmulti close
  "Used by with-closeable in a finally block to close objects.
Methods are defined for java.io.Closeable and a default for (.close) by reflection.
May be extended for differently named close methods."
  type)

(defmethod close :default
  [obj]
  (when obj (.close obj)))

(defmethod close java.io.Closeable
  [#^java.io.Closeable obj]
  (.close obj))

(defmethod close com.franz.util.Closeable
  [#^com.franz.util.Closeable obj]
  (.close obj))

;; *scope* is a stack or an atom containing a stack
;; of objects for which (open) is called and (close) is to be called
;; at the end of a (scope) block.
(defonce *scope* nil)

(defn close-all
  "Not intended to be used other than by scope.
  Calls close on all objects in open-stack, catches and prints any exceptions."
  [open-stack]
  (when (seq open-stack)
    (try
      (close (first open-stack))
      (catch Throwable e
        (binding [*out* *err*]
          (print "Ignoring exception from close: " e)
          (st/print-cause-trace e))))
    (recur (next open-stack))))

(defn open
  "Register obj to be closed before the enclosing with-open exits.
   Must be called within the context of with-open.
   Returns the same obj."
  [obj]
  (when-not *scope*
    (throw (Exception. "Not within a scope. Use com.franz.util/scope.")))
  (if (instance? clojure.lang.Atom *scope*)
    (when (and obj (not (some #{obj} @*scope*)))
      (swap! *scope* conj obj))
    (when (and obj (not (some #{obj} *scope*)))
      (set! *scope* (conj *scope* obj))))
  obj)

(defmacro scope
  "Similar to clojure.core/with-open, but also closes objects for which (open) was called within the body.

  A single try/finally is used around the body.

  All exceptions thrown by close methods will be caught and printed to System/err.
  For different behavior, use a binding on close to catch exceptions.

  Example: (scope (... (open (FileReader. x))))"
  [& body]
  `(binding [*scope* ()]
     (try
       ~@body
       (finally
        (close-all *scope*)))))

(defmacro with-open
  "See (scope).  Similar to clojure.core/with-open, but also closes objects
  for which (open) was called within the body.

  The bindings are wrapped by a call to open.
  In the finally, close-all is called, closing all opened objects in reverse order.

  Except for the different behavior of catching exceptions from close,
  this can replace clojure.core/with-open.

  Example: (with-open [f (FileReader. x)] ... )"
  [bindings & body]
  `(scope (let ~(into []
                      (mapcat (fn [[b v]]
                                (if (symbol? b)
                                  [b `(open ~v)]
                                  (throw
                                   (IllegalArgumentException.
                                    (str "with-open: binding must be a symbol: " b)))))
                              (partition 2 bindings)))
            ~@body)))

(defmacro scope-let
  "Establishes a scope and a plain let form."
  [let-bindings & body]
  `(scope
     (let ~let-bindings
       ~@body)))

(defmacro scope1
  "Establishes a scope only if there is not already one open.
  This allows one fn to return an object that will still be open only if
  the caller opened its own scope.

  For example:
  (scope
     (let [resource (scope1
                       (open ...))]
        (process resource)))"
  [& body]
  `(let [scope1# (fn [] ~@body)]
     (if (or (nil? *scope*) (instance? clojure.lang.Atom *scope*))
       (scope
         (scope1#))
       (scope1#))))

(defmacro scope1-let
  "Establishes a scope1 (a scope if there isn't one already) and a plain let form."
  [let-bindings & body]
  `(scope1 (let ~let-bindings
             ~@body)))

(defn root-scope []
  "For use in the REPL. Opens a scope that will only be closed with root-close!
  Note that scope1 will not reuse this scope, but still open a new one."
  (alter-var-root #'*scope* (fn [cur] (or cur (atom ())))))

(defn root-close []
  "For use in the REPL to close resources opened since (root-scope) was called."
  (when (instance? clojure.lang.Atom *scope*)
    (close-all @*scope*)
    (alter-var-root #'*scope* (fn [_] nil))))

(defn printlns
  "println each item in a collection"
  [col]
  (doseq [x col]
    (println x)))

(defn read-lines
  "Calls clojure.core/line-seq, but f is a File and (open) is called on the
  reader that is created, so read-lines must be called with a (scope)."
  [#^java.io.File f]
  (line-seq (open (java.io.BufferedReader. (open (java.io.FileReader. f))))))

(defn write-lines
  [#^java.io.File f
   lines]
  (with-open [out (java.io.PrintWriter. (java.io.FileWriter. f))]
    (doseq [ln lines]
      (.println #^java.io.PrintWriter out ln))))
