;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Copyright (c) 2008-2010 Franz Inc.
;; All rights reserved. This program and the accompanying materials
;; are made available under the terms of the Eclipse Public License v1.0
;; which accompanies this distribution, and is available at
;; http://www.eclipse.org/legal/epl-v10.html
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  ;; Tutorial code for using Franz AllegroGraph with Clojure.
  
  ;; Usage:

  ;; In the source distribution of agraph-java-client,
  ;; in the clojure directory...
  ;;
  ;; Next, download all dependencies:
  ;; > lein deps
  ;;
  ;; Start the repl:
  ;; > lein repl
  ;;
  ;; Or, start the repl for Emacs/Slime:
  ;; > lein swank
  ;;
    
  ;; Load this file:
  (require 'com.franz.agraph.tutorial)

  ;; Change to this namespace:
  (in-ns 'com.franz.agraph.tutorial)
  
  ;; You may want to set your own connection params:
  (def *connection-params* {:url "http://localhost:8080"
                            :username "test" :password "xyzzy"
                            :catalog "java-catalog"
                            :repository "cljtutorial"})

  ;; Optional, for convenience in the REPL:
  (use '[clojure stacktrace])
  (use '[clojure.contrib repl-utils trace])

  ;; Execute the examples: test1-test16
  (example1)
  (examples-all)
  )

(ns com.franz.agraph.tutorial
  "Tutorial code for using Franz AllegroGraph from the Clojure language.
  Follows the Python and Java tutorial code."
  (:refer-clojure :exclude [name with-open])
  (:import [com.franz.agraph.repository
            AGCatalog AGQueryLanguage AGRepository
            AGRepositoryConnection AGServer AGValueFactory]
           [org.openrdf.model ValueFactory Resource Literal]
           [org.openrdf.model.vocabulary RDF XMLSchema]
           [org.openrdf.query QueryLanguage]
           [org.openrdf.query.impl DatasetImpl]
           [org.openrdf.rio RDFFormat RDFHandler]
           [org.openrdf.rio.ntriples NTriplesWriter]
           [org.openrdf.rio.rdfxml RDFXMLWriter]
           [java.io File OutputStream FileOutputStream])
  (:use [com.franz util openrdf agraph]))

(alter-meta! *ns* assoc :author "Franz Inc <www.franz.com>, Mike Hinchey <mhinchey@franz.com>")

;; Clojure equivalents of the Python and Java tests are below.
;; You may use agclj.sh to start the Clojure REPL in a console,
;; or in Emacs/slime/swank-clojure.

;; Note, you can also try the Java tutorial in the Clojure REPL.
;; However, in Slime/swank-clojure, the System.out prints to the
;; *inferior-lisp* buffer not the Slime REPL, so it may be easier to
;; use that instead of slime.
;; (import 'tutorial.TutorialExamples)
;; (TutorialExamples/test1)

(defonce *connection-params* {:url "http://localhost:8080"
                              :username "test" :password "xyzzy"
                              :catalog "java-catalog"
                              :repository "cljtutorial"})

(def *agraph-java-tutorial-dir* (or (System/getProperty "com.franz.agraph.tutorial.dir")
                                    (.getCanonicalPath (java.io.File. "../src/tutorial/"))))

(defn example1
  "lists catalogs and more info about the scratch catalog."
  []
  (scope-let [server (ag-server *connection-params*)]
    (println "Available catalogs:" (catalogs server))
    (let [catalog (ag-catalog server (:catalog *connection-params*))]
      (println "Available repositories in catalog"
               (name catalog) ":" (repositories catalog))
      (let [repo (ag-repo-con catalog (:repository *connection-params*))]
        (clear! repo)
        (println "Repository" (name repo) "is up!"
                 "It contains" (repo-size repo) "statements.")
        (repository repo)))))

(defn example2
  "demonstrates adding and removing triples."
  []
  (scope1-let [server (ag-server *connection-params*)
               catalog (ag-catalog server (:catalog *connection-params*))
               repo (ag-repo-con catalog (:repository *connection-params*))
               f (value-factory repo)
               ;; create some resources and literals to make statements out of
               alice (uri f "http://example.org/people/alice")
               bob (uri f "http://example.org/people/bob")
               name (uri f "http://example.org/ontology/name")
               person (uri f "http://example.org/ontology/Person")
               bobsName (literal f "Bob")
               alicesName (literal f "Alice")]
    (clear! repo)
    (println "Triple count before inserts:" (repo-size repo))
    (printlns (get-statements repo [nil nil nil]))
    
    (add! repo [alice RDF/TYPE person] nil)
    (add! repo [alice name alicesName] nil)
    (add! repo [bob RDF/TYPE person] nil)
    (add! repo [bob (uri f "http://example.org/ontology/name") bobsName] nil)
    (println "Triple count after adding:" (repo-size repo))
    
    (remove! repo [bob name bobsName])
    (println "Triple count after removing:" (repo-size repo))
    
    ;; add it back
    (add! repo [bob name bobsName] nil)
    (repository repo)))

(defn example3
  "demonstrates a SPARQL query using the data from test2"
  []
  (scope-let [repo (repo-connection (example2))
             server (ag-server *connection-params*)
             catalog (ag-catalog server (:catalog *connection-params*))
             repo (ag-repo-con catalog (:repository *connection-params*))]
    (printlns (tuple-query repo QueryLanguage/SPARQL "SELECT ?s ?p ?o  WHERE {?s ?p ?o .}"
                           nil))))

(defn example4
  ""
  []
  (scope-let [repo (repo-connection (example2))]
    (let [alice (uri (value-factory repo) "http://example.org/people/alice")]
      (printlns (get-statements repo [alice nil nil] nil)))))

(defn example5
  "Typed Literals"
  []
  (scope
    (let [server (ag-server *connection-params*)
          catalog (ag-catalog server (:catalog *connection-params*))
          repo (ag-repo-con catalog (:repository *connection-params*))
          f (value-factory repo)
          exns "http://example.org/people/"
          alice (uri f "http://example.org/people/alice")
          age (uri f exns "age")
          weight (uri f exns, "weight")
          favoriteColor (uri f exns "favoriteColor")
          birthdate (uri f exns "birthdate")
          ted (uri f exns "Ted")
          red (literal f "Red")
          rouge (literal f "Rouge" "fr")
          fortyTwoInt (literal f "42" XMLSchema/INT)
          fortyTwoLong (literal f "42" XMLSchema/LONG)
          fortyTwoUntyped (literal f "42")
          date (literal f "1984-12-06" XMLSchema/DATE)
          time (literal f "1984-12-06T09:00:00" XMLSchema/DATETIME)]
      (clear! repo)
      (add-all! repo
                [(.createStatement f alice age fortyTwoInt)
                 (.createStatement f ted age fortyTwoUntyped)
                 [alice weight (literal f "20.5")]
                 [ted weight (literal f "20.5" XMLSchema/FLOAT)]
                 [alice favoriteColor red]
                 [ted favoriteColor rouge]
                 [alice birthdate date]
                 [ted birthdate time]])
      (doseq [obj [nil fortyTwoInt fortyTwoLong fortyTwoUntyped
                   (literal f "20.5" XMLSchema/FLOAT)
                   (literal f "20.5") red rouge]]
        (println "Retrieve triples matching" obj ".")
        (printlns (get-statements repo [nil nil obj] nil)))
      (doseq [obj ["42", "\"42\"", "20.5", "\"20.5\"", "\"20.5\"^^xsd:float"
                   "\"Rouge\"@fr", "\"Rouge\"", "\"1984-12-06\"^^xsd:date"]]
        (println "Query triples matching" obj ".")
        (printlns (tuple-query repo QueryLanguage/SPARQL
                               (str "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "
                                    "SELECT ?s ?p ?o WHERE {?s ?p ?o . filter (?o = " obj ")}")
                               nil))))))

(defn example6
  []
  (scope1
    (let [server (ag-server *connection-params*)
          catalog (ag-catalog server (:catalog *connection-params*))
          repo (ag-repo-con catalog (:repository *connection-params*)
                            {:namespaces {"vcd" "http://www.w3.org/2001/vcard-rdf/3.0#"}})
          f (value-factory repo)
          vcards (new File *agraph-java-tutorial-dir* "/java-vcards.rdf")
          kennedy (new File *agraph-java-tutorial-dir* "/java-kennedy.ntriples")
          baseURI "http://example.org/example/local"
          context (uri f "http://example.org#vcards")]
      (clear! repo)
      (add-from! repo vcards baseURI RDFFormat/RDFXML context)
      (add-from! repo kennedy baseURI RDFFormat/NTRIPLES nil)
      (println "After loading, repository contains " (repo-size repo context)
               " vcard triples in context '" context "'\n    and   "
               (repo-size repo nil) " kennedy triples in context 'nil'.")
      (repository repo))))

(defn tutorial-repo
  "Shortcut for tutorial functions, returns the AGRepositoryConnection."
  ([] (tutorial-repo nil))
  ([rcon-args]
     (let [server (ag-server *connection-params*)
           catalog (ag-catalog server (:catalog *connection-params*))
           repo (ag-repo-con catalog (:repository *connection-params*) rcon-args)]
       repo)))

(defn example7
  []
  (scope-let [repo (repo-connection (example6))
              repo (tutorial-repo)]
    (println "Match all and print subjects and contexts:")
    (printlns (get-statements repo [nil nil nil] nil))
    
    (println "Same thing with SPARQL query:")
    (printlns (tuple-query repo QueryLanguage/SPARQL
                           "SELECT DISTINCT ?s ?c WHERE {graph ?c {?s ?p ?o .} }" nil))))

(defn example8
  "Writing RDF or NTriples to a file"
  [& [write-to-file?]]
  (scope-let [repo (repo-connection (example6))
              repo (tutorial-repo)
              contexts (resource-array [(uri (value-factory repo) "http://example.org#vcards")])]
    (let [output (if write-to-file?
                   (new FileOutputStream "/tmp/temp.nt")
                   *out*)
          writer (new NTriplesWriter output)]
      (println "Writing NTriples to" output)
      (.export repo writer contexts))
    
    (let [output (if write-to-file?
                   (new FileOutputStream "/tmp/temp.rdf")
                   *out*)
          writer (new RDFXMLWriter output)]
      (println "Writing RDFXML to" output)
      (.export repo writer contexts))))

(defn example9
  "Writing the result of a statements match to a file."
  []
  (scope-let [repo (repo-connection (example6))
              repo (tutorial-repo)]
    (.exportStatements repo nil RDF/TYPE nil false (new RDFXMLWriter *out*) (resource-array nil))))

(defn example10
  "Datasets and multiple contexts"
  []
  (scope-let [repo (clear! (tutorial-repo))
              f (value-factory repo)
              exns "http://example.org/people/"
              alice (uri f exns "alice")
              bob (uri f exns "bob")
              ted (uri f exns "ted")
              name (uri f "http://example.org/ontology/name")
              person (uri f "http://example.org/ontology/Person")
              alicesName (literal f "Alice")
              bobsName (literal f "Bob")
              tedsName (literal f "Ted")
              context1 (uri f exns "cxt1")
              context2 (uri f exns "cxt2")]
    (add-all! repo [[alice RDF/TYPE person context1]
                    [alice name alicesName context1]
                    [bob RDF/TYPE person context2]
                    [bob name bobsName context2]
                    [ted RDF/TYPE person]
                    [ted name tedsName]])
    (println "All triples in all contexts:")
    (printlns (get-statements repo [nil nil nil]))
    (println "Triples in contexts 1 or 2:")
    (printlns (get-statements repo [nil nil nil] {:contexts [context1 context2]}))
    (println "Triples in contexts nil or 2:")
    (printlns (get-statements repo [nil nil nil] {:contexts [nil context2]}))
    
    (println "Query over contexts 1 and 2:")
    (printlns (tuple-query repo QueryLanguage/SPARQL
                           "SELECT ?s ?p ?o ?c WHERE { GRAPH ?c {?s ?p ?o . } }"
                           {:dataset (doto (new DatasetImpl)
                                       (.addNamedGraph context1)
                                       (.addNamedGraph context1))}))
    
    (println "Query over the null context:")
    (printlns (tuple-query repo QueryLanguage/SPARQL
                           "SELECT ?s ?p ?o WHERE {?s ?p ?o . }"
                           {:dataset (doto (new DatasetImpl)
                                       (.addDefaultGraph nil))}))))

(defn example11
  "Namespaces"
  []
  (scope
    (let [exns "http://example.org/people/"
          server (ag-server *connection-params*)
          catalog (ag-catalog server (:catalog *connection-params*))
          repo (ag-repo-con catalog (:repository *connection-params*) {:namespaces {"ex" exns}})
          f (value-factory repo)
          alice (uri f exns "alice")
          person (uri f exns "Person")]
      (clear! repo)
      (add! repo [alice RDF/TYPE person] nil)
      (printlns (tuple-query repo QueryLanguage/SPARQL
                             "SELECT ?s ?p ?o WHERE { ?s ?p ?o . FILTER ((?p = rdf:type) && (?o = ex:Person) ) }"
                             nil)))))

(defn example12
  "Text search"
  []
  (scope-let [exns "http://example.org/people/"
              server (ag-server *connection-params*)
              catalog (ag-catalog server (:catalog *connection-params*))
              repo (ag-repo-con catalog (:repository *connection-params*) {:namespaces {"ex" exns}})
              ;; Note, namespace {'fti' "http://franz.com/ns/allegrograph/2.2/textindex/"} is already built-in
              f (value-factory repo)
              alice (uri f exns "alice1")
              persontype (uri f exns "Person")
              fullname (uri f exns "fullname")
              alicename (literal f "Alice B. Toklas")
              book (uri f exns "book1")
              booktype (uri f exns "Book")
              booktitle (uri f exns "title")
              wonderland (literal f "Alice in Wonderland")]
    (clear! repo)
    (create-freetext-index repo "index1" (uri f exns "fullname"))
    (add-all! repo [[alice RDF/TYPE persontype]
                    [alice fullname alicename]
                    [book RDF/TYPE booktype]
                    [book booktitle wonderland]])
    (doseq [match ["?s fti:match 'Alice' ."
                   "?s fti:match 'Ali*' ."
                   "?s fti:match '?l?c?' ."
                   ;; TODO: "FILTER regex(?o, \"lic\")"
                   ]]
      (let [query (str "SELECT ?s ?p ?o WHERE { ?s ?p ?o . " match " }")]
        (printlns (take 5 (tuple-query repo QueryLanguage/SPARQL query nil)))))))

(defn example13
  "Ask, Construct, and Describe queries"
  []
  (scope-let [repo (repo-connection (example2) {:namespaces {"ex" "http://example.org/people/"
                                                             "ont" "http://example.org/ontology/"}})
              f (value-factory repo)]
    (printlns (tuple-query repo QueryLanguage/SPARQL
                           "select ?s ?p ?o where { ?s ?p ?o}" nil))
    (println "Boolean result:"
             (query-boolean repo QueryLanguage/SPARQL
                            "ask { ?s ont:name \"Alice\" }" nil))
    (print "Construct result: ")
    (doall (map #(print % " ")
                (query-graph repo QueryLanguage/SPARQL
                             "construct {?s ?p ?o} where { ?s ?p ?o . filter (?o = \"Alice\") } " nil)))
    (println)
    (println "Describe result: ")
    (printlns (query-graph repo QueryLanguage/SPARQL
                           "describe ?s where { ?s ?p ?o . filter (?o = \"Alice\") }" nil))))

(defn example14
  "Parametric Queries"
  []
  (scope-let [repo (repo-connection (example2))
              f (value-factory repo)
              alice (uri f "http://example.org/people/alice")
              bob (uri f "http://example.org/people/bob")]
    (println "Facts about Alice:")
    (printlns (tuple-query repo QueryLanguage/SPARQL
                           "select ?s ?p ?o where { ?s ?p ?o}"
                           {:bindings {"s" alice}}))
    (println "Facts about Bob:")
    (printlns (tuple-query repo QueryLanguage/SPARQL
                           "select ?s ?p ?o where { ?s ?p ?o}"
                           {:bindings {"s" bob}}))))

(defn example16
  "Federated triple stores."
  []
  (scope-let [server (ag-server *connection-params*)
              cat (ag-catalog server (:catalog *connection-params*))
              ex "http://www.demo.com/example#"
              repo-args {:namespaces {"ex" ex}}
              ;; create two ordinary stores, and one federated store: 
              red (clear! (ag-repo-con cat "redthings" repo-args))
              green (clear! (ag-repo-con cat "greenthings" repo-args))
              rainbow (repo-federation server [red green] repo-args)
              rf (value-factory red)
              gf (value-factory green)
              rbf (value-factory rainbow)]
        ;; add a few triples to the red and green stores:
        (doseq [[c f s o] [[red rf "mcintosh" "Apple"]
                           [red rf "reddelicious" "Apple"]
                           [green gf "pippen" "Apple"]
                           [green gf "kermitthefrog" "Frog"]
                           ]]
          (add! c (uri f (str ex s)) RDF/TYPE (uri rf (str ex o))))
        ;; query each of the stores; observe that the federated one is the union of the other two:
        (doseq [[kind repo] [["red" red]
                             ["green" green]
                             ["federated" rainbow]]]
          (println)
          (println kind "apples:")
          (printlns (tuple-query repo QueryLanguage/SPARQL
                                 "select ?s where { ?s rdf:type ex:Apple }"
                                 nil)))))

;; (defn test17
;;   "Prolog queries"
;;   []
;;   (with-open2 [rcon (repo-connection (test6))]
;;     (.deleteEnvironment rcon "kennedys") ;; start fresh
;;     (.setEnvironment rcon "kennedys")
;;     (.setNamespace rcon "kdy" "http://www.franz.com/simple#")
;;     (.setRuleLanguage rcon lang-prolog)
;;     (.addRules rcon "
;;        (<-- (female ?x) ;; IF
;;           (q ?x !kdy:sex !kdy:female))
;;        (<-- (male ?x) ;; IF
;;           (q ?x !kdy:sex !kdy:male))
;;        ")
;;     ;; This causes a failure(correctly):
;;     ;; conn.deleteRule('male')
;;     (with-results [row (prepare-query rcon {:type :tuple :lang lang-prolog} "
;;       (select (?person ?name)
;;               (q ?person !rdf:type !kdy:person)
;;               (male ?person)
;;               (q ?person !kdy:first-name ?name)
;;               )
;;       ")]
;;       (println row))))

(defn examples-all
  []
  (example1)
  (example2)
  (example3)
  (example4)
  (example5)
  (example6)
  (example7)
  (example8)
  (example9)
  (example10)
  (example11)
  (example12)
  (example13)
  (example14)
  (example16)
  )
