;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Copyright (c) 2008-2010 Franz Inc.
;; All rights reserved. This program and the accompanying materials
;; are made available under the terms of the Eclipse Public License v1.0
;; which accompanies this distribution, and is available at
;; http://www.eclipse.org/legal/epl-v10.html
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defproject org.clojars.mikehinchey/agraph-clj "4.0.5d-rfe9969-snapshot"
  :description "Clojure client API for Franz AllegroGraph v4"
  :url "http://github.com/mikehinchey/agraph-java-client"
  :source-path "src"
  :jar-dir "dist"
  :warn-on-reflection true
  :manifest {"Implementation-Title" "Clojure client API for Franz AllegroGraph v4"
             "Implementation-Version" "4.0.5d-rfe9969-snapshot"
             "Built-At" #=(str #=(java.util.Date.))
             "Implementation-Vendor" "Franz, Inc"}
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [com.franz/agraph-java-client "4.0.5d"]
                 [org.clojars.mikehinchey/agraph-java-client-test "4.0.5d-rfe9969-snapshot"]
                 ]
  :dev-dependencies [[org.clojure/clojure-contrib "1.2.0"]
                     [swank-clojure "1.2.1"]
                     [lein-clojars "0.6.0"]
                     ;;[ant/ant-launcher "1.6.2"]
                     ])
