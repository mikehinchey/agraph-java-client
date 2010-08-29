;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Copyright (c) 2008-2010 Franz Inc.
;; All rights reserved. This program and the accompanying materials
;; are made available under the terms of the Eclipse Public License v1.0
;; which accompanies this distribution, and is available at
;; http://www.eclipse.org/legal/epl-v10.html
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns com.franz.util.exec
  "Utility functions for executing threads."
  (:import [java.util.concurrent
            ExecutorService Executors
            ThreadPoolExecutor TimeUnit
            BlockingQueue SynchronousQueue
            ScheduledExecutorService
            Future]))

(defn current-time-ms
  []
  (System/currentTimeMillis))

(defn repeat-while
  "like repeat, but ends with a predicate (which is presumably stateful)"
  [pred x]
  (lazy-seq (when (pred) (cons x (repeat-while pred x)))))

(let [tum {"ns" TimeUnit/NANOSECONDS
           "ms" TimeUnit/MILLISECONDS
           "s" TimeUnit/SECONDS
           "m" TimeUnit/MINUTES
           "h" TimeUnit/HOURS
           "d" TimeUnit/DAYS}]
  (defn time-spec
    "parse a time spec into a TimeUnit:
    3s -> [3 SECONDS]
    5ms -> [5 MILLISECONDS]
    etc."
    [spec]
    (let [spec (if (or (keyword? spec) (symbol? spec))
                 (name spec)
                 spec)
          [n tu] (next (first (re-seq #"(\d*)(.*)" spec)))]
      (if (and n tu)
        [(Long/parseLong n)
         (or (tum tu)
             (throw (RuntimeException. (str "Unable to parse TimeUnit: " tu))))]
        (throw (RuntimeException. (str "Unable to parse TimeUnit spec: " n tu)))))))

(defn nprocs
  []
  (.availableProcessors (Runtime/getRuntime)))

(defn f-get-all
  [futures]
  (doall (map #(.get #^Future %) futures)))

(defn fixed-thread-pool
  [#^Integer n]
  (Executors/newFixedThreadPool n))

(defn single-exec
  []
  (Executors/newSingleThreadExecutor))

(defn submit
  [#^ExecutorService x
   #^Runnable f]
  (.submit x f))

(defn invoke-all
  [#^ExecutorService x
   #^java.util.Collection coll-f]
  (.invokeAll x coll-f))

;; (defn thread-pool-exec
;;   [#^Integer corePoolSize
;;    #^Integer maximumPoolSize
;;    #^BlockingQueue q]
;;   (ThreadPoolExecutor. corePoolSize maximumPoolSize
;;                        10 TimeUnit/SECONDS q))

;; (defn sync-q
;;   [ff continue?]
;;   (let [q (SynchronousQueue.)]
;;     (.start (Thread. (fn sync-q-f []
;;                        (while @continue?
;;                               (.put q (ff))))
;;                      "sync-q"))
;;     q))

;; (defn count-active
;;   [x]
;;   (.getActiveCount #^java.util.concurrent.ThreadPoolExecutor x))

(defn scheduled-pool
  [#^Integer corePoolSize]
  (Executors/newScheduledThreadPool corePoolSize))

(defn scheduled-single
  []
  (Executors/newSingleThreadScheduledExecutor))

(defn periodic-with-delay
  [#^ScheduledExecutorService exec
   #^Long initial-delay
   delay-time-spec
   f]
  (let [[#^Long dn #^TimeUnit dtu] (time-spec delay-time-spec)]
    (.scheduleWithFixedDelay exec f
                             initial-delay dn dtu)))

(defn once-with-delay
  [#^ScheduledExecutorService x
   delay-time-spec
   #^Runnable f]
  (let [[#^Long dn #^TimeUnit dtu] (time-spec delay-time-spec)]
    (.schedule x f dn dtu)))
