package org.openrefine.rdf.model.operation;

import org.openrefine.rdf.RDFTransform;
import org.openrefine.rdf.model.Util;
import com.google.refine.browsing.Engine;
import com.google.refine.browsing.FilteredRecords;
import com.google.refine.browsing.RecordVisitor;
import com.google.refine.model.Project;
import com.google.refine.model.Record;

import org.apache.jena.riot.system.StreamRDF;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RDFRecordVisitor extends RDFVisitor implements RecordVisitor {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:RDFRecordVisitor" );

    public RDFRecordVisitor(RDFTransform theTransform, StreamRDF theWriter) {
        super(theTransform, theWriter);
    }

    abstract public boolean visit(Project theProject, Record theRecord);

    public void buildModel(Project theProject, Engine theEngine) {
        FilteredRecords filteredRecords = theEngine.getFilteredRecords();
        if ( Util.isVerbose(3) ) RDFRecordVisitor.logger.info("buildModel: visit matching filtered records");
        filteredRecords.accept(theProject, this);
    }
}
