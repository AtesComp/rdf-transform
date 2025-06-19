/*
 *  Class PreviewRDFRecordVisitor
 *
 *  The RDF Transform's Preview RDF as Record Visitor.
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
import com.google.refine.model.Record;

import org.apache.jena.iri.IRI;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.system.StreamRDF;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreviewRDFRecordVisitor extends RDFRecordVisitor {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:PrevRDFRecV");

    private int iLimit = Util.getSampleLimit();
    private int iCount = 0;

    public PreviewRDFRecordVisitor(RDFTransform theTransform, StreamRDF theWriter, RDFFormat theFormat) {
        super(theTransform, theWriter, theFormat);
        this.iLimit = Util.getSampleLimit();
        if ( Util.isDebugMode() ) PreviewRDFRecordVisitor.logger.info("DEBUG: Created...");
    }

    public boolean visit(Project theProject, Record theRecord) {
        // Test for end of sample...
        if ( this.iLimit > 0 && this.iCount >= this.iLimit ) {
            return true; // ...stop visitation process
        }
        try {
            if ( Util.isDebugMode() ) PreviewRDFRecordVisitor.logger.info("DEBUG: Visiting Record: " + theRecord.recordIndex + " on count: " +  this.iCount);
            IRI baseIRI = this.getRDFTransform().getBaseIRI();
            List<ResourceNode> listRoots = this.getRDFTransform().getRoots();
            for ( ResourceNode root : listRoots ) {
                root.createStatements(baseIRI, this.theDSGraph, theProject, theRecord);

                if ( Util.isDebugMode() ) {
                    PreviewRDFRecordVisitor.logger.info("DEBUG:   " +
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
                        PreviewRDFRecordVisitor.logger.warn("WARNING:   Limit Reached: Memory may soon become exhausted!");
                    }
                }
            }
            this.iCount += 1;

            // Flush any remaining statements...
            this.flushStatements();
        }
        catch (Exception ex) {
            PreviewRDFRecordVisitor.logger.error("ERROR: Visit Issue: " + ex.getMessage(), ex);
            if ( Util.isVerbose() ) ex.printStackTrace();
            return true; // ...stop visitation process
        }

        return false;
    }
}
