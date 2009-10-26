import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.BindingSet;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.impl.DatasetImpl;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.ntriples.NTriplesWriter;
import org.openrdf.rio.rdfxml.RDFXMLWriter;

import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGQueryLanguage;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import com.franz.agraph.repository.AGValueFactory;

public class TutorialExamples {
	/*
	 * Edited by BDC for Java Client Tutorial.
	 */

    static private final String SERVER_URL = "http://localhost:8080";
    static private final String CATALOG_ID = "scratch";
    static private final String USERNAME = "test";
    static private final String PASSWORD = "xyzzy";

    static final String FOAF_NS = "http://xmlns.com/foaf/0.1/";

    /**
     * Creating a Repository
     */
    public static AGRepositoryConnection example1(boolean close) throws RepositoryException {
        // Tests getting the repository up. 
        println("\nStarting example1().");
        AGServer server = new AGServer(SERVER_URL, USERNAME, PASSWORD);
        println("Available catalogs: " + (server.listCatalogs()));
        AGCatalog catalog = server.getCatalog(CATALOG_ID);
        println("Available repositories in catalog " + 
                (catalog.getCatalogName()) + ": " + 
                catalog.getAllRepositories());
        AGRepository myRepository = catalog.createRepository(CATALOG_ID);
        println("Got a repository.");
        myRepository.initialize();
        println("Initialized repository.");
        AGRepositoryConnection conn = myRepository.getConnection();
        println("Got a connection.");
        conn.clear();  // remove previous triples, if any.
        println("Cleared the connection.");
        println("Repository " + (myRepository.getRepositoryID()) +
                " is up! It contains " + (conn.size()) +
                " statements."              
                );
        if (close) {
            // tidy up
            conn.close();
            myRepository.shutDown();
            return null;
        }
        return conn;
    }
    
    /**
     * Asserting and Retracting Triples
     */
    public static AGRepositoryConnection example2(boolean close) throws RepositoryException {
        // Asserts some statements and counts them.
        AGRepositoryConnection conn = example1(false);
        AGValueFactory vf = conn.getRepository().getValueFactory();
        println("\nStarting example2().");
        // Create some resources and literals to make statements from.
        URI alice = vf.createURI("http://example.org/people/alice");
        URI bob = vf.createURI("http://example.org/people/bob");
        URI name = vf.createURI("http://example.org/ontology/name");
        URI person = vf.createURI("http://example.org/ontology/Person");
        Literal bobsName = vf.createLiteral("Bob");
        Literal alicesName = vf.createLiteral("Alice");
        println("Triple count before inserts: " + 
                (conn.size()));
        // Alice's name is "Alice"
        conn.add(alice, name, alicesName);
        // Alice is a person
        conn.add(alice, RDF.TYPE, person);
        //Bob's name is "Bob"
        conn.add(bob, name, bobsName);
        //Bob is a person, too. 
        conn.add(bob, RDF.TYPE, person);
        println("Added four triples.");
        println("Triple count after inserts: " + 
                (conn.size()));
        RepositoryResult<Statement> result = conn.getStatements(null, null, null, false);
        while (result.hasNext()) {
            Statement st = result.next();
            println(st);
        }
        conn.remove(bob, name, bobsName);
        println("Removed one triple.");
        println("Triple count after deletion: " + 
                (conn.size()));
        // put it back so we can continue with other examples
        conn.add(bob, name, bobsName);
        if (close) {
            conn.close();
            conn.getRepository().shutDown();
            return null;
        }
        return conn;
    }
    
    /**
     * A SPARQL Query
     */
    public static void example3() throws Exception {
        AGRepositoryConnection conn = example2(false);
        println("\nStarting example3().");
       try {
            String queryString = "SELECT ?s ?p ?o  WHERE {?s ?p ?o .}";
            TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
            TupleQueryResult result = tupleQuery.evaluate();
            try {
                while (result.hasNext()) {
                    BindingSet bindingSet = result.next();
                    Value s = bindingSet.getValue("s");
                    Value p = bindingSet.getValue("p");
                    Value o = bindingSet.getValue("o");
                    System.out.format("%s %s %s\n", s, p, o);
                }
            } finally {
                result.close();
            }
        } finally {
            conn.close();
        }
    }

    /**
     * Statement Matching
     */
    public static void example4() throws RepositoryException {
        RepositoryConnection conn = example2(false);
        Repository myRepository = conn.getRepository();
        println("\nStarting example4().");
        URI alice = myRepository.getValueFactory().createURI("http://example.org/people/alice");
        RepositoryResult<Statement> statements = conn.getStatements(alice, null, null, false);
        try {
            statements.enableDuplicateFilter();
            while (statements.hasNext()) {
                println(statements.next());
            }
        } finally {
            statements.close();
        }
        
//        println( "Same thing using JDBC:");
//        JDBCResultSet resultSet = ((AGRepositoryConnection)conn).getJDBCStatements(alice, null, null, false);
//        while (resultSet.next()) {
//            //println("   " + resultSet.getRow());
//            println("   " + resultSet.getValue(2) + "   " + resultSet.getString(2)); 
//        }
        conn.close();
        myRepository.shutDown();
    }

    /**
     * Literal Values
     */
    public static void example5() throws Exception {
        RepositoryConnection conn = example2(false);
        Repository myRepository = conn.getRepository();
        ValueFactory f = myRepository.getValueFactory();
        println("\nStarting example5().");
        conn.clear();
        String exns = "http://example.org/people/";
        URI alice = f.createURI("http://example.org/people/alice");
        URI ted = f.createURI(exns, "ted");
        URI age = f.createURI(exns, "age");
        URI weight = f.createURI(exns, "weight");    
        URI favoriteColor = f.createURI(exns, "favoriteColor");
        URI birthdate = f.createURI(exns, "birthdate");
        Literal red = f.createLiteral("Red");
        Literal rouge = f.createLiteral("Rouge", "fr");
        Literal fortyTwoInt = f.createLiteral("42", XMLSchema.INT);
        Literal fortyTwoLong = f.createLiteral("42", XMLSchema.LONG);    
        Literal fortyTwoUntyped = f.createLiteral("42");
        Literal date = f.createLiteral("1984-12-06", XMLSchema.DATE);     
        Literal time = f.createLiteral("1984-12-06T09:00:00", XMLSchema.DATETIME);  
        Literal weightUntyped = f.createLiteral("120.5");
        Literal weightFloat = f.createLiteral("120.5", XMLSchema.FLOAT);
        Statement stmt1 = f.createStatement(alice, age, fortyTwoInt);
        Statement stmt2 = f.createStatement(ted, age, fortyTwoLong);    
        Statement stmt3 = f.createStatement(ted, age, fortyTwoUntyped);
        conn.add(stmt1);
        conn.add(stmt2);
        conn.add(stmt3);
        conn.add(alice, weight, weightFloat);
        conn.add(ted, weight, weightUntyped);
        conn.add(alice, favoriteColor, red);
        conn.add(ted, favoriteColor, rouge);
        conn.add(alice, birthdate, date);
        conn.add(ted, birthdate, time);    
        for (Literal obj : new Literal[] {null, fortyTwoInt, fortyTwoLong, fortyTwoUntyped,  weightFloat, weightUntyped,
                    red, rouge}) {
            println( "\nRetrieve triples matching " + obj + ".");
            RepositoryResult<Statement> statements = conn.getStatements(null, null, obj, false);
            try {
                while (statements.hasNext()) {
                    println(statements.next());
                }
            } finally {
                statements.close();
            }
        }
        // SPARQL
        for (String obj : new String[]{"42", "\"42\"", "120.5", "\"120.5\"", "\"120.5\"^^xsd:float",
                                       "\"Rouge\"@fr", "\"Rouge\"", "\"1984-12-06\"^^xsd:date"}) {
            println( "\nQuery triples matching " + obj + ".");
            String queryString = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> SELECT ?s ?p ?o WHERE {?s ?p ?o . filter (?o = " + obj + ")}";
            TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
            TupleQueryResult result = tupleQuery.evaluate();
            try {
                while (result.hasNext()) {
                    BindingSet bindingSet = result.next();
                    Value s = bindingSet.getValue("s");
                    Value p = bindingSet.getValue("p");
                    Value o = bindingSet.getValue("o");
                    println("  " + s + " " + p + " " + o);
                }
            } finally {
                result.close();
            }
        }
        {
            // Search for date using date object in triple pattern.
            println("\nRetrieve triples matching DATE object.");
            RepositoryResult<Statement> statements = conn.getStatements(null, null, date, false);
            try {
                while (statements.hasNext()) {
                    println(statements.next());
                }
            } finally {
                statements.close();
            }
        }

        {
            println("\nMatch triples having a specific DATE value.");
            RepositoryResult<Statement> statements = conn.getStatements(null, null,
                    f.createLiteral("\"1984-12-06\"^^<http://www.w3.org/2001/XMLSchema#date>"), false);
            try {
                while (statements.hasNext()) {
                    println(statements.next());
                }
            } finally {
                statements.close();
            }
        }
        {
            // Search for time using datetime object in triple pattern.
            println("\nRetrieve triples matching DATETIME object.");
            RepositoryResult<Statement> statements = conn.getStatements(null, null, time, false);
            try {
                while (statements.hasNext()) {
                    println(statements.next());
                }
            } finally {
                statements.close();
            }
        }
        {
            println("\nMatch triples having a specific DATETIME value.");
            RepositoryResult<Statement> statements = conn.getStatements(null, null,
                    f.createLiteral("\"1984-12-06T09:00:00\"^^<http://www.w3.org/2001/XMLSchema#dateTime>"), false);
            try {
                while (statements.hasNext()) {
                    println(statements.next());
                }
            } finally {
                statements.close();
            }
        }

        conn.close();
        myRepository.shutDown();
    }
    
    /**
     * Importing Triples my version

    public static AGRepositoryConnection example6(boolean close) throws Exception {
        AGServer server = new AGServer(SERVER_URL, USERNAME, PASSWORD);
        AGCatalog catalog = server.getCatalog(CATALOG_ID);
        AGRepository myRepository = catalog.createRepository(CATALOG_ID);
        myRepository.initialize();
        AGRepositoryConnection conn = myRepository.getConnection();
conn.clear(); // renew?
// TODO: openDedicated /home/bdc/ag40/src/src/agraph-java-client/src/tutorial
        ValueFactory f = myRepository.getValueFactory();
        String path1 = "C:\\cygwin\\home\\BDC\\ag40\\src\\src\\agraph-java-client\\src\\tutorial\\vc-db-1.rdf";    
//        String path2 = "C:\\cygwin\\home\\BDC\\ag40\\src\\src\\agraph-java-client\\src\\tutorial\\kennedy.ntriples";            
        String path2 = "C:\\kennedy.ntriples";
        String baseURI = "http://example.org/example/local";
        Resource context = f.createURI("http://example.org#vcards");
        conn.setNamespace("vcd", "http://www.w3.org/2001/vcard-rdf/3.0#");
        println("Before v-card");
        conn.add(new File(path1), baseURI, RDFFormat.RDFXML, context);
        // read kennedy triples into the null context:
        println("Before kennedy");
        conn.add(new File(path2), baseURI, RDFFormat.NTRIPLES);
        // read vcards triples into the context 'context':
        println("After");
        println("After loading, repository contains " + conn.size(context) +
                " vcard triples in context '" + context + "'\n    and   " +
                conn.size() + " football triples in context 'null'.");
        if (close) {
            conn.close();
            return null;
        }
        return conn;
    }
*/
    
    public static AGRepositoryConnection example6(boolean close) throws Exception {
        AGServer server = new AGServer(SERVER_URL, USERNAME, PASSWORD);
        AGCatalog catalog = server.getCatalog(CATALOG_ID);
        AGRepository myRepository = catalog.createRepository(CATALOG_ID);
        myRepository.initialize();
        AGRepositoryConnection conn = myRepository.getConnection();
//        conn.openSession();
        conn.clear();
        ValueFactory f = myRepository.getValueFactory();
        String path1 = "src/vc-db-1.rdf";    
        String path2 = "src/kennedy.ntriples";            
//        String path1 = "C:\\cygwin\\home\\BDC\\ag40\\src\\src\\agraph-java-client\\src\\tutorial\\vc-db-1.rdf";    
//        String path2 = "C:\\cygwin\\home\\BDC\\ag40\\src\\src\\agraph-java-client\\src\\tutorial\\kennedy.ntriples";            

        String baseURI = "http://example.org/example/local";
        URI context = f.createURI("http://example.org#vcards");
//        conn.setNamespace("vcd", "http://www.w3.org/2001/vcard-rdf/3.0#");
        // read vcards triples into the context 'context':
        conn.add(new File(path1), baseURI, RDFFormat.RDFXML, context);
        // read Kennedy triples into the null context:
        conn.add(new File(path2), baseURI, RDFFormat.NTRIPLES);
        println("After loading, repository contains " + conn.size(context) +
                " vcard triples in context '" + context + "'\n    and   " +
                conn.size((Resource)null) + " kennedy triples in context 'null'.");
        if (close) {
            conn.close();
            return null;
        }
        return conn;
    }
 
    
    /**
     * Importing Triples, query
     */
    public static void example7() throws Exception {
        RepositoryConnection conn = example6(false);
        println("\nMatch all and print subjects and contexts");
        RepositoryResult<Statement> result = conn.getStatements(null, null, null, false);
// TODO: limit=25
        for (int i = 0; i < 25 && result.hasNext(); i++) {
            Statement stmt = result.next();
            println(stmt.getSubject() + "  " + stmt.getContext());
        }
        result.close();
        
        println("\nSame thing with SPARQL query (can't retrieve triples in the null context)");
        String queryString = "SELECT ?s ?c WHERE {graph ?c {?s ?p ?o .} }";
        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        TupleQueryResult qresult = tupleQuery.evaluate();       
        while (qresult.hasNext()) {
            BindingSet bindingSet = qresult.next();
            println(bindingSet.getBinding("s") + "  " + bindingSet.getBinding("c"));
        }
        qresult.close();
        conn.close();
    }

    // Writing RDF or NTriples to a file
    public static void example8() throws Exception {
        RepositoryConnection conn = example6(false);
        Repository myRepository = conn.getRepository();
        URI context = myRepository.getValueFactory().createURI("http://example.org#vcards");
        String outputFile = "/tmp/temp.nt";
//        outputFile = null;
        if (outputFile == null) {
            println("\nWriting n-triples to Standard Out instead of to a file");
        } else {
            println("\nWriting n-triples to: " + outputFile);
        }
        OutputStream output = (outputFile != null) ? new FileOutputStream(outputFile) : System.out;
        NTriplesWriter ntriplesWriter = new NTriplesWriter(output);
        conn.export(ntriplesWriter, context);
        String outputFile2 = "/tmp/temp.rdf";
//        outputFile2 = null;
        if (outputFile2 == null) {
            println("\nWriting RDF to Standard Out instead of to a file");
        } else {
            println("\nWriting RDF to: " + outputFile2);
        }
        output = (outputFile2 != null) ? new FileOutputStream(outputFile2) : System.out;
        RDFXMLWriter rdfxmlfWriter = new RDFXMLWriter(output);
        conn.export(rdfxmlfWriter, context);
        output.write('\n');
        conn.close();
    }
    
    /**
     * Writing the result of a statements match to a file.
     */
    public static void example9() throws Exception {    
        RepositoryConnection conn = example6(false);
        conn.exportStatements(null, RDF.TYPE, null, false, new RDFXMLWriter(System.out));
        conn.close();
    }

    /**
     * Datasets and multiple contexts.
     */
    public static void example10 () throws Exception {
        RepositoryConnection conn = example1(false);
        Repository myRepository = conn.getRepository();
        ValueFactory f = myRepository.getValueFactory();
        String exns = "http://example.org/people/";
        URI alice = f.createURI(exns, "alice");
        URI bob = f.createURI(exns, "bob");
        URI ted = f.createURI(exns, "ted");        
        URI person = f.createURI("http://example.org/ontology/Person");
        URI name = f.createURI("http://example.org/ontology/name");
        Literal alicesName = f.createLiteral("Alice");
        Literal bobsName = f.createLiteral("Bob");
        Literal tedsName = f.createLiteral("Ted");        
        URI context1 = f.createURI(exns, "cxt1");      
        URI context2 = f.createURI(exns, "cxt2");         
        conn.add(alice, RDF.TYPE, person, context1);
        conn.add(alice, name, alicesName, context1);
        conn.add(bob, RDF.TYPE, person, context2);
        conn.add(bob, name, bobsName, context2);
        conn.add(ted, RDF.TYPE, person);
        conn.add(ted, name, tedsName);
        RepositoryResult<Statement> statements = conn.getStatements(null, null, null, false);
        println("\nAll triples in all contexts:");        
        while (statements.hasNext()) {
            println(statements.next());            
        }
        statements = conn.getStatements(null, null, null, false, context1, context2);
        println("\nTriples in contexts 1 or 2:");        
        while (statements.hasNext()) {
            println(statements.next());
        }
        statements = conn.getStatements(null, null, null, false, null, context2);
        println("\nTriples in contexts null or 2:");        
        while (statements.hasNext()) {
            println(statements.next());
        }

        // testing named graph query
        String queryString = "SELECT ?s ?p ?o ?c WHERE { GRAPH ?c {?s ?p ?o . } }";        
        DatasetImpl ds = new DatasetImpl();
        ds.addNamedGraph(context1);
        ds.addNamedGraph(context2);
        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        tupleQuery.setDataset(ds);
        TupleQueryResult result = tupleQuery.evaluate();    
        println("\nQuery over contexts 1 and 2.");
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            println(bindingSet.getBinding("s") + " " + 
            		bindingSet.getBinding("p") + " " +
            		bindingSet.getBinding("o") + " " +
            		bindingSet.getBinding("c"));
        }    
        
        queryString = "SELECT ?s ?p ?o WHERE {?s ?p ?o . }";
        ds = new DatasetImpl();
        // TODO: ds.addDefaultGraph(null);
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        tupleQuery.setDataset(ds);
        result = tupleQuery.evaluate();    
        println("\nQuery over the null context.");
        while (result.hasNext()) {
            println(result.next());
        }
        conn.close();
    }
    
    /**
     * Namespaces
     */
    public static void example11 () throws Exception {
        RepositoryConnection conn = example1(false);
        Repository myRepository = conn.getRepository();
	    ValueFactory f = myRepository.getValueFactory();
	    String exns = "http://example.org/people/";
	    URI alice = f.createURI(exns, "alice");
	    URI person = f.createURI(exns, "Person");
	    conn.add(alice, RDF.TYPE, person);
	    conn.setNamespace("ex", exns);
	    //conn.removeNamespace("ex");
	    String queryString = "SELECT ?s ?p ?o WHERE { ?s ?p ?o . FILTER ((?p = rdf:type) && (?o = ex:Person) ) }";
	    TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
	    TupleQueryResult result = tupleQuery.evaluate();  
	    while (result.hasNext()) {
        	println(result.next());
        }
	    result.close();
	    conn.close();
	}                                                   

	/**
	 * Text search
	 */
	public static void example12 () throws Exception {    
        AGRepositoryConnection conn = example1(false);
	    ValueFactory f = conn.getValueFactory();
	    String exns = "http://example.org/people/";
	    conn.setNamespace("ex", exns);
//	    conn.registerFreetextPredicate(f.createURI("http://example.org/people/name"));
	    conn.registerFreetextPredicate(f.createURI(exns,"fullname"));
	    URI alice = f.createURI(exns, "alice1");
	    URI persontype = f.createURI(exns, "Person");
	    URI fullname = f.createURI(exns, "fullname");    
	    Literal alicename = f.createLiteral("Alice B. Toklas");
	    URI book =  f.createURI(exns, "book1");
	    URI booktype = f.createURI(exns, "Book");
	    URI booktitle = f.createURI(exns, "title");    
	    Literal wonderland = f.createLiteral("Alice in Wonderland");
	    conn.clear();    
	    conn.add(alice, RDF.TYPE, persontype);
	    conn.add(alice, fullname, alicename);
	    conn.add(book, RDF.TYPE, booktype);    
	    conn.add(book, booktitle, wonderland); 
	    //conn.setNamespace('fti', "http://franz.com/ns/allegrograph/2.2/textindex/");  // is already built-in  
/*	    String[] testMatches = {"?s fti:match 'Alice' .",
	            "?s fti:match 'Ali*' .",
	            "?s fti:match '?l?c?' .",
	            "FILTER regex(?o, \"lic\")"
	            };
	    for (int i = 0; i < testMatches.length; i++) {
	        String queryString = "SELECT ?s ?p ?o WHERE { ?s ?p ?o . " + testMatches[i] + " }";
            System.out.println("Query for match with " + testMatches[i]);
            TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
            TupleQueryResult result = (TupleQueryResult)tupleQuery.evaluate();
            int count = 0;
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                if (count < 5) {
                    println(bindingSet);
                }
                count += 1;
            }
            println("Found " + count + " query results");
            result.close();
        }
        */
        println("\nWhole-word match for 'Alice'.");
        String queryString = 
        	"SELECT ?s ?p ?o " +
        	"WHERE { ?s ?p ?o . ?s fti:match 'Alice' . }";
        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        TupleQueryResult result = (TupleQueryResult)tupleQuery.evaluate();
        int count = 0;
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            if (count < 5) {
                println(bindingSet);
            }
            count += 1;
        }
        result.close();
            
        println("\nWildcard match for 'Ali*'.");
        queryString = 
        	"SELECT ?s ?p ?o " +
        	"WHERE { ?s ?p ?o . ?s fti:match 'Ali*' . }";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = (TupleQueryResult)tupleQuery.evaluate();
        count = 0;
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            if (count < 5) {
                println(bindingSet);
            }
            count += 1;
        }
        result.close();
            
        println("\nWildcard match for '?l?ce?.");
        queryString = 
        	"SELECT ?s ?p ?o " +
        	"WHERE { ?s ?p ?o . ?s fti:match '?l?c?' . }";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = (TupleQueryResult)tupleQuery.evaluate();
        count = 0;
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            if (count < 5) {
                println(bindingSet);
            }
            count += 1;
        }
        result.close();
            
        println("\nSubstring match for 'lic'.");
        queryString = 
        	"SELECT ?s ?p ?o " +
        	"WHERE { ?s ?p ?o . FILTER regex(?o, \"lic\") }";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = (TupleQueryResult)tupleQuery.evaluate();
        count = 0;
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            if (count < 5) {
                println(bindingSet);
            }
            count += 1;
        }
        result.close();
            
            
            conn.close();
    }
    
    
    /**
     * Ask, Construct, and Describe queries
     */ 
    public static void example13 () throws Exception {
        RepositoryConnection conn = example2(false);
        conn.setNamespace("ex", "http://example.org/people/");
        conn.setNamespace("ont", "http://example.org/ontology/");
        println("\nSELECT result:");
        String queryString = "select ?s ?p ?o where { ?s ?p ?o} ";
        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        TupleQueryResult result = tupleQuery.evaluate();
        while (result.hasNext()) {
            println(result.next());
        }
        result.close();
        queryString = "ask { ?s ont:name \"Alice\" } ";
        BooleanQuery booleanQuery = conn.prepareBooleanQuery(QueryLanguage.SPARQL, queryString);
        boolean truth = booleanQuery.evaluate(); 
        println("\nBoolean result: " + truth);
        queryString = "construct {?s ?p ?o} where { ?s ?p ?o . filter (?o = \"Alice\") } ";
        GraphQuery constructQuery = conn.prepareGraphQuery(QueryLanguage.SPARQL, queryString);
        GraphQueryResult gresult = constructQuery.evaluate(); 
        List statements = new ArrayList();
        while (gresult.hasNext()) {
            statements.add(gresult.next());
        }
        println("\nConstruct result:\n" + statements);
        queryString = "describe ?s where { ?s ?p ?o . filter (?o = \"Alice\") } ";
        GraphQuery describeQuery = conn.prepareGraphQuery(QueryLanguage.SPARQL, queryString);
        gresult = describeQuery.evaluate(); 
        println("\nDescribe result:");
        while (gresult.hasNext()) {
            println(gresult.next());
        }
        gresult.close();
        conn.close();
    }

    /**
     * Parametric Queries
     */
    public static void example14() throws Exception {
        RepositoryConnection conn = example2(false);
        ValueFactory f = conn.getValueFactory();
        URI alice = f.createURI("http://example.org/people/alice");
        URI bob = f.createURI("http://example.org/people/bob");
        String queryString = "select ?s ?p ?o where { ?s ?p ?o} ";
        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        tupleQuery.setBinding("s", alice);
        TupleQueryResult result = tupleQuery.evaluate();
        println("\nFacts about Alice:");            
        while (result.hasNext()) {
            println(result.next());
        }
        result.close();
        tupleQuery.setBinding("s", bob);
        println("\nFacts about Bob:");    
        result = tupleQuery.evaluate();
        while (result.hasNext()) {
            println(result.next());
        }
        result.close();
        conn.close();
    }

    /**
     * Range matches
     */
    public static void example15() throws Exception {
        println("Starting example15().");
        AGRepositoryConnection conn = example1(false);
        ValueFactory f = conn.getValueFactory();
        conn.clear();
        String exns = "http://example.org/people/";
        conn.setNamespace("ex", exns);
        URI alice = f.createURI(exns, "alice");
        URI bob = f.createURI(exns, "bob");
        URI carol = f.createURI(exns, "carol");    
        URI age = f.createURI(exns, "age");    
        conn.add(alice, age, f.createLiteral(42));
        conn.add(bob, age, f.createLiteral(45.1));
        conn.add(carol, age, f.createLiteral("39"));

        println("\nRange query for integers and floats.");
        String queryString = 
        	"SELECT ?s ?p ?o  " +
        	"WHERE { ?s ?p ?o . " +
        	"FILTER ((?o >= 30) && (?o <= 50)) }";
        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        TupleQueryResult result = tupleQuery.evaluate();
        try {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Value s = bindingSet.getValue("s");
                Value p = bindingSet.getValue("p");
                Value o = bindingSet.getValue("o");
                System.out.format("%s %s %s\n", s, p, o);
            }
        } finally {
            result.close();
        }
        
        println("\nRange query for integers, floats, and integers in strings.");
        String queryString2 = 
        	"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
        	"SELECT ?s ?p ?o  " +
        	"WHERE { ?s ?p ?o . " +
        	"FILTER ((xsd:integer(?o) >= 30) && (xsd:integer(?o) <= 50)) }";
        TupleQuery tupleQuery2 = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString2);
        TupleQueryResult result2 = tupleQuery2.evaluate();
        try {
            while (result2.hasNext()) {
                BindingSet bindingSet = result2.next();
                Value s = bindingSet.getValue("s");
                Value p = bindingSet.getValue("p");
                Value o = bindingSet.getValue("o");
                System.out.format("%s %s %s\n", s, p, o);
            }
        } finally {
            result2.close();
        }

        conn.close();
    }
    
    private static void pt(String kind, TupleQueryResult rows) throws Exception {
        println("\n" + kind + " Apples:\t");
        while (rows.hasNext()) {
            println(rows.next());
        }
        rows.close();
    }

    /**
     * Federated triple stores.
     */
    public static void example16() throws Exception {
        AGRepositoryConnection conn = example6(false);
        AGRepository myRepository = conn.getRepository();
        AGCatalog catalog = myRepository.getCatalog();
        // create two ordinary stores, and one federated store:
        AGRepository redRepo = catalog.createRepository("redthings");
        redRepo.initialize();
        RepositoryConnection redConn = redRepo.getConnection();
        redConn.clear();
        ValueFactory rf = redConn.getValueFactory();
        AGRepository greenRepo = catalog.createRepository("greenthings");
        greenRepo.initialize();
        RepositoryConnection greenConn = greenRepo.getConnection();
        greenConn.clear();
        ValueFactory gf = greenConn.getValueFactory();
        AGServer server = myRepository.getCatalog().getServer();
        AGRepository rainbowRepo = server.createFederation("rainbowthings",redRepo, greenRepo);
        rainbowRepo.initialize();
        RepositoryConnection rainbowConn = rainbowRepo.getConnection();
        String ex = "http://www.demo.com/example#";
        // add a few triples to the red and green stores:
        redConn.add(rf.createURI(ex+"mcintosh"), RDF.TYPE, rf.createURI(ex+"Apple"));
        redConn.add(rf.createURI(ex+"reddelicious"), RDF.TYPE, rf.createURI(ex+"Apple"));    
        greenConn.add(gf.createURI(ex+"pippin"), RDF.TYPE, gf.createURI(ex+"Apple"));
        greenConn.add(gf.createURI(ex+"kermitthefrog"), RDF.TYPE, gf.createURI(ex+"Frog"));
        redConn.setNamespace("ex", ex);
        greenConn.setNamespace("ex", ex);
        rainbowConn.setNamespace("ex", ex);
        String queryString = "select ?s where { ?s rdf:type ex:Apple }";
        // query each of the stores; observe that the federated one is the union of the other two:
        pt("red", redConn.prepareTupleQuery(QueryLanguage.SPARQL, queryString).evaluate());
        pt("green", greenConn.prepareTupleQuery(QueryLanguage.SPARQL, queryString).evaluate());
        pt("federated", rainbowConn.prepareTupleQuery(QueryLanguage.SPARQL, queryString).evaluate());
    }

    /**
     * Prolog queries
     */
    public static void example17() throws Exception {
        AGRepositoryConnection conn = example6(false);
 //       AGRepository myRepository = conn.getRepository();
        conn.setNamespace("kdy", "http://www.franz.com/simple#");
//        conn.setRuleLanguage(AGQueryLanguage.PROLOG);
        String rules1 =
            "(<-- (woman ?person) ;; IF\n" +
            "     (q ?person !kdy:sex !kdy:female)\n" +
            "     (q ?person !rdf:type !kdy:person))\n" +
            "(<-- (man ?person) ;; IF\n" +
            "     (q ?person !kdy:sex !kdy:male)\n" +
            "     (q ?person !rdf:type !kdy:person))";
        conn.addRules(rules1);
        println("\nFirst and Last names of all \"men.\"");
        String queryString =
            "(select (?first ?last)\n" +
            "        (man ?person)\n" +
            "        (q ?person !kdy:first-name ?first)\n" +
            "        (q ?person !kdy:last-name ?last))";
        TupleQuery tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
        TupleQueryResult result = tupleQuery.evaluate();
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            Value f = bindingSet.getValue("first");
            Value l = bindingSet.getValue("last");
            println(f + " " + l);
        }
        result.close();
        conn.close();
    }

    /**
     * Loading Prolog rules
     */
    public static void example18() throws Exception {
        AGRepositoryConnection conn = example6(false);
        conn.setNamespace("kdy", "http://www.franz.com/simple#");
        conn.setNamespace("rltv", "http://www.franz.com/simple#");
//        conn.setRuleLanguage(AGQueryLanguage.PROLOG);
        String path = "src/relative_rules.txt";
        conn.addRules(new FileInputStream(path));
        String queryString = 
        	"(select (?person ?uncle) " +
        		"(uncle ?y ?x)" +
        		"(name ?x ?person)" +
        		"(name ?y ?uncle))";
        TupleQuery tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
        TupleQueryResult result = tupleQuery.evaluate();     
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            Value p = bindingSet.getValue("person");
            Value u = bindingSet.getValue("uncle");
            println(u + " is the uncle of " + p);
        }
        result.close();
        conn.close();
    }

    /**
     * RDFS++ Reasoning
     */
    public static void example19() throws Exception {
        AGRepositoryConnection conn = example1(false);
        // Examples of RDFS++ inference.  Was originally example 2A.
        ValueFactory f = conn.getValueFactory();
        URI robert = f.createURI("http://example.org/people/robert");
        URI roberta = f.createURI("http://example.org/people/roberta");
        URI bob = f.createURI("http://example.org/people/bob");
        URI bobby = f.createURI("http://example.org/people/bobby");
        // create name and child predicates, and Person class.
        URI name = f.createURI("http://example.org/ontology/name");
        URI fatherOf = f.createURI("http://example.org/ontology/fatherOf");
        URI person = f.createURI("http://example.org/ontology/Person");
        // create literal values for names    
        Literal bobsName = f.createLiteral("Bob");
        Literal bobbysName = f.createLiteral("Bobby");
        Literal robertsName = f.createLiteral("Robert");
        Literal robertasName = f.createLiteral("Roberta");
        // Bob is the same person as Robert
        conn.add(bob, OWL.SAMEAS, robert);
        // Robert, Bob, and children are people
        conn.add(robert, RDF.TYPE, person);
        conn.add(roberta, RDF.TYPE, person);
        conn.add(bob, RDF.TYPE, person);
        conn.add(bobby, RDF.TYPE, person);
        // They all have names.
        conn.add(robert, name, robertsName);
        conn.add(roberta, name, robertasName);
        conn.add(bob, name, bobsName);
        conn.add(bobby, name, bobbysName);
        // robert has a child
        conn.add(robert, fatherOf, roberta);
        // bob has a child
        conn.add(bob, fatherOf, bobby);
        
        // List the children of Robert, with inference OFF.
        println("/nChildren of Robert, inference OFF");
        printRows( conn.getStatements(robert, fatherOf, null, false) );
        // List the children of Robert with inference ON. The owl:sameAs
        // link combines the children of Bob with those of Robert.
        println("/nChildren of Robert, inference ON");
        printRows( conn.getStatements(robert, fatherOf, null, true) );
        // Remove the owl:sameAs link so we can try the next example.
        conn.remove(bob, OWL.SAMEAS, robert);
        
        // Define new predicate, hasFather, as the inverse of fatherOf.
        URI hasFather = f.createURI("http://example.org/ontology/hasFather");
        conn.add(hasFather, OWL.INVERSEOF, fatherOf);
        // Search for people who have fathers, even though there are no hasFather triples.
        // With inference OFF.
        println("/nPeople with fathers, inference OFF");
        printRows( conn.getStatements(null, hasFather, null, false) );
        // With inference ON. The owl:inverseOf link allows AllegroGraph to
        // deduce the inverse links.
        println("/nPeople with fathers, inference ON");
        printRows( conn.getStatements(null, hasFather, null, true) );
        // Remove owl:inverseOf property.
        conn.remove(hasFather, OWL.INVERSEOF, fatherOf);

//         Next 12 lines were for owl:inverseFunctionalProperty, but that isn't
//         supported yet in AG.  Commenting them out. 
//         Add fatherOf link from Robert to Bobby, giving Bobby two fathers. 
//        conn.add(robert, fatherOf, bobby)
//         Now make fatherOf a 'reverse functional property'
//        conn.add(fatherOf, RDF.TYPE, OWL.INVERSEFUNCTIONALPROPERTY)
//         Bob has how many children? 
//         With inference OFF.
//        print "Who is Bob the father of, inference OFF"
//        for s in conn.getStatements(bob, fatherOf, None, None): print s    
//         With inference ON. AllegroGraph knows that Bob and Robert must
//         be the same person.
//        print "Who is Bob the father of, inference ON"
//        for s in conn.getStatements(bob, fatherOf, None, None, True): print s  
//         Subproperty example.  We'll make fatherOf an rdfs:subpropertyOf parentOf.

        URI parentOf = f.createURI("http://example.org/ontology/parentOf");
        conn.add(fatherOf, RDFS.SUBPROPERTYOF, parentOf);
        // Now search for inferred parentOf links.
        // Search for parentOf links, even though there are no parentOf triples.
        // With inference OFF.
        println("/nPeople with parents, inference OFF");
        printRows( conn.getStatements(null, parentOf, null, false) );
        // With inference ON. The rdfs:subpropertyOf link allows AllegroGraph to 
        // deduce that fatherOf links imply parentOf links.
        println("/nPeople with parents, inference ON");
        printRows( conn.getStatements(null, parentOf, null, true) );
        conn.remove(fatherOf, RDFS.SUBPROPERTYOF, parentOf);
        
        // The next example shows rdfs:range and rdfs:domain in action.
        // We'll create two new rdf:type classes.  Note that classes are capitalized.
        URI parent = f.createURI("http://example.org/ontology/Parent");
        URI child = f.createURI("http://exmaple.org/ontology/Child");
        // The following triples say that a fatherOf link points from a parent to a child.
        conn.add(fatherOf, RDFS.DOMAIN, parent);
        conn.add(fatherOf, RDFS.RANGE, child);
        // Now we can search for rdf:type parent.
        println("/nWho are the parents?  Inference ON.");
        printRows( conn.getStatements(null, RDF.TYPE, parent, true) );
        // And we can search for rdf:type child.
        println("/nWho are the children?  Inference ON.");
        printRows( conn.getStatements(null, RDF.TYPE, child, true) );
        conn.close();
    }
    
    /**
     * Geospatial Reasoning
     */
    /*
    public static void example20() throws Exception {
        AGRepositoryConnection conn = example1(false);
        ValueFactory f = conn.getValueFactory();
        conn = example1(false);
        conn.clear();
        println("Starting example20().");
        String exns = "http://example.org/people/";
        conn.setNamespace("ex", exns);
        URI alice = f.createURI(exns, "alice");
        URI bob = f.createURI(exns, "bob");
        URI carol = f.createURI(exns, "carol");
//        conn.createRectangularSystem(scale=1, xMax=100, yMax=100);
        URI location = f.createURI(exns, "location");
        //conn.registerDatatypeMapping(predicate=location, nativeType="int")   
        //conn.registerDatatypeMapping(predicate=location, nativeType="float")       
        conn.add(alice, location, conn.createCoordinate(30,30));
        conn.add(bob, location, conn.createCoordinate(40, 40));
        conn.add(carol, location, conn.createCoordinate(50, 50)); 
        Object box1 = conn.createBox(20, 40, 20, 40);
        println(box1);
        println("Find people located within box1.");
        printRows( conn.getStatements(null, location, box1) );
        circle1 = conn.createCircle(35, 35, radius=10);  
        println(circle1);
        println("Find people located within circle1.");
        printRows( conn.getStatements(null, location, circle1) ); 
        Object polygon1 = conn.createPolygon([(10,40), (50,10), (35,40), (50,70)]);
        println(polygon1);
        println("Find people located within polygon1.");
        printRows( conn.getStatements(null, location, polygon1) );
        // now we switch to a LatLong (spherical) coordinate system
        //latLongGeoType = conn.createLatLongSystem(scale=5) #, unit='km')
//        latLongGeoType = conn.createLatLongSystem(scale=5, unit='degree');
        URI amsterdam = f.createURI(exns, "amsterdam");
        URI london = f.createURI(exns, "london");
        URI sanfrancisco = f.createURI(exns, "sanfrancisco");
        URI salvador = f.createURI(exns, "salvador");
        location = f.createURI(exns, "geolocation");
    //    conn.registerDatatypeMapping(predicate=location, nativeType="float")
        conn.add(amsterdam, location, conn.createCoordinate(52.366665, 4.883333));
        conn.add(london, location, conn.createCoordinate(51.533333, -0.08333333));
        conn.add(sanfrancisco, location, conn.createCoordinate(37.783333, -122.433334));
        conn.add(salvador, location, conn.createCoordinate(13.783333, -88.45));
        Object box2 = conn.createBox( 25.0, 50.0, -130.0, -70.0);
        println(box2);
        println("Locate entities within box2.");
        printRows( conn.getStatements(null, location, box2) );
        circle2 = conn.createCircle(19.3994, -99.08, 2000, "km");
        println(circle2);
        println("Locate entities within circle2.");
        printRows( conn.getStatements(None, location, circle2) );
        polygon2 = conn.createPolygon([(51.0, 2.00),(60.0, -5.0),(48.0,-12.5)]);
        println(polygon2);
        println("Locate entities within polygon2.");
        printRows( conn.getStatements(None, location, polygon2) );
        conn.close();
    }
    */

// TODO: example21() Social Network Analysis Reasoning

    /**
     * Test of dedicated session Commit/Rollback
     */
    public static void example22() throws Exception {
        // Create common session and dedicated session.
        AGServer server = new AGServer(SERVER_URL, USERNAME, PASSWORD);
        AGCatalog catalog = server.getCatalog(CATALOG_ID);
        AGRepository myRepository = catalog.createRepository("agraph_test");
        myRepository.initialize();
        AGRepositoryConnection common = myRepository.getConnection();
        closeBeforeExit(common);
        AGRepositoryConnection dedicated = myRepository.getConnection();
        closeBeforeExit(dedicated);
        common.clear();
        dedicated.clear();
//        dedicated.openSession()  // open dedicated session 
        // The following paths are relative to os.getcwd(), the working directory.
        println("Current working directory is: " + new File(".").getAbsolutePath());
        //Load LasMis into common session, Kennedy into dedicated session.
        String path1 = "src/kennedy.ntriples";
        String path2 = "src/lesmis.rdf";
        String baseURI = "http://example.org/example/local";
        // read kennedy triples into the dedicated session:
        println("Load 1214 kennedy.ntriples into dedicated session.");
        dedicated.add(new File(path1), baseURI, RDFFormat.NTRIPLES);
        // read lesmis triples into the common session:
        println("Load 916 lesmis triples into the common session.");
        common.add(new File(path2), baseURI, RDFFormat.RDFXML);
        
        println("\nSince we have done neither a commit nor a rollback, queries directed");
        println("to one back end should not be able to retreive triples from the other connection.");
        println("\nAfter loading, there are:");
        println(dedicated.size() + " kennedy triples in context 'null' of the dedicated session;");
        println(common.size() + " lesmis triples in context 'null' of the common session.");
        println("The answers should be 1214, and 916. ");
        // Check for partitioning:
        //    Look for Valjean in common session, should find it.
        //    Look for Kennedy in common session, should not find it.
        //    Look for Kennedy in dedicated session, should find it.
        //    Look for Valjean in dedicated session, should not find it.
        Literal valjean = common.getValueFactory().createLiteral("Valjean");
        Literal kennedy = dedicated.getValueFactory().createLiteral("Kennedy");
        printRows("\nUsing getStatements() on common session; should find Valjean:",
                1, common.getStatements(null, null, valjean, false));
// limit=1
        printRows("\nUsing getStatements() on common session; should not find Kennedy:",
                1, common.getStatements(null, null, kennedy, false));
// limit=1
        printRows("\nUsing getStatements() on dedicated session; should not find Valjean:",
                1, dedicated.getStatements(null, null, valjean, false));
// limit=1
        printRows("\nUsing getStatements() on dedicated session; should find Kennedy:",
                1, dedicated.getStatements(null, null, kennedy, false));
// limit=1
        
        // Rollback
        // Check for partitioning:
        //     Look for LesMis in common session, should find it.
        //     Look for Kennedy in common session, should not find it.
        //     Look for Kennedy in dedicated session, should not find it.
        //     Look for LesMis in dedicated session, should find it.
        println("\nRolling back contents of dedicated session.");
        dedicated.rollback();
        valjean = common.getValueFactory().createLiteral("Valjean");
        kennedy = dedicated.getValueFactory().createLiteral("Kennedy");
        printRows("\nUsing getStatements() on common session; should find Valjean:",
                1, common.getStatements(null, null, valjean, false));
        printRows("\nUsing getStatements() on common session; should not find Kennedys:",
                1, common.getStatements(null, null, kennedy, false));
        printRows("\nUsing getStatements() on dedicated session; should not find Kennedys:",
                1, dedicated.getStatements(null, null, kennedy, false));
        printRows("\nUsing getStatements() on dedicated session; should find Valjean:",
                1, dedicated.getStatements(null, null, valjean, false));
        // Reload the Kennedy data into the dedicated session.
        // Commit dedicated session.
        // Check for partitioning:
        //     Look for LesMis in common session, should find it.
        //     Look for Kennedy in common session, should find it.
        //     Look for Kennedy in dedicated session, should find it.
        //     Look for LesMis in dedicated session, should find it.
        // read kennedy triples into the dedicated session:
        println("\nReload 1214 kennedy.ntriples into dedicated session.");
        dedicated.add(new File(path1), baseURI, RDFFormat.NTRIPLES);
        println("\nCommitting contents of dedicated session.");
        dedicated.commit();
        valjean = common.getValueFactory().createLiteral("Valjean");
        kennedy = dedicated.getValueFactory().createLiteral("Kennedy");
        printRows("\nUsing getStatements() on common session; should find Valjean:",
                1, common.getStatements(null, null, valjean, false));
        printRows("\nUsing getStatements() on common session; should find Kennedys:",
                1, common.getStatements(null, null, kennedy, false));
        printRows("\nUsing getStatements() on dedicated session; should find Kennedys:",
                1, dedicated.getStatements(null, null, kennedy, false));
        printRows("\nUsing getStatements() on dedicated session; should find Valjean:",
                1, dedicated.getStatements(null, null, valjean, false));
    }



    /**
     * Usage: all
     * Usage: [1-14]+
     */
    public static void main(String[] args) throws Exception {
        List<Integer> choices = new ArrayList<Integer>();
        if (args.length == 0) {
            // for choosing by editing this code
            choices.add(6);
        } else if (args[0].equals("all")) {
            for (int i = 1; i <= 15; i++) {
                choices.add(i);
            }
        } else {
            for (int i = 0; i < args.length; i++) {
                choices.add(Integer.parseInt(args[i]));
            }
        }
        try {
            for (Integer choice : choices) {
                println("Running example " + choice);
                switch(choice) {
                case 1: example1(true); break;
                case 2: example2(true); break;            
                case 3: example3(); break;            
                case 4: example4(); break;                        
                case 5: example5(); break;                                    
                case 6: example6(true); break;    
                case 7: example7(); break;
                case 8: example8(); break;            
                case 9: example9(); break;    
                case 10: example10(); break;
                case 11: example11(); break;            
                case 12: example12(); break;                        
                case 13: example13(); break;                                    
                case 14: example14(); break;                                    
                case 15: example15(); break;                                    
                //case 16: example16(); break;                                    
                case 17: example17(); break;                                    
                case 18: example18(); break;                                    
                case 19: example19(); break;                                    
                //case 20: example20(); break;
                //case 21: example21(); break;            
                //case 22: example22(); break;                        
                default: throw new IllegalArgumentException("There is no example " + choice);
                }
            }
        } finally {
            for (AGRepositoryConnection conn : toClose) {
                close(conn);
            }
        }
    }
    
    public static void println(Object x) {
        System.out.println(x);
    }
    
    static void printRows(RepositoryResult<Statement> rows) throws Exception {
        while (rows.hasNext()) {
            println(rows.next());
        }
        rows.close();
    }

    static void printRows(String headerMsg, int limit, RepositoryResult<Statement> rows) throws Exception {
        int count = 0;
        while (count > limit && rows.hasNext()) {
            println(rows.next());
            count++;
        }
        println("Number of results: " + count);
        rows.close();
    }

    static void close(AGRepositoryConnection conn) {
        try {
            conn.close();
        } catch (Exception e) {
            System.err.println("Error closing repository connection: " + e);
            e.printStackTrace();
        }
    }
    
    private static List<AGRepositoryConnection> toClose = new ArrayList<AGRepositoryConnection>();
    
    /**
     * This is just a quick mechanism to make sure all connections get closed.
     */
    private static void closeBeforeExit(AGRepositoryConnection conn) {
        toClose.add(conn);
    }
    
}
