package com.google.refine.rdf.model.operation;

import com.google.refine.rdf.RDFTransform;
import com.google.refine.rdf.model.Util;
import com.google.refine.browsing.Engine;
import com.google.refine.browsing.FilteredRecords;
import com.google.refine.browsing.RecordVisitor;
import com.google.refine.model.Project;
import com.google.refine.model.Record;

import org.eclipse.rdf4j.rio.RDFWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RDFRecordVisitor extends RDFVisitor implements RecordVisitor {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:RDFRecordVisitor" );

    public RDFRecordVisitor(RDFTransform theTransform, RDFWriter theWriter) {
        super(theTransform, theWriter);
    }

    abstract public boolean visit(Project theProject, Record theRecord);

    public void buildModel(Project theProject, Engine theEngine) {
        FilteredRecords filteredRecords = theEngine.getFilteredRecords();
		if ( Util.isVerbose(3) ) RDFRecordVisitor.logger.info("buildModel: visit matching filtered records");
        filteredRecords.accept(theProject, this);
    }
}
