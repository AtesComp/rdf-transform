package com.google.refine.rdf.command;

import com.google.refine.rdf.operation.PreviewRDFRecordVisitor;
import com.google.refine.rdf.operation.PreviewRDFRowVisitor;
import com.google.refine.rdf.RDFTransform;
import com.google.refine.rdf.Util;
import com.google.refine.rdf.vocab.Vocabulary;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreviewRDFCommand extends Command {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:PrevRDFCmd");

    private int iRowLimit = 10; // Default

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
		try {
            //response.setCharacterEncoding("UTF-8");
            //response.setHeader("Content-Type", "application/json");

            Project theProject = this.getProject(request);
            Engine theEngine = PreviewRDFCommand.getEngine(request, theProject);

            String strTransform = request.getParameter(RDFTransform.KEY);
            JsonNode jnodeRoot = ParsingUtilities.evaluateJsonStringToObjectNode(strTransform);
            RDFTransform theTransform = RDFTransform.reconstruct(jnodeRoot);

            String strRowLimit = request.getParameter("rowLimit");
            int iPassRowLimit = this.iRowLimit; // ...set to default limit
            if (strRowLimit != null) {
                try {
                    iPassRowLimit = Integer.parseInt(strRowLimit); // set to user limit
                }
                catch (NumberFormatException ex) {
                    // ignore, use default...
                }
            }

            // Setup writer for output...
            StringWriter strwriterBase = new StringWriter();
	        RDFWriter theWriter = Rio.createWriter(RDFFormat.TURTLE, strwriterBase);
	        PreviewRDFRecordVisitor theRecordVisitor = null;
	        PreviewRDFRowVisitor theRowVisitor = null;
            boolean bRecordMode = theProject.recordModel.hasRecords();
            if (bRecordMode) {
                theRecordVisitor = new PreviewRDFRecordVisitor(theTransform, theWriter, iPassRowLimit);
            }
            else {
                theRowVisitor = new PreviewRDFRowVisitor(theTransform, theWriter, iPassRowLimit);
            }

            // Start writing...
			theWriter.startRDF();

            // Process vocabularies...
	        for ( Vocabulary vocab : theTransform.getPrefixesMap().values() ) {
		        theWriter.handleNamespace( vocab.getPrefix(), vocab.getNamespace() );
	        }

            // Process sample records/rows of data...
            if (bRecordMode) {
                Util.buildModel(theProject, theEngine, theRecordVisitor);
            }
            else {
                Util.buildModel(theProject, theEngine, theRowVisitor);
            }

            theWriter.endRDF();
            // ...end writing

            String strStatements = strwriterBase.getBuffer().toString();
            if ( Util.isVerbose(4) )
                logger.info("Preview Statements:\n" + strStatements);

            // Send back to client...
            PreviewRDFCommand.respondJSON( response, new PreviewResponse( strStatements ) );
        }
		catch (Exception ex) {
            PreviewRDFCommand.respondException(response, ex);
        }
    }

    private class PreviewResponse {
    	@JsonProperty("v")
    	String strValue;

    	protected PreviewResponse(String strValue) {
    		this.strValue = strValue;
    	}
    }
}
