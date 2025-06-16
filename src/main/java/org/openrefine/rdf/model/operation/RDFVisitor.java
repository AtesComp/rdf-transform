/*
 *  Class RDFVisitor
 *
 *  The RDF Visitor base class used by other RDF visitors.
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

package org.openrefine.rdf.model.operation;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import com.google.refine.model.Project;

import org.openrefine.rdf.RDFTransform;
import org.openrefine.rdf.model.Util;
import org.openrefine.rdf.model.vocab.Vocabulary;

import com.google.refine.browsing.Engine;

import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFWriterRegistry;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The RDFVisitor processes RDF triples/quads via its methods:
 * <ul>
 *   <li>buildDSGraph() -
 *      Builds a DatasetGraph for the Visitor to process RDF data.
 *      Output depends on whether a StreamRDF writer (theWriter) is given or not. When given, the
 *      process writes RDF data as it is processed via the start(), flushStatements(), and end()
 *      methods. When not given, after buildDSGraph() is called, the calling process writes RDF data
 *      via an RDFDataMgr write() method on either the DatasetGraph or its Graph.
 *   </li>
 *   <li>start() - Called by the FilteredRows or FilteredRecords accept() method in this.buildDSGraph(Project, Engine) </li>
 *   <li>flushStatements() - Called </li>
 *   <li>end() - Called by the FilteredRows or FilteredRecords accept() method in this.buildDSGraph(Project, Engine)</li>
 * </ul>
 */
public abstract class RDFVisitor {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:RDFVisitor");

    private final RDFTransform theTransform;
    private final StreamRDF theWriter;
    private Boolean bQuadWriter;
    protected final DatasetGraph theDSGraph;
    protected boolean bLimitWarning = true;

    /**
     *
     * @param theTransform - The RDF Transform to visit.
     * @param theWriter - The StreamRDF writer for output. It is null for pretty languages.
     * @param theFormat - The RDFFormat used by theWriter for output. It is null for pretty languages.
     */
    public RDFVisitor(RDFTransform theTransform, StreamRDF theWriter, RDFFormat theFormat) {
        this.theTransform = theTransform;
        this.theWriter = theWriter;
        this.bQuadWriter = null;

        // Initializing dataset graph...
        this.theDSGraph = DatasetGraphFactory.create();

        //
        // Populate the namespaces in the repository...
        //

        // Prepare Namespaces...
        String strBaseIRI = this.theTransform.getBaseIRIAsString();
        Collection<Vocabulary> theNamespaces = this.theTransform.getNamespaces();

        // Check for the BaseIRI (default namespace) in the Prefixed Namespaces...
        boolean bUseBaseIRI = true; // ...default: use the BaseIRI
        for (Vocabulary vocab : theNamespaces) {
            // If the BaseIRI is in the Prefixed Namespace...
            if ( vocab.getNamespace().equals(strBaseIRI) ) {
                bUseBaseIRI = false; // ...don't use the BaseIRI!
                break;
            }
        }

        PrefixMap thePrefixes = this.theDSGraph.prefixes();
        thePrefixes.clear();

        // Set Default Namespace for repository...
        if ( bUseBaseIRI && ! strBaseIRI.isEmpty() ) {
            if ( Util.isDebugMode() ) RDFVisitor.logger.info("DEBUG: Using BaseIRI " + strBaseIRI);
            thePrefixes.add("", strBaseIRI);
        }
        else {
            if ( Util.isDebugMode() ) RDFVisitor.logger.info("DEBUG: Not using BaseIRI");
        }

        // Set Prefix Namespaces for repository...
        for (Vocabulary vocab : theNamespaces) {
            thePrefixes.add( vocab.getPrefix(), vocab.getNamespace() );
        }

        if (theWriter != null && theFormat != null) {
            if      ( RDFWriterRegistry.getWriterDatasetFactory(theFormat) != null) {
                this.bQuadWriter = true;
            }
            else if ( RDFWriterRegistry.getWriterGraphFactory(theFormat) != null) {
                this.bQuadWriter = false;
            }
            else {
                IOException exIO = new IOException("Dataset does not have a Dataset or Graph writer for " + theFormat.getLang().getName() + "!");
                RDFVisitor.logger.error("ERROR: Determining Quad/Triple Type: " + exIO.getMessage(), exIO);
                if ( Util.isVerbose() ) exIO.printStackTrace();
                throw new RuntimeException(exIO.getMessage(), exIO);
            }
        }
        else if (theWriter == null && theFormat == null) {
            this.bQuadWriter = null; // ...unused
        }
        else {
            IOException exIO = new IOException("The writer and format MUST BOTH be set or null!");
            RDFVisitor.logger.error("ERROR: Determining Quad/Triple Type: " + exIO.getMessage(), exIO);
            if ( Util.isVerbose() ) exIO.printStackTrace();
            throw new RuntimeException(exIO.getMessage(), exIO);
        }

    }

    public RDFTransform getRDFTransform() {
        return this.theTransform;
    }

    public DatasetGraph getDSGraph() {
        return this.theDSGraph;
    }

    public boolean isNoWriter() {
        return (this.theWriter == null);
    }

    abstract public void buildDSGraph(Project theProject, Engine theEngine);

    /**
     * Performs any necessary processing before visiting the selected (filtered) data rows or records.
     * Called by the FilteredRows or FilteredRecords accept() method in this.buildDSGraph(Project, Engine)
     * @param theProject
     */
    public void start(Project theProject) {
        if ( Util.isVerbose(3) ) RDFVisitor.logger.info("Starting Visitation...");

        // If we do NOT have a writer, let the calling processor control all model activity...
        if ( this.theWriter == null ) {
            return;
        }

        // Export namespace information previously populated in the model...
        try {
            Map<String, String> nsMap = this.theDSGraph.prefixes().getMapping();
            for ( Map.Entry<String, String> ns : nsMap.entrySet() ) {
                String strPrefix = ns.getKey();
                String strNamespace = ns.getValue();
                this.theWriter.prefix(strPrefix, strNamespace);
                if ( Util.isDebugMode() ) RDFVisitor.logger.info("DEBUG: Prefix: " + strPrefix + "  " + strNamespace);
            }
        }
        catch (Exception ex) {
            RDFVisitor.logger.error("ERROR: Exporting Prefixes: " + ex.getMessage(), ex);
            if ( Util.isVerbose() ) ex.printStackTrace();
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    /**
     * Performs any necessary processing after visiting the selected (filtered) data rows or records.
     * Called by the FilteredRows or FilteredRecords accept() method in this.buildDSGraph(Project, Engine)
     * @param theProject
     */
    public void end(Project theProject) {
        if ( Util.isVerbose(3) ) RDFVisitor.logger.info("...Ending Visitation");

        // If we do NOT have a writer, let the calling processor control all DatasetGraph activity...
        if ( this.theWriter == null ) {
            return;
        }

        // Close the dataset graph automatically...
        try {
            this.theDSGraph.close();
        }
        catch (Exception ex) {
            RDFVisitor.logger.error("ERROR: Closing DatasetGraph: " + ex.getMessage(), ex);
            if ( Util.isVerbose() ) ex.printStackTrace();
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    protected void flushStatements() {
        if ( this.theWriter == null ) {
            return;
        }

        //
        // Export statements...
        //

        // If theWriter is a Quad writer...
        if (this.bQuadWriter) {
            Iterator<Quad> stmtIter = this.theDSGraph.find();
            try {
                while ( stmtIter.hasNext() ) {
                    this.theWriter.quad( stmtIter.next() );
                }
            }
            finally {}
        }
        // Otherwise, theWriter is a Triple writer...
        else {
            ExtendedIterator<Triple> stmtIter = this.theDSGraph.getUnionGraph().find();
            try {
                while ( stmtIter.hasNext() ) {
                    this.theWriter.triple( stmtIter.next() );
                }
            }
            finally {
                stmtIter.close();
            }
        }

    }

    public void closeDSGraph() {
        this.theDSGraph.close();
    }
}
