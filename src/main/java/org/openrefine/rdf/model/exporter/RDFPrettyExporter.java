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
import com.google.refine.exporters.WriterExporter;
import com.google.refine.model.Project;

import org.apache.commons.io.output.WriterOutputStream;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Class RDFPrettyExporter
 *
 *  An exporter used to transform OpenRefine project data to RDF *in the prettiest format possible*
 *  meaning the data is condensed and formatted for readability.  This requires that the entire graph
 *  is accessible by the print process to organize repeated resources and literals and, therefore,
 *  cannot process discreet data chunks--i.e., it does not scale.  Furthermore, the entire graph must
 *  currently fit in the available memory as the graph used to dump the project data is a memory
 *  graph.
 *
 *  Use with relatively small graphs (compared to available memory) as the process could result in a
 *  critical failure.
 */
public class RDFPrettyExporter extends RDFExporter implements WriterExporter {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:RDFPrettyExporter");

    private OutputStream outputStream = null;

    public RDFPrettyExporter(RDFFormat format, String strName) {
        super(format, strName);
    }

    public void export(Project theProject, Properties options, Engine theEngine,
                        OutputStream outputStream)
            throws IOException {
        if ( Util.isDebugMode() ) RDFPrettyExporter.logger.info("DEBUG: Exporting " + this.strName + " via OutputStream");
        this.outputStream = outputStream;
        this.export(theProject, options, theEngine);
    }

    public void export(Project theProject, Properties options, Engine theEngine,
                        Writer theWriter)
             throws IOException
    {
        if ( Util.isDebugMode() ) RDFPrettyExporter.logger.info("DEBUG: Exporting " + this.strName + " via Writer");
        this.outputStream = new WriterOutputStream(theWriter, Charset.forName("UTF-8"));
        this.export(theProject, options, theEngine);
    }

    private void export(Project theProject, Properties options, Engine theEngine)
             throws IOException
    {
        RDFTransform theTransform = RDFTransform.getRDFTransform(theProject);
        try {
            if ( Util.isDebugMode() ) RDFPrettyExporter.logger.info("  Starting RDF Export...");

            // Process all records/rows of data for statements...
            RDFVisitor theVisitor = null;
            if ( theProject.recordModel.hasRecords() ) {
                if ( Util.isDebugMode() ) RDFPrettyExporter.logger.info("    Process by Record Visitor...");
                theVisitor = new ExportRDFRecordVisitor(theTransform, null);
            }
            else {
                if ( Util.isDebugMode() ) RDFPrettyExporter.logger.info("    Process by Row Visitor...");
                theVisitor = new ExportRDFRowVisitor(theTransform, null);
            }
            theVisitor.buildModel(theProject, theEngine);

            RDFDataMgr.write(this.outputStream, theVisitor.getModel(), this.format) ;
            if ( Util.isDebugMode() ) RDFPrettyExporter.logger.info("  ...Ended RDF Export.");
        }
        catch (Exception ex) {
            if ( Util.isDebugMode() ) RDFPrettyExporter.logger.error("DEBUG: Error exporting " + this.strName, ex);
            if ( Util.isVerbose() || Util.isDebugMode() ) ex.printStackTrace();
            throw new IOException(ex.getMessage(), ex);
        }
    }
}
