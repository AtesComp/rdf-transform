package org.openrefine.rdf.model.vocab;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.Lang;
import org.openrefine.rdf.model.Util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VocabularyImporter {
    static private final Logger logger = LoggerFactory.getLogger("RDFT:VocabImporter");

    static private final String USER_AGENT = "OpenRefine.Extension.RDF-Transform";

    static private final String PREFIXES = // Default Namespaces...
        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
        "PREFIX owl: <http://www.w3.org/2002/07/owl#> " +
        "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> ";

    static private final String CLASSES_QUERY =
        VocabularyImporter.PREFIXES +
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
    

    static private final String PROPERTIES_QUERY =
        VocabularyImporter.PREFIXES +
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
    private boolean bXMLSchema;
    private Model modelImport;

    public VocabularyImporter(String strPrefix, String strNamespace) {
        this.strPrefix = strPrefix;
        this.strNamespace = strNamespace;
        this.bStrictlyRDF = false;
        this.bXMLSchema = false;
        this.modelImport = null;
    }

    public void importVocabulary(String strFetchURL, List<RDFTClass> classes, List<RDFTProperty> properties)
            throws VocabularyImportException
    {
        if ( Util.isDebugMode() ) VocabularyImporter.logger.info("DEBUG: Import by given URL: " + strFetchURL);
        if (strFetchURL == null) {
            if ( Util.isDebugMode() ) VocabularyImporter.logger.info("DEBUG: Nothing to import! URL is null.");
            return;
        }
        this.getModel(strFetchURL);
        if (this.modelImport == null) {
            return;
        }
        this.getTerms(classes, properties);
    }

    public void importVocabulary(Model model, List<RDFTClass> classes, List<RDFTProperty> properties)
            throws VocabularyImportException
    {
        if ( Util.isDebugMode() ) VocabularyImporter.logger.info("DEBUG: Import by existing model...");
        this.modelImport = model;
        if (this.modelImport == null) {
            if ( Util.isDebugMode() ) VocabularyImporter.logger.info("DEBUG: Nothing to import! Import model is null.");
        }
        this.getTerms(classes, properties);
    }

    private void getModel(String strFetchURL)
            throws VocabularyImportException
    {
        this.modelImport = null;

        this.faultyContentNegotiation(strFetchURL); // ...set up booleans...
        if (this.bXMLSchema) {
            VocabularyImporter.logger.info("NOTE: XMLSchema is a built-in Datatype ontology...skipping import.");
            return;
        }

        try {
            if ( Util.isDebugMode() ) VocabularyImporter.logger.info("DEBUG: Creating model for URL");
            this.modelImport = ModelFactory.createDefaultModel();

            if ( Util.isDebugMode() ) VocabularyImporter.logger.info("DEBUG: Reading model from URL");
            if (this.bStrictlyRDF) {
                this.modelImport.read( strFetchURL, Lang.RDFXML.getName() ); ;
            }
            else {
                this.modelImport.read(strFetchURL);
            }
        }
        catch (Exception ex) {
            this.modelImport = null;
            throw new VocabularyImportException("ERROR: Importing vocabulary: " + strFetchURL, ex);
        }
    }

    protected void getTerms(List<RDFTClass> classes, List<RDFTProperty> properties)
        throws VocabularyImportException
    {
        // Set RDFS Class and Property load conditions...
        String[] astrLoader = new String[6];
        astrLoader[RDFTNode.iPrefix] = this.strPrefix;
        astrLoader[RDFTNode.iNamespace] = this.strNamespace;
        astrLoader[RDFTNode.iLocalPart] = "";    // ...empty string, not null, since we purposely
                                                // need to extract the LocalPart from the IRI later
                                                // during the Class / Property Node creation.
                                                // A null is an error condition.
        //
        // Get classes...
        //
        if ( Util.isDebugMode() ) VocabularyImporter.logger.info("DEBUG: Querying classes from ontology...");
        this.queryClasses(classes, astrLoader);

        //
        // Get properties...
        //
        if ( Util.isDebugMode() ) VocabularyImporter.logger.info("DEBUG: Querying properties from ontology...");
        this.queryProperties(properties, astrLoader);
    }

    private void queryClasses(List<RDFTClass> classes, String[] astrLoader)
            throws VocabularyImportException
    {
        Set<String> seen = new HashSet<String>();

        Query query = QueryFactory.create( String.format(VocabularyImporter.CLASSES_QUERY, this.strNamespace) );
        try (QueryExecution qexec = QueryExecutionFactory.create(query, this.modelImport)) {
            ResultSet results = qexec.execSelect();
            while ( results.hasNext() ) {
                QuerySolution solution = results.nextSolution();
                if ( processSolution(solution, astrLoader, seen) ) {
                    classes.add( new RDFTClass(astrLoader) );
                }
            }
        }
        catch (Exception ex) {
            throw new VocabularyImportException("ERROR: Processing vocabulary [" + this.strPrefix + "] classes: " + ex.getMessage(), ex);
        }
    }

    private void queryProperties(List<RDFTProperty> properties, String[] astrLoader)
            throws VocabularyImportException
    {
        Set<String> seen = new HashSet<String>();

        Query query = QueryFactory.create( String.format(VocabularyImporter.PROPERTIES_QUERY, this.strNamespace) );
        try (QueryExecution qexec = QueryExecutionFactory.create(query, this.modelImport)) {
            ResultSet results = qexec.execSelect();
            while ( results.hasNext() ) {
                QuerySolution solution = results.nextSolution();
                if ( processSolution(solution, astrLoader, seen) ) {
                    properties.add( new RDFTProperty(astrLoader) );
                }
            }
        }
        catch (Exception ex) {
            throw new VocabularyImportException("ERROR: Processing vocabulary [" + this.strPrefix + "] properties: " + ex.getMessage(), ex);
        }
    }

    private boolean processSolution(QuerySolution solution, String[] astrLoader, Set<String> seen) {
        astrLoader[RDFTNode.iIRI] = null;
        RDFNode node = solution.get("resource");
        if (node != null) { // ...should never be null...
            astrLoader[RDFTNode.iIRI] = node.toString();
        }
        if ( seen.contains(astrLoader[RDFTNode.iIRI]) ) {
            return false;
        }
        seen.add(astrLoader[RDFTNode.iIRI]);
        astrLoader[RDFTNode.iLabel] =
            getFirstNotNullLiteral( new RDFNode[] {
                                solution.get("en_label"),
                                solution.get("label")
                            } );
        astrLoader[RDFTNode.iDesc] =
            getFirstNotNullLiteral( new RDFNode[] {
                                solution.get("en_definition"),  // 1: Definitions
                                solution.get("definition"),
                                solution.get("en_description"), // 2: Descriptions
                                solution.get("description")
                            } );
        return true;
    }

    private String getFirstNotNullLiteral(RDFNode[] nodes) {
        String strNode = null;
        for (int i = 0; i < nodes.length; i++) {
            strNode = getString(nodes[i]);
            if (strNode != null) {
                break;
            }
        }
        return strNode;
    }

    private String getString(RDFNode node) {
        String strLabel = null;
        if (node != null) {
            try {
                strLabel = node.asLiteral().getLexicalForm();
            }
            catch (Exception ex) {
                if ( Util.isDebugMode() ) VocabularyImporter.logger.warn("DEBUG: Expected Literal");
                strLabel = null;
            }
        }
        return strLabel;
    }

    private void faultyContentNegotiation(String strNamespace) {
        // SKOS: We add an exceptional treatment for SKOS as their deployment does not handle an
        //       "Accept" header properly!  SKOS always returns "HTML" if the "Accept" header
        //       contains HTML regardless other more preferred options.
        this.bStrictlyRDF = strNamespace.equals("http://www.w3.org/2004/02/skos/core#");
        this.bXMLSchema = strNamespace.equals("http://www.w3.org/2001/XMLSchema#");
    }
}
