# AllegroGraph Java Client API

Client API to [Franz AllegroGraph](http://www.franz.com/agraph/)
triple store database.

This [agraph-java-client](http://github.com/franzinc/agraph-java-client) provides:

* Java client API
* Adapter for Sesame
* Adapter for Jena
* Clojure client API

[AllegroGraph Docs](http://www.franz.com/agraph/support/documentation/current/)

License: EPL


## Prerequisites:

* [Download AllegroGraph](http://www.franz.com/agraph/downloads/)
* Install
* Run the updater to get the latest patches


## Java

The primary public package is <code>com.franz.agbase</code>.

Supports Prolog queries.


### Sesame

[Sesame](http://www.openrdf.org/) 2.2.4,
([API](http://www.openrdf.org/doc/sesame2/2.2.4/apidocs/))


### Jena

Jena 2.5


## Clojure

Uses [Clojure](http://clojure.org) 1.1

The tutorial included is similar to the Python tutorial:

* src/com/franz/agraph/tutorial.clj
* [Python tutorial](http://www.franz.com/agraph/support/documentation/current/python-tutorial.html)
* [Python API](http://github.com/franzinc/agraph-python/tree/agraph32)

agclj.sh can be used to start a Clojure REPL in Emacs/Slime or in a console. It depends on agraph-java.


## Development

For Ant users, the Java library includes build.xml. The following
command line will build the agraph-java-client jar:

    <code>ant build</code>

For Maven users, the Java library includes pom.xml and an Ant target
to install. A pom-sesame.xml is also included because this library is not
available in another public maven repo. The following command line
will build and install the jars for agraph-java-client and
openrdf-sesame to your local maven directory (~/.m2/).

    <code>ant mvn-install</code>

The Clojure library includes a project.clj for use with Leiningen. It
depends on the agraph-java-client, so you will need to use the
mvn-install command above before using lein. The following command
line will install all dependencies in agraph-java-client/clojure/lib/.

    <code>lein deps</code>

