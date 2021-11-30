package com.google.refine.rdf.operation;

import com.google.refine.browsing.RowVisitor;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.rdf.RDFTransform;

import org.eclipse.rdf4j.rio.RDFWriter;

public abstract class RDFRowVisitor extends RDFVisitor implements RowVisitor {

    public RDFRowVisitor(RDFTransform transform, RDFWriter writer) {
        super(transform, writer);
    }

    abstract public boolean visit(Project project, int iRowIndex, Row row);
}
