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

import org.apache.jena.riot.RDFFormat;
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
                if ( Util.isDebugMode() ) PreviewRDFCommand.logger.info("  No Transform JSON.");
                PreviewRDFCommand.respondJSON(response, CodeResponse.error);
                return;
            }
            JsonNode jnodeTransform = ParsingUtilities.evaluateJsonStringToObjectNode(strTransform);
            if ( jnodeTransform == null || jnodeTransform.isNull() || jnodeTransform.isEmpty() ) {
                if ( Util.isDebugMode() ) PreviewRDFCommand.logger.info("  No Transform.");
                PreviewRDFCommand.respondJSON(response, CodeResponse.error);
                return;
            }
            RDFTransform theTransform = RDFTransform.reconstruct(theProject, jnodeTransform);
            if ( Util.isDebugMode() ) PreviewRDFCommand.logger.info("  Transform reconstructed.");

            //if ( Util.isDebugMode() ) {
            //    PreviewRDFCommand.logger.info( "Given Transform:\n" + strTransform );
            //    StringWriter theStringWriter = new StringWriter();
            //    JsonGenerator jsonWriter = ParsingUtilities.mapper.getFactory().createGenerator(theStringWriter);
            //    theTransform.write(jsonWriter);
            //    PreviewRDFCommand.logger.info( "Processed Transform:\n" + theStringWriter.getBuffer().toString() );
            //}

            //
            // Process Sample Limit...
            //
            String strLimit = request.getParameter("sampleLimit");
            int iLimit = Util.getSampleLimit(); // ...set to current limit
            if (strLimit != null) { // ...a limit was passed
                try {
                    iLimit = Integer.parseInt(strLimit); // ...set to user limit
                }
                catch (NumberFormatException ex) {
                    // ignore, use default...
                }
            }
            if ( iLimit != Util.getSampleLimit() ) {
                Util.setSampleLimit(iLimit);
            }
            if ( Util.isDebugMode() ) PreviewRDFCommand.logger.info("  Sample Limit processed.");

            // Setup writer for output...
            ByteArrayOutputStream osOut = new ByteArrayOutputStream();
            StreamRDF theWriter = StreamRDFWriter.getWriterStream(osOut, RDFFormat.TURTLE_BLOCKS);

            // Start writing...
            if ( Util.isDebugMode() ) PreviewRDFCommand.logger.info("  Starting RDF Processing...");
            theWriter.start();

            //
            // Process sample records/rows of data for statements...
            //
            RDFVisitor theVisitor = null;
            if ( theProject.recordModel.hasRecords() ) {
                if ( Util.isDebugMode() ) PreviewRDFCommand.logger.info("    Process by Record Visitor...");
                theVisitor = new PreviewRDFRecordVisitor(theTransform, theWriter);
            }
            else {
                if ( Util.isDebugMode() ) PreviewRDFCommand.logger.info("    Process by Row Visitor...");
                theVisitor = new PreviewRDFRowVisitor(theTransform, theWriter);
            }
            theVisitor.buildModel(theProject, theEngine);

            theWriter.finish();
            if ( Util.isDebugMode() ) PreviewRDFCommand.logger.info("  ...Ended RDF Processing.");
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
