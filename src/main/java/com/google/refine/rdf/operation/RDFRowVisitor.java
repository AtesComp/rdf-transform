package com.google.refine.rdf.operation;

import com.google.refine.rdf.RDFTransform;
import com.google.refine.rdf.Util;

import com.google.refine.browsing.Engine;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

import org.eclipse.rdf4j.rio.RDFWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RDFRowVisitor extends RDFVisitor implements RowVisitor {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:RDFRowVisitor" );

    public RDFRowVisitor(RDFTransform theTransform, RDFWriter theWriter) {
        super(theTransform, theWriter);
    }

    abstract public boolean visit(Project theProject, int iRowIndex, Row theRow);

    public void buildModel(Project theProject, Engine theEngine) {
        FilteredRows filteredRows = theEngine.getAllFilteredRows();
		if ( Util.isVerbose(3) ) logger.info("buildModel: visit matching filtered rows");
        filteredRows.accept(theProject, this);
    }
}
