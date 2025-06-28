/*
 *  Class PreviewRDFRowVisitor
 *
 *  The RDF Transform's Preview RDF as Row Visitor.
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

import java.util.List;

import org.openrefine.rdf.RDFTransform;
import org.openrefine.rdf.model.ResourceNode;
import org.openrefine.rdf.model.Util;

import com.google.refine.model.Project;
import com.google.refine.model.Row;

import org.apache.jena.iri.IRI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreviewRDFRowVisitor extends RDFRowVisitor {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:PreviewRDFRowV");

    private int iLimit = Util.getSampleLimit();
    private int iCount = 0;

    public PreviewRDFRowVisitor(RDFTransform theTransform) {
        super(theTransform);
        this.iLimit = Util.getSampleLimit();
        if ( Util.isDebugMode() ) PreviewRDFRowVisitor.logger.info("DEBUG: Created...");
    }

    public boolean visit(Project theProject, int iRowIndex, Row theRow) {
        // Test for end of sample...
        if ( this.iLimit > 0 && this.iCount >= this.iLimit ) {
            return true; // ...stop visitation process
        }
        try {
            if ( Util.isDebugMode() ) PreviewRDFRowVisitor.logger.info("DEBUG: Visiting Row: " + iRowIndex + " on count: " +  this.iCount);
            IRI baseIRI = this.getRDFTransform().getBaseIRI();
            List<ResourceNode> listRoots = this.getRDFTransform().getRoots();
            for ( ResourceNode root : listRoots ) {
                root.createStatements(baseIRI, this.theDSGraph, theProject, iRowIndex );

                if ( Util.isDebugMode() ) {
                    PreviewRDFRowVisitor.logger.info("DEBUG:   Root\n" +
                        "  Name: " + root.getNodeName() + "\n" +
                        "  Type: " + root.getNodeType() + "\n" +
                        "  Graph Count: " + this.theDSGraph.size() + "\n" +
                        "  Stmt  Count: " + this.theDSGraph.getUnionGraph().size()
                    );
                }
                // WARNING: this.theDSGraph.getUnionGraph().size() > Util.getExportLimit()
            }
            this.iCount += 1;
        }
        catch (Exception ex) {
            PreviewRDFRowVisitor.logger.error("ERROR: Visit Issue: " + ex.getMessage(), ex);
            if ( Util.isVerbose() ) ex.printStackTrace();
            return true; // ...stop visitation process
        }

        return false;
    }

    public boolean visit(Project theProject, int iRowIndex, int iSortedRowIndex, Row theRow) {
        return this.visit(theProject, iRowIndex, theRow);
    }
}
