package com.google.refine.rdf.operation;

import com.google.refine.rdf.RDFTransform;
import com.google.refine.rdf.ResourceNode;

import com.google.refine.model.Project;
import com.google.refine.model.Row;

import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFWriter;

public class ExportRDFRowVisitor extends RDFRowVisitor {

    public ExportRDFRowVisitor(RDFTransform transformer, RDFWriter writer) {
        super(transformer, writer);
    }

    public boolean visit(Project theProject, int iRowIndex, Row row) {
        ParsedIRI baseIRI = this.getRDFTransform().getBaseIRI();
        RepositoryConnection connection = this.getModel().getConnection();
        ValueFactory factory = connection.getValueFactory();
        for (ResourceNode root : this.getRDFTransform().getRoots()) {
            root.createStatements(baseIRI, factory, connection, theProject,
                                    row, iRowIndex);
            try {
                // Flush here to preserve root ordering in the output file...
                flushStatements();
            }
            catch (RepositoryException ex) {
                throw new RuntimeException(ex);
            }
            catch (RDFHandlerException ex) {
                throw new RuntimeException(ex);
            }
        }

        return false;
    }
}
