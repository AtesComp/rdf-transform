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

public class PreviewRDFRowVisitor extends RDFRowVisitor {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:PrevRDFRowV");

    private int iLimit = Util.getSampleLimit();
    private int iCount = 0;

    public PreviewRDFRowVisitor(RDFTransform theTransform, RDFWriter theWriter) {
        super(theTransform, theWriter);
        this.iLimit = Util.getSampleLimit();
    }

    public boolean visit(Project theProject, int iRowIndex, Row theRow) {
        // Test for end of sample...
        if ( this.iLimit > 0 && this.iCount >= this.iLimit ) {
            return true; // ...stop visitation process
        }
        try {
            if ( Util.isDebugMode() ) PreviewRDFRowVisitor.logger.info("DEBUG: Visiting Row: " + iRowIndex + " on count: " +  this.iCount);
            ParsedIRI baseIRI = this.getRDFTransform().getBaseIRI();
            RepositoryConnection theConnection = this.getModel().getConnection();
            ValueFactory theFactory = theConnection.getValueFactory();
            List<ResourceNode> listRoots = this.getRDFTransform().getRoots();
            for ( ResourceNode root : listRoots ) {
                root.createStatements(baseIRI, theFactory, theConnection, theProject, iRowIndex );

                if ( Util.isDebugMode() ) {
                    PreviewRDFRowVisitor.logger.info("DEBUG:   " +
                        "Root: " + root.getNodeName() + "(" + root.getNodeType() + ")  " +
                        "Size: " + theConnection.size()
                    );
                }
            }
            this.iCount += 1;

            this.flushStatements();
        }
        catch (RepositoryException ex) {
            PreviewRDFRowVisitor.logger.error("Connection Issue: ", ex);
            if ( Util.isVerbose() || Util.isDebugMode() ) ex.printStackTrace();
            return true; // ...stop visitation process
        }
        catch (RDFHandlerException ex) {
            PreviewRDFRowVisitor.logger.error("Flush Issue: ", ex);
            if ( Util.isVerbose() || Util.isDebugMode() ) ex.printStackTrace();
            return true; // ...stop visitation process
        }

        return false;
    }
}
