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
import java.util.Map;

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
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFWriterRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class RDFStreamExporter<br />
 *<br />
 *  An exporter used to transform OpenRefine project data to RDF *at scale* meaning discreet data chunks
 *  are processed and dumped to persistent storage sequentially until complete.  Therefore, only discreet
 *  memory and processing are perform no matter how large the project data.  Additionally, the memory can
 *  be optimized for a predetermined size to minimize the number of memory to persistent storage writes.
 */
public class RDFStreamExporter extends RDFExporter implements WriterExporter, StreamExporter {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:RDFStreamExporter");

    private OutputStream theOutputStream = null;

    public RDFStreamExporter(RDFFormat format, String strLang) {
        super(format, strLang);
    }

    @Override
    public void export(Project theProject, Map<String, String> options, Engine theEngine, OutputStream outputStream)
            throws IOException {
        if ( Util.isDebugMode() ) RDFStreamExporter.logger.info("DEBUG: Exporting " + this.theExportLang + " via OutputStream");
        this.theOutputStream = outputStream;
        this.export(theProject, options, theEngine);
    }

    @Override
    public void export(Project theProject, Map<String, String> options, Engine theEngine, final Writer someWriter)
             throws IOException
    {
        if ( Util.isDebugMode() ) RDFStreamExporter.logger.info("DEBUG: Exporting " + this.theExportLang + " via Writer");
        this.theOutputStream = WriterOutputStream.builder().setWriter(someWriter).setCharset("UTF-8").get();
        this.export(theProject, options, theEngine);
    }

    private void export(Project theProject, Map<String, String> options, Engine theEngine)
            throws IOException
    {
        RDFTransform theTransform = RDFTransform.getRDFTransform(theProject);
        try {
            if ( Util.isDebugMode() ) RDFStreamExporter.logger.info("DEBUG:   Starting RDF Export...");

            // Process all records/rows of data for statements...
            RDFVisitor theVisitor = null;
            if ( theProject.recordModel.hasRecords() ) {
                if ( Util.isDebugMode() ) RDFStreamExporter.logger.info("DEBUG:     Process by Record Visitor...");
                theVisitor = new ExportRDFRecordVisitor(theTransform);
            }
            else {
                if ( Util.isDebugMode() ) RDFStreamExporter.logger.info("DEBUG:     Process by Row Visitor...");
                theVisitor = new ExportRDFRowVisitor(theTransform);
            }

            if ( Util.isDebugMode() ) RDFStreamExporter.logger.info("DEBUG:     Building the graph...");
            theVisitor.buildDSGraph(theProject, theEngine); // ...auto-closes as theVisitor has a writer: theWriter != null

            if ( Util.isDebugMode() ) RDFStreamExporter.logger.info("DEBUG:     Writing the graph as " + this.theExportLang + "...");
            if      ( RDFWriterRegistry.getWriterDatasetFactory( this.getFormat() ) != null) {
                RDFDataMgr.write( this.theOutputStream, theVisitor.getDSGraph(), this.getFormat() ); // ...multi-graph
            }
            else if ( RDFWriterRegistry.getWriterGraphFactory( this.getFormat() ) != null) {
                RDFDataMgr.write( this.theOutputStream, theVisitor.getDSGraph().getUnionGraph(), this.getFormat() ); // ...single graph
            }
            else throw new IOException("Dataset does not have a Dataset or Graph writer for " + this.theExportLang + "!");

            theVisitor.closeDSGraph(); // ...close since the theVisitor has no writer: theWriter == null

            if ( Util.isDebugMode() ) RDFStreamExporter.logger.info("DEBUG:   ...Ended RDF Export " + this.theExportLang);
        }
        catch (Exception ex) {
            RDFStreamExporter.logger.error("ERROR: Error exporting " + this.theExportLang, ex);
            if ( Util.isVerbose() ) ex.printStackTrace();
            throw new IOException(ex.getMessage(), ex);
        }
    }
}
