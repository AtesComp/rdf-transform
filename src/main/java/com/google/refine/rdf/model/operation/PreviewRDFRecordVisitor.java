package com.google.refine.rdf.model.operation;

import java.util.List;

import com.google.refine.rdf.RDFTransform;
import com.google.refine.rdf.model.ResourceNode;
import com.google.refine.rdf.model.Util;
import com.google.refine.model.Project;
import com.google.refine.model.Record;

import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreviewRDFRecordVisitor extends RDFRecordVisitor {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:PrevRDFRecV");

    private int iLimit = Util.getSampleLimit();
    private int iCount = 0;

    public PreviewRDFRecordVisitor(RDFTransform theTransform, RDFWriter theWriter) {
        super(theTransform, theWriter);
        this.iLimit = Util.getSampleLimit();
        if ( Util.isDebugMode() ) PreviewRDFRecordVisitor.logger.info("DEBUG: Created...");
    }

    public boolean visit(Project theProject, Record theRecord) {
        // Test for end of sample...
        if ( this.iLimit > 0 && this.iCount >= this.iLimit ) {
            return true; // ...stop visitation process
        }
        try {
            if ( Util.isDebugMode() ) PreviewRDFRecordVisitor.logger.info("DEBUG: Visiting Record: " + theRecord.recordIndex + " on count: " +  this.iCount);
            ParsedIRI baseIRI = this.getRDFTransform().getBaseIRI();
            RepositoryConnection theConnection = this.getModel().getConnection();
            ValueFactory theFactory = theConnection.getValueFactory();
            List<ResourceNode> listRoots = this.getRDFTransform().getRoots();
            for ( ResourceNode root : listRoots ) {
                root.createStatements(baseIRI, theFactory, theConnection, theProject, theRecord);

                if ( Util.isDebugMode() ) {
                    PreviewRDFRecordVisitor.logger.info("DEBUG:   " +
                        "Root: " + root.getNodeName() + "(" + root.getNodeType() + ")  " +
                        "Size: " + theConnection.size()
                    );
                }
            }
            this.iCount += 1;

            // Flush all statements...
            this.flushStatements();
        }
        catch (RepositoryException ex) {
            PreviewRDFRecordVisitor.logger.error("Connection Issue: ", ex);
            if ( Util.isVerbose() || Util.isDebugMode() ) ex.printStackTrace();
            return true; // ...stop visitation process
        }
        catch (RDFHandlerException ex) {
            PreviewRDFRecordVisitor.logger.error("Flush Issue: ", ex);
            if ( Util.isVerbose() || Util.isDebugMode() ) ex.printStackTrace();
            return true; // ...stop visitation process
        }

        return false;
    }
}
