/*
 *  Class VocabularyImporter
 *
 *  A Vocabulary Importer class used to manage and parse vocabulary imports for
 *  an RDF Transform.
 *
 *  Copyright 2025 Keven L. Ates
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.openrefine.rdf.model.vocab;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
//import org.apache.jena.rdf.model.Model;
//import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.Lang;
import org.openrefine.rdf.model.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VocabularyImporter {
    static private final Logger logger = LoggerFactory.getLogger("RDFT:VocabImporter");

    //static private final String USER_AGENT = "OpenRefine.Extension.RDF-Transform";

    private String m_strPrefix = null;
    private String m_strNamespace = null;
    private String m_strLocation = null;
    private DatasetGraph m_theDSGraph = null;

    // Faulty Content Negotiators to Modify Processing...
    private boolean bStrictlyRDF = false;

    public VocabularyImporter(String strPrefix, String strNamespace, String strLocation) {
        this.m_strPrefix = strPrefix;
        this.m_strNamespace = strNamespace;
        this.m_strLocation = strLocation;
    }

    public void importVocabulary(List<RDFTClass> classes, List<RDFTProperty> properties)
            throws VocabularyImportException
    {
        if (this.m_theDSGraph != null) this.m_theDSGraph.clear(); // ...erase all the old data
        if ( Util.isDebugMode() ) VocabularyImporter.logger.info("DEBUG: Import by given URL: " + this.m_strLocation);
        if (this.m_strLocation == null) {
            if ( Util.isDebugMode() ) VocabularyImporter.logger.info("DEBUG: Nothing to import! URL is null.");
            return;
        }
        this.getDatasetGraph();
        if (this.m_theDSGraph == null) {
            if ( Util.isDebugMode() ) VocabularyImporter.logger.info("DEBUG: Nothing to import! Import dataset graph is null.");
            return;
        }
        this.getTerms(classes, properties);
    }

    public void importVocabulary(DatasetGraph theDSGraph, List<RDFTClass> classes, List<RDFTProperty> properties)
            throws VocabularyImportException
    {
        if (this.m_theDSGraph != null) this.m_theDSGraph.clear(); // ...erase all the old data
        if ( Util.isDebugMode() ) VocabularyImporter.logger.info("DEBUG: Import by given dataset graph...");
        this.m_theDSGraph = theDSGraph;
        if (this.m_theDSGraph == null) {
            if ( Util.isDebugMode() ) VocabularyImporter.logger.info("DEBUG: Nothing to import! Import dataset graph is null.");
            return;
        }
        this.getTerms(classes, properties);
    }

    private void getDatasetGraph()
            throws VocabularyImportException
    {
        this.m_theDSGraph = null;

        // Check the Namespace for process modification...
        if ( this.faultyContentNegotiation(this.m_strNamespace) ) {
            return;
        }

        try {
            if ( Util.isDebugMode() ) VocabularyImporter.logger.info("DEBUG: Load dataset graph from URL");
            if (this.bStrictlyRDF) {
                this.m_theDSGraph = RDFDataMgr.loadDatasetGraph(this.m_strLocation, Lang.RDFXML);
            }
            else {
                this.m_theDSGraph = RDFDataMgr.loadDatasetGraph(this.m_strLocation);
            }
        }
        catch (Exception ex) {
            this.m_theDSGraph = null;
            throw new VocabularyImportException("Importing vocabulary: " + this.m_strNamespace, ex);
        }
    }

    protected void getTerms(List<RDFTClass> classes, List<RDFTProperty> properties)
        throws VocabularyImportException
    {
        // Set RDFS Class and Property load conditions...
        String[] astrLoader = new String[6];
        astrLoader[RDFTNode.iPrefix] = this.m_strPrefix;
        astrLoader[RDFTNode.iNamespace] = this.m_strNamespace;
        astrLoader[RDFTNode.iLocalPart] = "";   // ...empty string, not null, since we purposely
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
        String strMessage = " vocabulary [" + this.m_strPrefix + "] classes: ";
        Set<String> seen = new HashSet<String>();

        Query query = null;
        try {
            String strRawQuery = Util.getVocabQueryPrefixes() + " " + Util.getVocabQueryClasses();
            String strQuery = String.format(strRawQuery, this.m_strNamespace);
            if ( Util.isDebugMode() ) VocabularyImporter.logger.info("DEBUG: Create class query:\n" + strQuery);
            query = QueryFactory.create(strQuery);
        }
        catch (Exception ex) {
            VocabularyImporter.logger.error( "ERROR: Query " + ex.getMessage() );
            throw new VocabularyImportException("Query " + strMessage + ex.getMessage(), ex);
        }
        try {
            QueryExecution qexec = QueryExecutionFactory.create(query, this.m_theDSGraph);
            //QueryExecution qexec = QueryExecutionFactory.create(query, this.modelImport);
            ResultSet results = qexec.execSelect();
            if ( results.hasNext() ) {
                while ( results.hasNext() ) {
                    QuerySolution solution = results.next();
                    try {
                        if ( this.processSolution(solution, astrLoader, seen) ) {
                            classes.add( new RDFTClass(astrLoader) );
                        }
                        if ( Util.isDebugMode() ) {
                            VocabularyImporter.logger.info(
                                "DEBUG: Solution IRI: " +
                                ( ( astrLoader[RDFTNode.iIRI] == null ) ? "NULL" : astrLoader[RDFTNode.iIRI] )
                            );
                        }
                    }
                    catch (Exception ex) {
                        if ( Util.isVerbose(2) ) {
                            VocabularyImporter.logger.warn(
                                "WARNING: Processing" + strMessage +
                                    "solution failed! [" + astrLoader[RDFTNode.iIRI] + "]"
                            );
                        }
                        // ...continue processing...
                    }
                }
            }
            else if ( Util.isDebugMode() ) VocabularyImporter.logger.info("DEBUG: NO Solution!");
        }
        catch (Exception ex) {
            VocabularyImporter.logger.error("ERROR: Class query and results: " + ex.getMessage(), ex);
            return;
        }
    }

    private void queryProperties(List<RDFTProperty> properties, String[] astrLoader)
            throws VocabularyImportException
    {
        String strMessage = " vocabulary [" + this.m_strPrefix + "] properties: ";
        Set<String> seen = new HashSet<String>();

        Query query = null;
        try {
            String strRawQuery = Util.getVocabQueryPrefixes() + " " + Util.getVocabQueryProperties();
            String strQuery = String.format(strRawQuery, this.m_strNamespace);
            if ( Util.isDebugMode() ) VocabularyImporter.logger.info("DEBUG: Create property query:\n" + strQuery);
            query = QueryFactory.create(strQuery);
        }
        catch (Exception ex) {
            VocabularyImporter.logger.error( "ERROR: Query " + ex.getMessage() );
            throw new VocabularyImportException("Query " + strMessage + ex.getMessage(), ex);
        }
        try {
            QueryExecution qexec = QueryExecutionFactory.create(query, this.m_theDSGraph);
            //QueryExecution qexec = QueryExecutionFactory.create(query, this.modelImport);
            ResultSet results = qexec.execSelect();
            if ( results.hasNext() ) {
                while ( results.hasNext() ) {
                    QuerySolution solution = results.nextSolution();
                    try {
                        if ( this.processSolution(solution, astrLoader, seen) ) {
                            properties.add( new RDFTProperty(astrLoader) );
                        }
                        if ( Util.isDebugMode() ) {
                            VocabularyImporter.logger.info(
                                "DEBUG: Solution IRI: " +
                                ( ( astrLoader[RDFTNode.iIRI] == null ) ? "NULL" : astrLoader[RDFTNode.iIRI] )
                            );
                        }
                    }
                    catch (Exception ex) {
                        if ( Util.isVerbose(2) ) {
                            VocabularyImporter.logger.warn(
                                "WARNING: Processing" + strMessage +
                                    "solution failed! [" + astrLoader[RDFTNode.iIRI] + "]"
                            );
                        }
                        // ...continue processing...
                    }
                }
            }
            else if ( Util.isDebugMode() ) VocabularyImporter.logger.info("DEBUG: NO Solution!");
        }
        catch (Exception ex) {
            VocabularyImporter.logger.error("ERROR: Property query and results: " + ex.getMessage(), ex);
            return;
        }
    }

    private boolean processSolution(QuerySolution solution, String[] astrLoader, Set<String> seen) {
        astrLoader[RDFTNode.iIRI] = null;
        RDFNode node = solution.get("resource");
        if (node == null) { // ...should never be null...
            return false;
        }
        astrLoader[RDFTNode.iIRI] = node.toString();
        if ( seen.contains(astrLoader[RDFTNode.iIRI]) ) {
            return false;
        }
        seen.add(astrLoader[RDFTNode.iIRI]);
        astrLoader[RDFTNode.iLabel] =
            this.getFirstNotNullLiteral(
                    new RDFNode[] {
                        solution.get("en_label"),
                        solution.get("label")
                    } );
        astrLoader[RDFTNode.iDesc] =
            this.getFirstNotNullLiteral(
                    new RDFNode[] {
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
            strNode = this.getString(nodes[i]);
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

    /**
     * Method faultyContentNegotiation(String strLocation)<br />
     * <br />
     * Processes the given namespace {@code String} to determine if the namespace
     * needs adjustments for retrieval or to stop processing altogether.<br />
     * <br />
     * The result is {@code true} if and only if further processing must stop. This
     * generally indicates that the namespace is not retrievable. The result is
     * {@code false} to indicate further processing is allowed with possible
     * modifications for the namespace via instance scoped {@code boolean} values.<br />
     *
     * @param  strLocation
     *         The {@code String} URL to process
     *
     * @return  {@code true} if the given URL cannot be retrieved,
     *          {@code false} otherwise.
     */
    private boolean faultyContentNegotiation(String strLocation) {
        //
        // Continue Processing: Set up booleans to process by later code outside this function
        //

        // SKOS: An exceptional treatment is provided for SKOS as their deployment does not handle an
        //       "Accept" header properly!  SKOS always returns "HTML" if the "Accept" header
        //       contains HTML regardless other more preferred options.
        this.bStrictlyRDF = strLocation.equals("http://www.w3.org/2004/02/skos/core#");
        if (this.bStrictlyRDF) {
            return false; // ...continue
        }

        //
        // Process Now: Set up code to process a return boolean to stop further processing
        //

        if ( strLocation.equals("http://www.w3.org/2001/XMLSchema#") ) {
            VocabularyImporter.logger.info("INFO: XMLSchema is a built-in Datatype ontology...skipping import.");
            return true; // ...stop
        }

        return false; // ...continue condition
    }
}
