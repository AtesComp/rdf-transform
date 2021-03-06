package org.openrefine.rdf.model.operation;

import org.openrefine.rdf.RDFTransform;
import org.openrefine.rdf.model.Util;
import com.google.refine.browsing.Engine;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

import org.apache.jena.riot.system.StreamRDF;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RDFRowVisitor extends RDFVisitor implements RowVisitor {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:RDFRowVisitor" );

    public RDFRowVisitor(RDFTransform theTransform, StreamRDF theWriter) {
        super(theTransform, theWriter);
    }

    abstract public boolean visit(Project theProject, int iRowIndex, Row theRow);

    public void buildModel(Project theProject, Engine theEngine) {
        FilteredRows filteredRows = theEngine.getAllFilteredRows();
        if ( Util.isVerbose(3) ) RDFRowVisitor.logger.info("buildModel: visit matching filtered rows");
        filteredRows.accept(theProject, this);
    }
}
