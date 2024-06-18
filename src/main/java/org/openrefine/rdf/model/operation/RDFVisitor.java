/*
 *  Class RDFVisitor
 *
 *  The RDF Visitor base class used by other RDF visitors.
 *
 *  Copyright 2024 Keven L. Ates
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

import java.util.Collection;
import java.util.Map;

import com.google.refine.model.Project;

import org.openrefine.rdf.RDFTransform;
import org.openrefine.rdf.model.Util;
import org.openrefine.rdf.model.vocab.Vocabulary;
import com.google.refine.browsing.Engine;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.util.iterator.ExtendedIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RDFVisitor {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:RDFVisitor");

    private final RDFTransform theTransform;
    private final StreamRDF theWriter;
    protected final Model theModel;
    protected boolean bLimitWarning = true;

    public RDFVisitor(RDFTransform theTransform, StreamRDF theWriter) {
        this.theTransform = theTransform;
        this.theWriter = theWriter;

        // Initializing model...
        this.theModel = ModelFactory.createDefaultModel();

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

        //this.theModel.begin();
        this.theModel.clearNsPrefixMap();
        this.theModel.enterCriticalSection(Model.WRITE);

        // Set Default Namespace for repository...
        if ( bUseBaseIRI && ! strBaseIRI.isEmpty() ) {
            if ( Util.isDebugMode() ) RDFVisitor.logger.info("DEBUG: Using BaseIRI " + strBaseIRI);
            this.theModel.setNsPrefix("", strBaseIRI); // ...default namespace
        }
        else {
            if ( Util.isDebugMode() ) RDFVisitor.logger.info("DEBUG: Not using BaseIRI");
        }

        // Set Prefix Namespaces for repository...
        for (Vocabulary vocab : theNamespaces) {
            this.theModel.setNsPrefix( vocab.getPrefix(), vocab.getNamespace() );
        }

        this.theModel.leaveCriticalSection();
        //this.theModel.commit();
        this.theModel.lock();
    }

    public RDFTransform getRDFTransform() {
        return this.theTransform;
    }

    public Model getModel() {
        return this.theModel;
    }

    public boolean isNoWriter() {
        return (this.theWriter == null);
    }

    abstract public void buildModel(Project theProject, Engine theEngine);

    /**
     * Performs any necessary processing before visiting the selected (filtered) data rows or records.
     * Called by the FilteredRows or FilteredRecords accept() method in this.buildModel(Project, Engine)
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
            Map<String, String> nsMap = this.theModel.getNsPrefixMap();
            for ( Map.Entry<String, String> ns : nsMap.entrySet() ) {
                String strPrefix = ns.getKey();
                String strNamespace = ns.getValue();
                this.theWriter.prefix(strPrefix, strNamespace);
                if ( Util.isDebugMode() ) RDFVisitor.logger.info("DEBUG: Prefix: " + strPrefix + "  " + strNamespace);
            }
        }
        catch (Exception ex) {
            RDFVisitor.logger.error("ERROR: Exporting Prefixes: " + ex.getMessage(), ex);
            if ( Util.isVerbose() || Util.isDebugMode() ) ex.printStackTrace();
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    /**
     * Performs any necessary processing after visiting the selected (filtered) data rows or records.
     * Called by the FilteredRows or FilteredRecords accept() method in this.buildModel(Project, Engine)
     * @param theProject
     */
    public void end(Project theProject) {
        if ( Util.isVerbose(3) ) RDFVisitor.logger.info("...Ending Visitation");

        // If we do NOT have a writer, let the calling processor control all model activity...
        if ( this.theWriter == null ) {
            return;
        }

        // Close the model automatically...
        try {
            this.theModel.close();
        }
        catch (Exception ex) {
            RDFVisitor.logger.error("ERROR: Closing Model: " + ex.getMessage(), ex);
            if ( Util.isVerbose() || Util.isDebugMode() ) ex.printStackTrace();
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    protected void flushStatements() {
        if ( this.theWriter == null ) {
            return;
        }
        // TODO: Code for future context upgrade (quads)

        // Export statements...
        this.theModel.enterCriticalSection(Model.READ);
        ExtendedIterator<Triple> stmtIter = this.theModel.getGraph().find();
        try {
            while ( stmtIter.hasNext() ) {
                this.theWriter.triple( stmtIter.next() );
            }
        }
        finally {
            stmtIter.close();
        }
        this.theModel.leaveCriticalSection();

        // Remove the exported statements from the model...
        this.theModel.enterCriticalSection(Model.WRITE);
        this.theModel.removeAll();
        this.theModel.leaveCriticalSection();
    }
}
