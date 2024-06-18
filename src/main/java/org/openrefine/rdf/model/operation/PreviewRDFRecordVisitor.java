/*
 *  Class PreviewRDFRecordVisitor
 *
 *  The RDF Transform's Preview RDF as Record Visitor.
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

import java.util.List;

import org.openrefine.rdf.RDFTransform;
import org.openrefine.rdf.model.ResourceNode;
import org.openrefine.rdf.model.Util;

import com.google.refine.model.Project;
import com.google.refine.model.Record;

import org.apache.jena.iri.IRI;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.system.StreamRDF;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreviewRDFRecordVisitor extends RDFRecordVisitor {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:PrevRDFRecV");

    private int iLimit = Util.getSampleLimit();
    private int iCount = 0;

    public PreviewRDFRecordVisitor(RDFTransform theTransform, StreamRDF theWriter) {
        super(theTransform, theWriter);
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
                this.theModel.enterCriticalSection(Model.WRITE);
                root.createStatements(baseIRI, this.theModel, theProject, theRecord);
                this.theModel.leaveCriticalSection();

                if ( Util.isDebugMode() ) {
                    PreviewRDFRecordVisitor.logger.info("DEBUG:   " +
                        "Root: " + root.getNodeName() + "(" + root.getNodeType() + ")  " +
                        "Model Size: " + this.theModel.size()
                    );
                }
            }
            this.iCount += 1;

            // Flush all statements...
            this.flushStatements();
        }
        catch (Exception ex) {
            PreviewRDFRecordVisitor.logger.error("ERROR: Visit Issue: " + ex.getMessage(), ex);
            if ( Util.isVerbose() || Util.isDebugMode() ) ex.printStackTrace();
            return true; // ...stop visitation process
        }

        return false;
    }
}
