package org.openrefine.rdf.model.exporter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
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

import org.apache.commons.io.output.WriterOutputStream;

import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RDFExporter implements WriterExporter, StreamExporter {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:RDFExporter");

    private RDFFormat format;

    public RDFExporter(RDFFormat format) {
        this.format = format;
        if ( Util.isDebugMode() ) RDFExporter.logger.info("DEBUG: Preparing exporter " + format.getLang().getName() + "...");
    }

    public void export(Project theProject, Properties options, Engine theEngine,
                        OutputStream outputStream)
            throws IOException {
        if ( Util.isDebugMode() ) RDFExporter.logger.info("DEBUG: Exporting " + this.format.getLang().getName() + " via OutputStream");
        StreamRDF theWriter = StreamRDFWriter.getWriterStream(outputStream, this.format);
        this.export(theProject, options, theEngine, theWriter);
    }

    public void export(Project theProject, Properties options, Engine theEngine,
                        Writer theWriter)
             throws IOException {
        if ( Util.isDebugMode() ) RDFExporter.logger.info("DEBUG: Exporting " + this.format.getLang().getName() + " via Writer");
        WriterOutputStream outputStream = new WriterOutputStream(theWriter, Charset.forName("UTF-8"));
        StreamRDF theNewWriter = StreamRDFWriter.getWriterStream(outputStream, this.format);
        this.export(theProject, options, theEngine, theNewWriter);
    }

    private void export(Project theProject, Properties options, Engine theEngine, StreamRDF theWriter)
            throws IOException {
        RDFTransform theTransform;
        theTransform = RDFTransform.getRDFTransform(theProject);
        try {
            if ( Util.isDebugMode() ) RDFExporter.logger.info("  Starting RDF...");
            theWriter.start();

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

            theWriter.finish();;
            if ( Util.isDebugMode() ) RDFExporter.logger.info("  ...Ended RDF.");
        }
        catch (Exception ex) {
            if ( Util.isDebugMode() ) RDFExporter.logger.error("DEBUG: Error exporting " + this.format.getLang().getName(), ex);
            if ( Util.isVerbose() || Util.isDebugMode() ) ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    public String getContentType() {
        if (this.format != null) {
            return this.format.getLang().getContentType().getContentTypeStr();
        }
        else { // ...export as Turtle...
            return RDFFormat.TURTLE.getLang().getContentType().getContentTypeStr();
        }
    }
}
