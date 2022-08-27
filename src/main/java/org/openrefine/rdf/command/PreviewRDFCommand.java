package org.openrefine.rdf.command;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;

import org.openrefine.rdf.RDFTransform;
import org.openrefine.rdf.model.Util;
import org.openrefine.rdf.model.operation.PreviewRDFRecordVisitor;
import org.openrefine.rdf.model.operation.PreviewRDFRowVisitor;
import org.openrefine.rdf.model.operation.RDFVisitor;

import com.google.refine.browsing.Engine;
import com.google.refine.commands.Command;
import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;

import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
//import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFWriter;

import com.fasterxml.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreviewRDFCommand extends Command {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:PrevRDFCmd");

    public PreviewRDFCommand() {
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ( Util.isDebugMode() ) PreviewRDFCommand.logger.info("DEBUG: Reconstructing Transform for Preview...");
        // No CSRF Token required for this command.

        try {
            // Get the project and engine...
            Project theProject = this.getProject(request);
            Engine theEngine = PreviewRDFCommand.getEngine(request, theProject);

            // Get the RDF Transform...
            String strTransform = request.getParameter(RDFTransform.KEY);
            if (strTransform == null) {
                PreviewRDFCommand.logger.info("ERROR: No Transform JSON! Cannot construct preview.");
                PreviewRDFCommand.respondJSON(response, CodeResponse.error);
                return;
            }
            JsonNode jnodeTransform = ParsingUtilities.evaluateJsonStringToObjectNode(strTransform);
            if ( jnodeTransform == null || jnodeTransform.isNull() || jnodeTransform.isEmpty() ) {
                PreviewRDFCommand.logger.info("ERROR: No Transform JSON Node! Cannot construct preview.");
                PreviewRDFCommand.respondJSON(response, CodeResponse.error);
                return;
            }
            RDFTransform theTransform = RDFTransform.reconstruct(theProject, jnodeTransform);
            if (theTransform == null) {
                PreviewRDFCommand.logger.info("ERROR: No Transform available! Cannot construct preview.");
                PreviewRDFCommand.respondJSON(response, CodeResponse.error);
                return;
            }
            if ( Util.isDebugMode() ) PreviewRDFCommand.logger.info("DEBUG:   Transform reconstructed.");

            //if ( Util.isDebugMode() ) {
            //    PreviewRDFCommand.logger.info( "Given Transform:\n" + strTransform );
            //    StringWriter theStringWriter = new StringWriter();
            //    JsonGenerator jsonWriter = ParsingUtilities.mapper.getFactory().createGenerator(theStringWriter);
            //    theTransform.write(jsonWriter);
            //    PreviewRDFCommand.logger.info( "Processed Transform:\n" + theStringWriter.getBuffer().toString() );
            //}

            //
            // Process Preview Stream...
            //
            boolean bPreviewStream = Util.isPreviewStream(); // ...set to current type: Pretty (false) or Stream (true)
            String strPreviewStream = request.getParameter("bPreviewStream");
            if (strPreviewStream != null) { // ...a preview type was passed from the UI
                bPreviewStream = Boolean.parseBoolean(strPreviewStream); // ...set to preview type
            }
            if ( Util.isDebugMode() ) PreviewRDFCommand.logger.info("DEBUG:   Preview Stream processed: " + strPreviewStream + ", " + bPreviewStream);

            //
            // Process Sample Limit...
            //
            int iSampleLimit = Util.getSampleLimit(); // ...set to current limit
            String strSampleLimit = request.getParameter("iSampleLimit");
            if (strSampleLimit != null) { // ...a sample limit was passed from the UI
                try {
                    iSampleLimit = Integer.parseInt(strSampleLimit); // ...set to sample limit
                }
                catch (NumberFormatException ex) {
                    // ignore, use default...
                }
            }
            if ( iSampleLimit != Util.getSampleLimit() ) {
                Util.setSampleLimit(iSampleLimit);
            }
            if ( Util.isDebugMode() ) PreviewRDFCommand.logger.info("DEBUG:   Sample Limit processed: " + strSampleLimit + ", " + iSampleLimit);

            // Setup writer for output...
            ByteArrayOutputStream osOut = new ByteArrayOutputStream();
            if ( Util.isDebugMode() ) PreviewRDFCommand.logger.info("DEBUG:   BAOS Setup.");

            //
            // Get the Stream Writer if applicable...
            //
            StreamRDF theWriter = null;
            if ( bPreviewStream )  {
                theWriter = StreamRDFWriter.getWriterStream(osOut, RDFFormat.TURTLE_BLOCKS);
                if (theWriter == null) {
                    PreviewRDFCommand.logger.warn("WARN: Cannot construct Stream-based writer. Using pretty printer.");
                }
                else {
                    if ( Util.isDebugMode() ) PreviewRDFCommand.logger.info("DEBUG:   Acquired writer: StreamRDFWriter.");
                }
            }

            // Start writing...
            if ( Util.isDebugMode() ) PreviewRDFCommand.logger.info("DEBUG:   Starting RDF Processing...");
            if (theWriter != null) {
                theWriter.start();
            }

            //
            // Process sample records/rows of data for statements...
            //
            RDFVisitor theVisitor = null;
            if ( theProject.recordModel.hasRecords() ) {
                if ( Util.isDebugMode() ) PreviewRDFCommand.logger.info("DEBUG:     Process by Record Visitor...");
                theVisitor = new PreviewRDFRecordVisitor(theTransform, theWriter);
            }
            else {
                if ( Util.isDebugMode() ) PreviewRDFCommand.logger.info("DEBUG:     Process by Row Visitor...");
                theVisitor = new PreviewRDFRowVisitor(theTransform, theWriter);
            }
            if ( Util.isDebugMode() ) PreviewRDFCommand.logger.info("DEBUG:     Building the model...");
            theVisitor.buildModel(theProject, theEngine);

            // If Stream Writer, end Stream Writer...
            if (theWriter != null) {
                theWriter.finish();
            }
            // Otherwise, write and end Pretty Writer...
            else {
                RDFDataMgr.write(osOut, theVisitor.getModel(), RDFFormat.TURTLE);
                theVisitor.getModel().close();
            }

            if ( Util.isDebugMode() ) PreviewRDFCommand.logger.info("DEBUG:   ...Ended RDF Processing.");
            // ...end writing

            String strStatements = osOut.toString(StandardCharsets.UTF_8);
            if ( Util.isVerbose(4) || Util.isDebugMode() ) PreviewRDFCommand.logger.info("Preview Statements:\n" + strStatements);

            // Send back to client...
            PreviewRDFCommand.respondJSON( response, new CodeResponse(strStatements) );
        }
        catch (Exception ex) {
            PreviewRDFCommand.logger.error("ERROR: Constructing Preview:" + ex.getMessage(), ex);
            if ( Util.isVerbose() || Util.isDebugMode() ) ex.printStackTrace();
            PreviewRDFCommand.respondJSON(response, CodeResponse.error);
        }
    }
}
