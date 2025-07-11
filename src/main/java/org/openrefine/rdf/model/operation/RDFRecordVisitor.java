/*
 *  Class RDFRecordVisitor
 *
 *  The RDF Record Visitor base class used by other RDF record visitors.
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

import org.openrefine.rdf.RDFTransform;
import org.openrefine.rdf.model.Util;
import com.google.refine.browsing.Engine;
import com.google.refine.browsing.FilteredRecords;
import com.google.refine.browsing.RecordVisitor;
import com.google.refine.model.Project;
import com.google.refine.model.Record;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RDFRecordVisitor extends RDFVisitor implements RecordVisitor {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:RDFRecordVisitor" );

    public RDFRecordVisitor(RDFTransform theTransform) {
        super(theTransform);
    }

    abstract public boolean visit(Project theProject, Record theRecord);
    abstract public boolean visit(Project theProject, int iSortedStartRowIndex, Record theRecord);

    public void buildDSGraph(Project theProject, Engine theEngine) {
        FilteredRecords filteredRecords = theEngine.getFilteredRecords();
        if ( Util.isVerbose(3) ) RDFRecordVisitor.logger.info("buildDSGraph: visit matching filtered records");
        // NOTE: The filteredRecords.accept() method calls this visitor's start() and end() methods.
        //      This visitor's end() method closes the DatasetGraph.
        filteredRecords.accept(theProject, this);
    }
}
