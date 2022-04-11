package com.google.refine.rdf.model.vocab;

import org.apache.any23.Any23;
//import org.apache.any23.http.HTTPClient;
import org.apache.any23.source.HTTPDocumentSource;
import org.apache.any23.writer.ReportingTripleHandler;
import org.apache.any23.writer.RepositoryWriter;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencer;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VocabularyImporter {
    private static final String USER_AGENT = "OpenRefine.Extension.RDF-Transform";

    private static final String PREFIXES = // Default Namespaces...
        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
        "PREFIX owl: <http://www.w3.org/2002/07/owl#> " +
        "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> ";

    private static final String CLASSES_QUERY =
        PREFIXES +
        "SELECT ?resource ?label ?en_label ?description ?en_description ?definition ?en_definition " +
        "WHERE { " +
        "?resource rdf:type ?type . " +
        "OPTIONAL { ?resource rdfs:label ?label . } " +
        "OPTIONAL { ?resource rdfs:label ?en_label . " +
                    "FILTER langMatches( lang(?en_label), \"EN\" ) } " +
        "OPTIONAL { ?resource rdfs:comment ?description . } " +
        "OPTIONAL { ?resource rdfs:comment ?en_description . FILTER langMatches( lang(?en_description), \"EN\" ) } " +
        "OPTIONAL { ?resource skos:definition ?definition . } " +
        "OPTIONAL { ?resource skos:definition ?en_definition . " +
                    "FILTER langMatches( lang(?en_definition), \"EN\" ) } " +
        "VALUES ?type { rdfs:Class owl:Class } " +
        "FILTER regex(str(?resource), \"^%s\") }";

    private static final String PROPERTIES_QUERY =
        PREFIXES +
        "SELECT ?resource ?label ?en_label ?description ?en_description ?definition ?en_definition " +
        "WHERE { " +
        "?resource rdf:type rdf:Property . " +
        "OPTIONAL { ?resource rdfs:label ?label . } " +
        "OPTIONAL { ?resource rdfs:label ?en_label . " +
                    "FILTER langMatches( lang(?en_label), \"EN\" ) } " +
        "OPTIONAL { ?resource rdfs:comment ?description . } " +
        "OPTIONAL { ?resource rdfs:comment ?en_description . " +
                    "FILTER langMatches( lang(?en_description), \"EN\" ) } " +
        "OPTIONAL { ?resource skos:definition ?definition.} " +
        "OPTIONAL { ?resource skos:definition ?en_definition . " +
                    "FILTER langMatches( lang(?en_definition), \"EN\" ) } " +
        "FILTER regex(str(?resource), \"^%s\") }";

    private String strPrefix;
    private String strNamespace;
    private boolean bStrictlyRDF;
    private Repository repository;

    public VocabularyImporter(String strPrefix, String strNamespace) {
        this.strPrefix = strPrefix;
        this.strNamespace = strNamespace;
        this.bStrictlyRDF = false;
    }

    public void importVocabulary(String strFetchURL,
                                    List<RDFSClass> classes, List<RDFSProperty> properties)
            throws VocabularyImportException
    {
        this.getModel(strFetchURL);
        this.getTerms(classes, properties);
    }

    public void importVocabulary(Repository repository,
                                    List<RDFSClass> classes, List<RDFSProperty> properties)
            throws VocabularyImportException {
        this.repository = repository;
        this.getTerms(classes, properties);
    }

    private void getModel(String strFetchURL)
            throws VocabularyImportException {
        this.faultyContentNegotiation(strFetchURL);
        Any23 extractor;
        try {
            if (this.bStrictlyRDF) {
                extractor = new Any23("rdf-xml");
            }
            else {
                extractor = new Any23();
            }
            extractor.setHTTPUserAgent(USER_AGENT);
            HTTPDocumentSource source = new HTTPDocumentSource(extractor.getHTTPClient(), strFetchURL);

            this.repository =
                new SailRepository( new SchemaCachingRDFSInferencer( new MemoryStore() ) );
            this.repository.init();

            RepositoryConnection connnect = this.repository.getConnection();
            RepositoryWriter writer = new RepositoryWriter(connnect);
            ReportingTripleHandler reporter = new ReportingTripleHandler(writer);

            extractor.extract(source, reporter);
        }
        catch (Exception ex) {
            throw new VocabularyImportException("Error importing vocabulary " + strFetchURL, ex);
        }
    }

    protected void getTerms(List<RDFSClass> classes, List<RDFSProperty> properties)
        throws VocabularyImportException
    {
        // Set RDFS Class and Property load conditions...
        String[] astrLoader = new String[6];
        astrLoader[RDFNode.iPrefix] = this.strPrefix;
        astrLoader[RDFNode.iNamespace] = this.strNamespace;
        astrLoader[RDFNode.iLocalPart] = "";    // ...empty string, not null, since we purposely
                                                // need to extract the LocalPart from the IRI later
                                                // during the Class / Property Node creation.
                                                // A null is an error condition.

        try {
            RepositoryConnection connection = this.repository.getConnection();
            try {
                //
                // Get classes...
                //
                TupleQuery query =
                    connection.
                        prepareTupleQuery(QueryLanguage.SPARQL,
                                            String.format(CLASSES_QUERY, this.strNamespace) );
                TupleQueryResult results = query.evaluate();

                Set<String> seen = new HashSet<String>();
                while ( results.hasNext() ) {
                    BindingSet solution = results.next();
                    astrLoader[RDFNode.iIRI] = solution.getValue("resource").stringValue();
                    if ( seen.contains(astrLoader[RDFNode.iIRI]) ) {
                        continue;
                    }
                    seen.add(astrLoader[RDFNode.iIRI]);
                    astrLoader[RDFNode.iLabel] =
                        getFirstNotNull( new Value[] {
                                            solution.getValue("en_label"),
                                            solution.getValue("label")
                                        } );
                    astrLoader[RDFNode.iDesc] =
                        getFirstNotNull( new Value[] {
                                            solution.getValue("en_definition"),  // 1: Definitions
                                            solution.getValue("definition"),
                                            solution.getValue("en_description"), // 2: Descriptions
                                            solution.getValue("description")
                                        } );
                    RDFSClass nodeClass = new RDFSClass(astrLoader);
                    classes.add(nodeClass);
                }

                //
                // Get properties...
                //
                query =
                    connection.
                        prepareTupleQuery(QueryLanguage.SPARQL,
                                            String.format(PROPERTIES_QUERY, this.strNamespace) );
                results = query.evaluate();
                seen = new HashSet<String>();
                while ( results.hasNext() ) {
                    BindingSet solution = results.next();
                    astrLoader[RDFNode.iIRI] = solution.getValue("resource").stringValue();
                    if ( seen.contains(astrLoader[RDFNode.iIRI]) ) {
                        continue;
                    }
                    seen.add(astrLoader[RDFNode.iIRI]);
                    astrLoader[RDFNode.iLabel] =
                        getFirstNotNull( new Value[] {
                                            solution.getValue("en_label"),
                                            solution.getValue("label")
                                        } );
                    astrLoader[RDFNode.iDesc] =
                        getFirstNotNull( new Value[] {
                                            solution.getValue("en_definition"),  // 1: Definitions
                                            solution.getValue("definition"),
                                            solution.getValue("en_description"), // 2: Descriptions
                                            solution.getValue("description")
                                        } );
                    RDFSProperty nodeProp = new RDFSProperty(astrLoader);
                    properties.add(nodeProp);
                }
            }
            catch (Exception ex) {
                throw new VocabularyImportException("Error processing vocabulary [" + this.strPrefix + "]", ex);
            }
            finally {
                connection.close();
            }
        }
        catch (RepositoryException ex) {
            throw new VocabularyImportException("Error connecting to repository [" + this.strPrefix + "]", ex);
        }
    }

    private String getFirstNotNull(Value[] values) {
        String s = null;
        for (int i = 0; i < values.length; i++) {
            s = getString(values[i]);
            if (s != null) {
                break;
            }
        }
        return s;
    }

    private String getString(Value val) {
        if (val != null) {
            return val.stringValue();
        }
        return null;
    }

    private void faultyContentNegotiation(String strNamespace) {
        // SKOS: We add an exceptional treatment for SKOS as their deployment does not handle an
        //       "Accept" header properly!  SKOS always returns "HTML" if the "Accept" header
        //       contains HTML regardless other more preferred options.
        this.bStrictlyRDF = strNamespace.equals("http://www.w3.org/2004/02/skos/core#");
    }
}
