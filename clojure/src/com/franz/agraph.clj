;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Copyright (c) 2008-2010 Franz Inc.
;; All rights reserved. This program and the accompanying materials
;; are made available under the terms of the Eclipse Public License v1.0
;; which accompanies this distribution, and is available at
;; http://www.eclipse.org/legal/epl-v10.html
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns com.franz.agraph
  "Clojure client API to Franz AllegroGraph 4.1.
 This API wraps the agraph-java-client API, which is an extension of the Sesame org.openrdf API.
 Communication with the server is through HTTP REST using JSON.
 Uses the Franz Clojure wrapper of Sesame in com.franz.openrdf."
  (:refer-clojure :exclude [name with-open])
  (:import [com.franz.agraph.repository
            AGCatalog AGQueryLanguage AGRepository
            AGRepositoryConnection AGServer AGValueFactory]
           [org.openrdf.model ValueFactory Resource Literal Statement URI]
           [org.openrdf.repository Repository RepositoryConnection]
           [org.openrdf.model.vocabulary RDF XMLSchema]
           [org.openrdf.query QueryLanguage BindingSet Binding])
  (:use [com.franz util openrdf]))

(alter-meta! *ns* assoc :author "Franz Inc <www.franz.com>, Mike Hinchey <mhinchey@franz.com>")

(defmethod name AGRepository [#^AGRepository x] (.getRepositoryID x))

(defmethod name AGRepositoryConnection [#^AGRepositoryConnection x] (name (.getRepository x)))

(defmethod name AGCatalog [#^AGCatalog x] (.getCatalogName x))

(defn ag-server
  [{:keys [url username password]}]
  (open (AGServer. url username password)))

(defn catalogs
  "Returns a seq of String names."
  [#^AGServer server]
  (seq (.listCatalogs server)))

(defn ag-catalog
  "Returns an AGCatalog."
  {:tag AGCatalog}
  [#^AGServer server name]
  (.getCatalog server name))

(defn repositories
  "Returns a seq of AGRepository objects."
  [#^AGCatalog catalog]
  (seq (.listRepositories catalog)))

(defn repository
  "access-verb must be a keyword from the set of access-verbs."
  ([#^AGCatalog catalog name access-verb]
     (open (.createRepository catalog #^String name
                              ;; TODO: (-access-verbs access-verb)
                              )))
  ;; TODO: this may be confusing since it doesn't open a repository,
  ;; only gets a reference.
  ([#^RepositoryConnection rcon]
     (.getRepository rcon)))

(defn ag-repo-con
  ([#^AGCatalog catalog repo-name]
     (repo-connection (repo-init (repository catalog repo-name nil))))
  ([#^AGCatalog catalog repo-name rcon-args]
     (repo-connection (repo-init (repository catalog repo-name nil)) rcon-args)))

(defn repo-federation
  "rcons: may be of type AGRepository or AGRepositoryConnection"
  [#^AGServer server rcons rcon-args]
  (-> (.federate server
                 (into-array AGRepository (map #(cond (instance? AGRepository %) %
                                                      (nil? %) nil
                                                      :else (.getRepository #^AGRepositoryConnection %))
                                                rcons)))
      open repo-init (repo-connection rcon-args)))

(defn add-from-server!
  ;; Different name from add-from! to make it less ambiguous.
  ;; This is an AllegroGraph extension to the openrdf api.
  "Add statements from a data file on the server.
   See add-from!.
  
     data:     a File, InputStream, or URL.
     contexts: 0 or more Resource objects"
  [#^AGRepositoryConnection repos-conn
   data
   #^String baseURI
   #^RDFFormat dataFormat
   & contexts]
  (.add repos-conn data baseURI dataFormat (resource-array contexts)))

(defn create-freetext-index
  [^AGRepositoryConnection repo
   ^String name
   & predicates]
  (.createFreetextIndex repo name (into-array URI predicates)))
