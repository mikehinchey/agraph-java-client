;; user.clj is run automatically by Clojure

;; The functions below are for convenience when using the REPL.

(defn repl-settings
  []
  ;; these settings will protect you from printing infinite seqs
  (set! *print-length* 50)
  (set! *print-level* 10)
  (use '[clojure stacktrace]))

(defn repl-use-contrib
  []
  (repl-settings)
  (use '[clojure.contrib repl-utils])
  (require '[clojure.contrib.trace :as t]))

(defn repl-use-incanter
  []
  (repl-use-contrib)
  (use '[incanter [core :exclude [log]] stats charts datasets]))
