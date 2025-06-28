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

import java.util.Collection;
import java.util.Iterator;

import com.google.refine.model.Project;

import org.openrefine.rdf.RDFTransform;
import org.openrefine.rdf.model.Util;
import org.openrefine.rdf.model.vocab.Vocabulary;

import com.google.refine.browsing.Engine;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.sparql.util.NodeUtils;
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
 *   <li>flushStatements() - Called by the visit() method in </li>
 *   <li>end() - Called by the FilteredRows or FilteredRecords accept() method in this.buildDSGraph(Project, Engine)</li>
 * </ul>
 */
public abstract class RDFVisitor {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:RDFVisitor");

    private final RDFTransform theTransform;
    protected final DatasetGraph theDSGraph;
    protected boolean bLimitWarning = true;

    /**
     * RDFVisitor ctor
     * @param theTransform - The RDF Transform to visit.
     */
    public RDFVisitor(RDFTransform theTransform) {
        this.theTransform = theTransform;

        // Initializing dataset graph...
        this.theDSGraph = DatasetGraphFactory.create(); // NOTE: Maybe createTxnMem() is better?
        String strBaseIRI = this.theTransform.getBaseIRIAsString();
        org.apache.jena.graph.Node nodeBaseGraph = NodeUtils.asNode( Util.getGraphIRIString(strBaseIRI) );
        {
            Graph graphBase = GraphFactory.createGraphMem(); // NOTE: Maybe createTxnGraph() is better?
            this.theDSGraph.addGraph(nodeBaseGraph, graphBase);
        }

        //
        // Populate the namespaces in the repository (DatasetGraph and Base Graph)...
        //

        // Prepare Namespaces...
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

        PrefixMap theDSGPrefixes = this.theDSGraph.prefixes();
        PrefixMapping theBGPrefixes = this.theDSGraph.getGraph(nodeBaseGraph).getPrefixMapping();
        theDSGPrefixes.clear();
        theBGPrefixes.clearNsPrefixMap();

        // Set Default Namespace for repository...
        if ( bUseBaseIRI && ! strBaseIRI.isEmpty() ) {
            if ( Util.isDebugMode() ) RDFVisitor.logger.info("DEBUG: Using BaseIRI " + strBaseIRI);
            theDSGPrefixes.add("", strBaseIRI);
            theBGPrefixes.setNsPrefix("", strBaseIRI);
        }
        else {
            if ( Util.isDebugMode() ) RDFVisitor.logger.info("DEBUG: Not using BaseIRI");
        }

        // Set Prefix Namespaces for repository...
        for (Vocabulary vocab : theNamespaces) {
            String strPrefix = vocab.getPrefix();
            String strNamespace = vocab.getNamespace();
            theDSGPrefixes.add(strPrefix, strNamespace);
            theBGPrefixes.setNsPrefix(strPrefix, strNamespace);
        }
    }

    public RDFTransform getRDFTransform() {
        return this.theTransform;
    }

    public DatasetGraph getDSGraph() {
        return this.theDSGraph;
    }

    // NOTE: Oddly enough, there is no abstract "visit()" method here as the visitor parameters depend on
    //      the derived class: "Row" or "Record" visitor.  See the RDFRowVisitor and RDFRecordVisitor classes.
    //abstract public boolean visit(Project theProject, ...);

    abstract public void buildDSGraph(Project theProject, Engine theEngine);

    /**
     * Performs any necessary processing before visiting the selected (filtered) data rows or records.
     * Called by the FilteredRows or FilteredRecords accept() method in this.buildDSGraph(Project, Engine)
     * @param theProject
     */
    public void start(Project theProject) {
        if ( Util.isVerbose(3) ) RDFVisitor.logger.info("Starting Visitation...");
    }

    /**
     * Performs any necessary processing after visiting the selected (filtered) data rows or records.
     * Called by the FilteredRows or FilteredRecords accept() method in this.buildDSGraph(Project, Engine)
     * @param theProject
     */
    public void end(Project theProject) {
        if ( Util.isVerbose(3) ) RDFVisitor.logger.info("...Ending Visitation");
    }

    private void clearGraphStatements() {
        Iterator<Node> iterGraphNodes = this.theDSGraph.listGraphNodes();
        while ( iterGraphNodes.hasNext() ) {
            Node nodeGraph = iterGraphNodes.next();
            if ( nodeGraph.isNodeGraph() && ! nodeGraph.getGraph().isEmpty() )
                this.theDSGraph.deleteAny(nodeGraph, Node.ANY, Node.ANY, Node.ANY);
        }
    }

    public void closeDSGraph() {
        this.clearGraphStatements();
        this.theDSGraph.close();
    }
}
