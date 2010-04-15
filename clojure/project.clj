;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Copyright (c) 2008-2010 Franz Inc.
;; All rights reserved. This program and the accompanying materials
;; are made available under the terms of the Eclipse Public License v1.0
;; which accompanies this distribution, and is available at
;; http://www.eclipse.org/legal/epl-v10.html
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defproject com.franz/agraph-clj "3.2-SNAPSHOT"
  :description "Clojure client API for Franz AllegroGraph 3.2 and 3.3"
  :url "http://github.com/franzinc/agraph-java-client"
  :namespaces [com.franz.agraph]
  :dependencies [[org.clojure/clojure "1.1.0"]
                 ;;[org.clojure/clojure-contrib "1.1.0"]
                 [com.franz/agraph-java-client "3.2"]]
  :dev-dependencies [[swank-clojure "1.1.0"]
                     ;;[lein-clojars "0.5.0-SNAPSHOT"]
                     ;;[ant/ant-launcher "1.6.2"]
                     ])
