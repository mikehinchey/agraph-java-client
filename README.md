# AllegroGraph Java Client API

Client API to [Franz AllegroGraph](http://www.franz.com/agraph/)
triple store database, versions 3.2 and 3.3.

This [agraph-java-client](http://github.com/franzinc/agraph-java-client) provides:

* Java client API
* Adapter for Sesame
* Adapter for Jena
* Clojure client API

[AllegroGraph Docs](http://www.franz.com/agraph/support/documentation/current/)


## Prerequisites:

* [Download AllegroGraph](http://www.franz.com/agraph/downloads/)
  version 3.2 or 3.3
* Install
* Run the updater to get the latest patches


## Java

The primary public package is <code>com.franz.agbase</code>.

Supports Prolog queries.


### Sesame

[Sesame](http://www.openrdf.org/) 2.2.4,
([API](http://www.openrdf.org/doc/sesame2/2.2.4/apidocs/))


### Jena

[Jena](http://jena.sourceforge.net/) 2.5


## Clojure

Uses [Clojure](http://clojure.org) 1.1

The tutorial included is similar to the Python tutorial. The comment
section at the top of the file gives instructions to get started.

* clojure/test/com/franz/agraph/tutorial.clj
* [Python tutorial](http://www.franz.com/agraph/support/documentation/current/python-tutorial.html)
* [Python API](http://github.com/franzinc/agraph-python/tree/agraph32)

### Usage

Add to your leiningen project.clj dependencies:

    [com.franz/agraph-clj "3.2-SNAPSHOT"]


## Development

For Ant users, the Java library includes build.xml. The following
command line will build the agraph-java-client jar:

    ant build

For Maven users, the Java library includes pom.xml and an Ant target
to install. A pom-sesame.xml is also included because this library is not
available in another public maven repo. The following command line
will build and install the jars for agraph-java-client and
openrdf-sesame to your local maven directory (~/.m2/).

    ant mvn-install

The Clojure library includes a project.clj for use with
[Leiningen](http://github.com/technomancy/leiningen/tree/stable). It
depends on the agraph-java-client, so you will need to use the
mvn-install command above before using lein. The following command
line will install all dependencies in agraph-java-client/clojure/lib/.

    lein deps

Alternatively, for Ant users, the Clojure library includes a
build.xml and libs/clojure-1.1.0.jar.

    ant build


## License

Copyright (c) 2008-2010 Franz Inc.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Eclipse Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/epl-v10.html

