;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Copyright (c) 2008-2010 Franz Inc.
;; All rights reserved. This program and the accompanying materials
;; are made available under the terms of the Eclipse Public License v1.0
;; which accompanies this distribution, and is available at
;; http://www.eclipse.org/legal/epl-v10.html
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns com.franz.util.util
  "Utility functions."
  )

(defn- find-exeception
  [ce e]
  (loop [#^Throwable e e]
    (cond (nil? e) nil
          (instance? ce e) e
          :else (recur (.getCause e)))))

(defmacro catch-deep
  [ce e & body]
  `(if-let [e# (find-exeception ~ce ~e)]
     (let [~'e e#]
       ~@body)
     (throw ~e)))
