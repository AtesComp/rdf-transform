package org.openrefine.rdf.model.operation;

import java.util.List;

import org.openrefine.rdf.RDFTransform;
import org.openrefine.rdf.model.ResourceNode;
import org.openrefine.rdf.model.Util;

import com.google.refine.model.Project;
import com.google.refine.model.Record;

import org.apache.jena.iri.IRI;
import org.apache.jena.riot.system.StreamRDF;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportRDFRecordVisitor extends RDFRecordVisitor {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:ExportRDFRecV");

    public ExportRDFRecordVisitor(RDFTransform theTransform, StreamRDF theWriter) {
        super(theTransform, theWriter);
        if ( Util.isDebugMode() ) ExportRDFRecordVisitor.logger.info("DEBUG: Created...");
    }

    public boolean visit(Project theProject, Record theRecord) {
        try {
            if ( Util.isDebugMode() ) ExportRDFRecordVisitor.logger.info("DEBUG: Visiting Record: " + theRecord.recordIndex);
            IRI baseIRI = this.getRDFTransform().getBaseIRI();
            List<ResourceNode> listRoots = this.getRDFTransform().getRoots();
            for ( ResourceNode root : listRoots ) {
                root.createStatements(baseIRI, theModel, theProject, theRecord);

                if ( Util.isDebugMode() ) {
                    ExportRDFRecordVisitor.logger.info("DEBUG:   " +
                        "Root: " + root.getNodeName() + "(" + root.getNodeType() + ")  " +
                        "Model Size: " + theModel.size()
                    );
                }
                //
                // Flush Statements
                //
                // Write and clear a discrete set of statements from the repository connection
                // as the transformed statements use in-memory resources until flushed to disk.
                // Otherwise, large files would use excessive memory!
                //
                if ( theModel.size() > Util.getExportLimit() ) {
                    this.flushStatements();
                    if ( this.isNoWriter() && bLimitWarning) {
                        this.bLimitWarning = false;
                        ExportRDFRecordVisitor.logger.warn("WARNING:   Limit Reached: Memory may soon become exhausted!");
                    }
                }
            }

            // Flush any remaining statements...
            this.flushStatements();
        }
        catch (Exception ex) {
            ExportRDFRecordVisitor.logger.error("ERROR: Visit Issue: " + ex.getMessage(), ex);
            if ( Util.isVerbose() || Util.isDebugMode() ) ex.printStackTrace();
            return true; // ...stop visitation process
        }

        return false;
    }    
}
