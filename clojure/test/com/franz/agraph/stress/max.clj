;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Copyright (c) 2008-2010 Franz Inc.
;; All rights reserved. This program and the accompanying materials
;; are made available under the terms of the Eclipse Public License v1.0
;; which accompanies this distribution, and is available at
;; http://www.eclipse.org/legal/epl-v10.html
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  ;; Run from shell:
  ;; ant test-max

  ;; Usage, in the REPL:
  (require :reload-all '[com.franz.agraph.stress [max :as m]])
  (def g (go 1 "maxstats.csv"))
  
  ;; use with incanter:
  (def xy (let [[sx sy] (m/stats-xy @(:stats g) :size)
                [ax ay] (m/stats-xy @(:stats g) :add)
                xy (xy-plot x y :series-label "size" :legend true :group-by ay)]
            ;;(add-lines xy x (map #(* 10 %) y) :series-label "add")
            (view xy)
            xy))
  )

(ns com.franz.agraph.stress.max
  "Stress AGraph"
  (:refer-clojure :exclude (name))
  (:import [test AGAbstractTest]
           [org.openrdf.model.vocabulary RDF XMLSchema]
           [test.stress Events]
           )
  (:use [com.franz util openrdf agraph]
        [com.franz.util exec]
        ))

(declare server cat repo vf)

(defn fwith-ag-cat
  [f]
  (scope-let [server1 (or (when (bound? server) server)
                          (ag-server {:url (AGAbstractTest/findServerUrl)
                                      :username (AGAbstractTest/username)
                                      :password (AGAbstractTest/password)}))
              cat1 (or (when (bound? cat) cat)
                       (ag-catalog server1 AGAbstractTest/CATALOG_ID))]
             (binding [server server1
                       cat cat1]
               (scope-let []
                          (.ping (ag-repo-con cat1 "clj-test-max")))
               (f))))

(defn fwith-ag-con
  [f]
  (scope-let [repo1 (ag-repo-con cat "clj-test-max"
                                 {:auto-commit false})
              vf1 (value-factory repo1)]
             (binding [repo repo1
                       vf vf1]
               (let [r (f)]
                 (.commit repo)
                 r))))

;; code below uses bound-fn so these vars and others will be bound across threads
(declare run? -log stats conf)

(defmacro r
  "for use in the repl only, opens AG"
  [& body]
  `(binding [run? (atom true)
             -log (atom [])
             stats (atom [])]
     (fwith-ag-cat #(fwith-ag-con (fn r-body [] ~@body)))))

(defn log
  [x]
  (swap! -log conj x))

(defn stat
  [stat]
  (swap! stats conj stat)
  stat)

(defn spit-stats
  [stats f]
  (let [stats (sort (comparator #(< (:start %1) (:start %2)))
                    stats)
        start0 (:start (first stats))]
    (write-lines f (cons (format "%s\t%s\t%s\t%s" "start" "size" "ms" "add")
                         (map (fn [{:keys [ms add size start]}]
                                (format "%d\t%d\t%d\t%d" (- start start0) size ms add))
                              stats)))))

(defn stats-xy
  [stats key]
  (let [stats (sort (comparator #(< (:start %1) (:start %2)))
                    stats)
        start0 (:start (first stats))
        x (map #(- (:start %) start0) stats)
        y (map key stats)]
    [x y]))

(defn stats-table
  [stats]
  (let [stats (sort (comparator #(< (:start %1) (:start %2)))
                    stats)
        start0 (:start (first stats))]
    (cons ["start" "size" "ms" "add"]
          (map (fn [{:keys [start size ms add]}]
                 [(- start start0) size ms add])
               stats))))

(defn add-some!
  [conf]
  (fwith-ag-con
    #(let [stmts (Events/makeTriples vf (conf :triples-per-event) (conf :events))
           start (current-time-ms)]
       (add-all! repo stmts)
       (stat {:ms (- (current-time-ms) start)
              :add (count stmts)
              :size (repo-size repo)
              :start start}))))

(defn run-adders
  []
  (while @run?
     (fwith-ag-cat
       (fn []
         (log [:xxx (:threads (first @conf))])
         (let [conf1 (first @conf)
               x (fixed-thread-pool (conf1 :threads))
               fs (invoke-all x (repeat (conf1 :threads)
                                        (bound-fn [] (log conf1)
                                                  (try
                                                   (while (and @run?
                                                               (= conf1 (first @conf)))
                                                          (add-some! conf1))
                                                   (catch Throwable e (log e) (throw e)))
                                                  )))]
           (log fs)
           {:x x :f fs})))))

(defn go
  [end-after-minutes out-file]
  (binding [run? (atom true)
            -log (atom [])
            stats (atom [])
            conf (atom (list {:triples-per-event 50 :events 1 :threads (* 2 (nprocs))}
                             {:triples-per-event 50 :events 2 :threads (* 2 (nprocs))}
                             {:triples-per-event 50 :events 3 :threads (* 2 (nprocs))}
                             {:triples-per-event 50 :events 4 :threads (* 2 (nprocs))}
                             ))
            ]
    (let [x (scheduled-pool 2)
          adders (submit x (bound-fn []
                                     (run-adders)))
          reconf-s (int (* 60 (/ end-after-minutes (count @conf))))
          reconf (periodic-with-delay x reconf-s (str reconf-s "s")
                   (bound-fn []
                             (log :reconf)
                             (swap! conf pop)))
          spit (periodic-with-delay x 1 "2s"
                 (bound-fn [] (spit-stats @stats (java.io.File. out-file))))
          killer (once-with-delay x (str end-after-minutes "m")
                   (bound-fn []
                             (log :end)
                             (.cancel adders false)
                             (reset! run? false)))]
      {:x x :f adders :run? run? :killer killer
       :stats stats :log -log :conf conf})))

(defn get-all
  [go]
  ;;(map #(.get % 1000)
  (.get (:f go)))

(defn stop
  [go]
  (binding [-log (:log go)] (log :stop))
  (reset! (go :run?) false))
