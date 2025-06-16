/*
 *  Class ExportRDFRowVisitor
 *
 *  The RDF Transform's Export RDF as Row Visitor.
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
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.system.StreamRDF;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportRDFRowVisitor extends RDFRowVisitor {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:ExportRDFRowV");

    public ExportRDFRowVisitor(RDFTransform theTransform, StreamRDF theWriter, RDFFormat theFormat) {
        super(theTransform, theWriter, theFormat);
    }

    public boolean visit(Project theProject, int iRowIndex, Row theRow) {
        try {
            if ( Util.isDebugMode() ) ExportRDFRowVisitor.logger.info("DEBUG: Visiting Row: " + iRowIndex);
            IRI baseIRI = this.getRDFTransform().getBaseIRI();
            List<ResourceNode> listRoots = this.getRDFTransform().getRoots();
            for ( ResourceNode root : listRoots ) {
                root.createStatements(baseIRI, this.theDSGraph, theProject, iRowIndex);

                if ( Util.isDebugMode() ) {
                    ExportRDFRowVisitor.logger.info("DEBUG:   " +
                        "Root: " + root.getNodeName() + "(" + root.getNodeType() + ")  " +
                        "DatasetGraph Size: " + this.theDSGraph.size()
                    );
                }
                //
                // Flush Statements
                //
                // Write and clear a discrete set of statements from the repository connection
                // as the transformed statements use in-memory resources until flushed to disk.
                // Otherwise, large files would use excessive memory!
                //
                if ( this.theDSGraph.size() > Util.getExportLimit() ) {
                    this.flushStatements();
                    if ( this.isNoWriter() && bLimitWarning) {
                        this.bLimitWarning = false;
                        ExportRDFRowVisitor.logger.warn("WARNING:   Limit Reached: Memory may soon become exhausted!");
                    }
                }
            }

            // Flush any remaining statements...
            this.flushStatements();
        }
        catch (Exception ex) {
            ExportRDFRowVisitor.logger.error("ERROR: Visit Issue: " + ex.getMessage(), ex);
            if ( Util.isVerbose() ) ex.printStackTrace();
            return true; // ...stop visitation process
        }

        return false;
    }
}
