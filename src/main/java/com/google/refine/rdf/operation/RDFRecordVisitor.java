package com.google.refine.rdf.operation;

import com.google.refine.browsing.RecordVisitor;
import com.google.refine.model.Project;
import com.google.refine.model.Record;
import com.google.refine.rdf.RDFTransform;

import org.eclipse.rdf4j.rio.RDFWriter;

public abstract class RDFRecordVisitor extends RDFVisitor implements RecordVisitor {

    public RDFRecordVisitor(RDFTransform transform, RDFWriter writer) {
        super(transform, writer);
    }

    abstract public boolean visit(Project project, Record record);
}
