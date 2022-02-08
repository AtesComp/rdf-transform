package com.google.refine.rdf.command;

import com.google.refine.rdf.RDFTransform;
import com.google.refine.rdf.model.Util;
import com.google.refine.rdf.model.operation.PreviewRDFRecordVisitor;
import com.google.refine.rdf.model.operation.PreviewRDFRowVisitor;
import com.google.refine.rdf.model.operation.RDFVisitor;
import com.google.refine.browsing.Engine;
import com.google.refine.commands.Command;
import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringWriter;

//import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreviewRDFCommand extends Command {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:PrevRDFCmd");

    public String strStatements;

	public PreviewRDFCommand() {
	}

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // No CSRF Token required for this command.

		try {
            Project theProject = this.getProject(request);
            Engine theEngine = PreviewRDFCommand.getEngine(request, theProject);

            String strTransform = request.getParameter(RDFTransform.KEY);
            JsonNode jnodeRoot = ParsingUtilities.evaluateJsonStringToObjectNode(strTransform);
            RDFTransform theTransform = RDFTransform.reconstruct(theProject, jnodeRoot);

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

            // Setup writer for output...
            StringWriter strwriterBase = new StringWriter();
	        RDFWriter theWriter = Rio.createWriter(RDFFormat.TURTLE, strwriterBase);

            // Start writing...
			theWriter.startRDF();

            // Process sample records/rows of data for statements...
	        RDFVisitor theVisitor = null;
            if ( theProject.recordModel.hasRecords() ) {
                theVisitor = new PreviewRDFRecordVisitor(theTransform, theWriter);
            }
            else {
                theVisitor = new PreviewRDFRowVisitor(theTransform, theWriter);
            }
            theVisitor.buildModel(theProject, theEngine);

            theWriter.endRDF();
            // ...end writing

            this.strStatements = strwriterBase.getBuffer().toString();
            if ( Util.isVerbose(4) || Util.isDebugMode() ) PreviewRDFCommand.logger.info("Preview Statements:\n" + strStatements);

            // Send back to client...
            PreviewRDFCommand.respondJSON( response, new CodeResponse(strStatements) );
        }
		catch (Exception ex) {
            PreviewRDFCommand.respondJSON(response, CodeResponse.error);
        }
    }
}
