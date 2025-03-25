/*
 *  Class RDFStreamExporter
 *
 *  A Stream optimized RDF Exporter.
 *
 *  Copyright 2025 Keven L. Ates
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.openrefine.rdf.model.exporter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
//import java.nio.charset.Charset;
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

/*
 * Class RDFStreamExporter
 *
 *  An exporter used to transform OpenRefine project data to RDF *at scale* meaning discreet data chunks
 *  are processed and dumped to persistent storage sequentially until complete.  Therefore, only discreet
 *  memory and processing are perform no matter how large the project data.  Additionally, the memory can
 *  be optimized for a predetermined size to minimize the number of memory to persistent storage writes.
 */
public class RDFStreamExporter extends RDFExporter implements WriterExporter, StreamExporter {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:RDFStreamExporter");

    private OutputStream theOutputStream = null;

    public RDFStreamExporter(RDFFormat format, String strName) {
        super(format, strName);
    }

    public void export(Project theProject, Properties options, Engine theEngine, OutputStream outputStream)
            throws IOException {
        if ( Util.isDebugMode() ) RDFStreamExporter.logger.info("DEBUG: Exporting " + this.theExportName + " via OutputStream");
        this.theOutputStream = outputStream;
        this.export(theProject, options, theEngine);
    }

    @Override
    public void export(Project theProject, Properties options, Engine theEngine, final Writer theWriter)
             throws IOException
    {
        if ( Util.isDebugMode() ) RDFStreamExporter.logger.info("DEBUG: Exporting " + this.theExportName + " via Writer");
        this.theOutputStream = WriterOutputStream.builder().setWriter(theWriter).setCharset("UTF-8").get();
        this.export(theProject, options, theEngine);
    }

    private void export(Project theProject, Properties options, Engine theEngine)
            throws IOException
    {
        StreamRDF theWriter = null;
        theWriter = StreamRDFWriter.getWriterStream(this.theOutputStream, this.theFormat);
        if (theWriter == null) {
            String strMsg = "ERROR: The writer is invalid! Cannot construct export.";
            RDFStreamExporter.logger.error(strMsg);
            throw new IOException(strMsg);
        }
        if ( Util.isDebugMode() ) RDFStreamExporter.logger.info("DEBUG:   Acquired writer: StreamRDFWriter.");

        RDFTransform theTransform = RDFTransform.getRDFTransform(theProject);
        try {
            if ( Util.isDebugMode() ) RDFStreamExporter.logger.info("DEBUG:   Starting RDF Export...");
            theWriter.start();

            // Process all records/rows of data for statements...
            RDFVisitor theVisitor = null;
            if ( theProject.recordModel.hasRecords() ) {
                if ( Util.isDebugMode() ) RDFStreamExporter.logger.info("DEBUG:     Process by Record Visitor...");
                theVisitor = new ExportRDFRecordVisitor(theTransform, theWriter);
            }
            else {
                if ( Util.isDebugMode() ) RDFStreamExporter.logger.info("DEBUG:     Process by Row Visitor...");
                theVisitor = new ExportRDFRowVisitor(theTransform, theWriter);
            }
            theVisitor.buildDSGraph(theProject, theEngine);

            theWriter.finish();
            if ( Util.isDebugMode() ) RDFStreamExporter.logger.info("DEBUG:   ...Ended RDF Export " + this.theExportName);
        }
        catch (Exception ex) {
            if ( Util.isDebugMode() ) RDFStreamExporter.logger.error("DEBUG: Error exporting " + this.theExportName, ex);
            if ( Util.isVerbose() || Util.isDebugMode() ) ex.printStackTrace();
            throw new IOException(ex.getMessage(), ex);
        }
    }
}
