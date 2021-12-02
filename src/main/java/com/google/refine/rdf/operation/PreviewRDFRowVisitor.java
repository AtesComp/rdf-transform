package com.google.refine.rdf.operation;

import java.util.List;

import com.google.refine.rdf.ResourceNode;
import com.google.refine.rdf.Util;
import com.google.refine.rdf.RDFTransform;

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
            if ( Util.isVerbose(4) )
                logger.info("Visiting Row: " + iRowIndex + " on count: " +  this.iCount);
            ParsedIRI baseIRI = this.getRDFTransform().getBaseIRI();
            RepositoryConnection theConnection = this.getModel().getConnection();
            ValueFactory theFactory = theConnection.getValueFactory();
            List<ResourceNode> listRoots = this.getRDFTransform().getRoots();
            for ( ResourceNode root : listRoots ) {
                root.createStatements(baseIRI, theFactory, theConnection, theProject,
                                        theRow, iRowIndex );
                if ( Util.isVerbose(4) )
                    logger.info("    " +
                        "Root: " + root.getNodeName() + "(" + root.getNodeType() + ")  " +
                        "Size: " + theConnection.size()
                    );
            }
            this.iCount += 1;

            this.flushStatements();
        }
        catch (RepositoryException ex) {
            logger.warn("Connection Issue: ", ex);
            if ( Util.isVerbose(4) ) ex.printStackTrace();
            return true; // ...stop visitation process
        }
        catch (RDFHandlerException ex) {
            logger.warn("Flush Issue: ", ex);
            if ( Util.isVerbose(4) ) ex.printStackTrace();
            return true; // ...stop visitation process
        }

        return false;
    }
}
