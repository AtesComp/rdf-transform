package org.openrefine.rdf.model.operation;

import java.util.List;

import org.openrefine.rdf.RDFTransform;
import org.openrefine.rdf.model.ResourceNode;
import org.openrefine.rdf.model.Util;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportRDFRowVisitor extends RDFRowVisitor {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:ExportRDFRowV");

    public ExportRDFRowVisitor(RDFTransform theTransform, RDFWriter theWriter) {
        super(theTransform, theWriter);
    }

    public boolean visit(Project theProject, int iRowIndex, Row theRow) {
        try {
            if ( Util.isDebugMode() ) ExportRDFRowVisitor.logger.info("DEBUG: Visiting Row: " + iRowIndex);
            ParsedIRI baseIRI = this.getRDFTransform().getBaseIRI();
            RepositoryConnection theConnection = this.getModel().getConnection();
            ValueFactory theFactory = theConnection.getValueFactory();
            List<ResourceNode> listRoots = this.getRDFTransform().getRoots();
            for ( ResourceNode root : listRoots ) {
                root.createStatements(baseIRI, theFactory, theConnection, theProject, iRowIndex);

                if ( Util.isDebugMode() ) {
                    ExportRDFRowVisitor.logger.info("DEBUG:   " +
                        "Root: " + root.getNodeName() + "(" + root.getNodeType() + ")  " +
                        "Size: " + theConnection.size()
                    );
                }
                //
                // Flush Statements
                //
                // Write and clear a discrete set of statements from the repository connection
                // as the transformed statements use in-memory resources until flushed to disk.
                // Otherwise, large files would use excessive memory!
                //
                if ( theConnection.size() > Util.getExportLimit() ) {
                    this.flushStatements();
                }
            }

            // Flush any remaining statements...
            this.flushStatements();
        }
        catch (RepositoryException ex) {
            ExportRDFRowVisitor.logger.error("Connection Issue: ", ex);
            if ( Util.isVerbose() || Util.isDebugMode() ) ex.printStackTrace();
            return true; // ...stop visitation process
        }
        catch (RDFHandlerException ex) {
            ExportRDFRowVisitor.logger.error("Flush Issue: ", ex);
            if ( Util.isVerbose() || Util.isDebugMode() ) ex.printStackTrace();
            return true; // ...stop visitation process
        }

        return false;
    }
}