package org.openrefine.rdf.model.exporter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Properties;

import org.openrefine.rdf.RDFTransform;
import org.openrefine.rdf.model.Util;
import org.openrefine.rdf.model.operation.ExportRDFRecordVisitor;
import org.openrefine.rdf.model.operation.ExportRDFRowVisitor;
import org.openrefine.rdf.model.operation.RDFVisitor;

import com.google.refine.browsing.Engine;
import com.google.refine.exporters.StreamExporter;
import com.google.refine.exporters.WriterExporter;
import com.google.refine.model.Project;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RDFExporter implements WriterExporter, StreamExporter {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:RDFExporter");

    private RDFFormat format;

    public RDFExporter(RDFFormat format) {
        this.format = format;
        if ( Util.isDebugMode() ) RDFExporter.logger.info("DEBUG: Preparing exporter " + format.getName() + "...");
    }

    public void export(Project theProject, Properties options, Engine theEngine,
                        OutputStream outputStream)
            throws IOException {
        if ( Util.isDebugMode() ) RDFExporter.logger.info("DEBUG: Exporting " + format.getName() + " via OutputStream");
        this.export(theProject, options, theEngine, Rio.createWriter(this.format, outputStream));
    }

    public void export(Project theProject, Properties options, Engine theEngine,
                        Writer theWriter)
             throws IOException {
        if ( Util.isDebugMode() ) RDFExporter.logger.info("DEBUG: Exporting " + format.getName() + " via Writer");
        this.export(theProject, options, theEngine, Rio.createWriter(this.format, theWriter));
    }

    private void export(Project theProject, Properties options, Engine theEngine,
                        RDFWriter theWriter)
            throws IOException {
        RDFTransform theTransform;
        theTransform = RDFTransform.getRDFTransform(theProject);
        try {
            if ( Util.isDebugMode() ) RDFExporter.logger.info("  Starting RDF...");
            theWriter.startRDF();

            // Process all records/rows of data for statements...
            RDFVisitor theVisitor = null;
            if ( theProject.recordModel.hasRecords() ) {
                if ( Util.isDebugMode() ) RDFExporter.logger.info("    Process by Record Visitor...");
                theVisitor = new ExportRDFRecordVisitor(theTransform, theWriter);
            }
            else {
                if ( Util.isDebugMode() ) RDFExporter.logger.info("    Process by Row Visitor...");
                theVisitor = new ExportRDFRowVisitor(theTransform, theWriter);
            }
            theVisitor.buildModel(theProject, theEngine);

            theWriter.endRDF();
            if ( Util.isDebugMode() ) RDFExporter.logger.info("  ...Ended RDF.");
        }
        catch (RDFHandlerException ex) {
            if ( Util.isDebugMode() ) RDFExporter.logger.error("DEBUG: Error exporting " + format.getName(), ex);
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    public String getContentType() {
        if (this.format != null) {
            return this.format.getDefaultMIMEType();
        }
        else { // ...export as Turtle...
            return RDFFormat.TURTLE.getDefaultMIMEType();
        }
    }
}
