/*
 *  Class ExportRDFRecordVisitor
 *
 *  The RDF Transform's Export RDF as Record Visitor.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportRDFRecordVisitor extends RDFRecordVisitor {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:ExportRDFRecV");

    public ExportRDFRecordVisitor(RDFTransform theTransform) {
        super(theTransform);
        if ( Util.isDebugMode() ) ExportRDFRecordVisitor.logger.info("DEBUG: Created...");
    }

    public boolean visit(Project theProject, Record theRecord) {
        try {
            if ( Util.isDebugMode() ) ExportRDFRecordVisitor.logger.info("DEBUG: Visiting Record: " + theRecord.recordIndex);
            IRI baseIRI = this.getRDFTransform().getBaseIRI();
            List<ResourceNode> listRoots = this.getRDFTransform().getRoots();
            for ( ResourceNode root : listRoots ) {
                root.createStatements(baseIRI, this.theDSGraph, theProject, theRecord);

                if ( Util.isDebugMode() ) {
                    ExportRDFRecordVisitor.logger.info("DEBUG:   Root\n" +
                        "  Name: " + root.getNodeName() + "\n" +
                        "  Type: " + root.getNodeType() + "\n" +
                        "  Graph Count: " + this.theDSGraph.size() + "\n" +
                        "  Stmt  Count: " + this.theDSGraph.getUnionGraph().size()
                    );
                }
                // WARNING: this.theDSGraph.getUnionGraph().size() > Util.getExportLimit()
            }
        }
        catch (Exception ex) {
            ExportRDFRecordVisitor.logger.error("ERROR: Visit Issue: " + ex.getMessage(), ex);
            if ( Util.isVerbose() ) ex.printStackTrace();
            return true; // ...stop visitation process
        }

        return false;
    }

    public boolean visit(Project theProject, int iSortedStartRowIndex, Record theRecord) {
        return this.visit(theProject, theRecord);
    }
}
