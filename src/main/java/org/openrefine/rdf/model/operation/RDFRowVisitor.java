/*
 *  Class RDFRowVisitor
 *
 *  The RDF Row Visitor base class used by other RDF row visitors.
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
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RDFRowVisitor extends RDFVisitor implements RowVisitor {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:RDFRowVisitor" );

    public RDFRowVisitor(RDFTransform theTransform) {
        super(theTransform);
    }

    abstract public boolean visit(Project theProject, int iRowIndex, Row theRow);
    abstract public boolean visit(Project theProject, int iRowIndex, int iSortedRowIndex, Row theRow);

    public void buildDSGraph(Project theProject, Engine theEngine) {
        FilteredRows filteredRows = theEngine.getAllFilteredRows();
        if ( Util.isVerbose(3) ) RDFRowVisitor.logger.info("buildDSGraph: visit matching filtered rows");
        // NOTE: The filteredRows.accept() method calls this visitor's start() and end() methods.
        //      This visitor's end() method closes the DatasetGraph.
        filteredRows.accept(theProject, this);
    }
}
