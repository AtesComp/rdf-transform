package com.google.refine.rdf.command;

import java.io.IOException;
import java.util.Properties;

import com.google.refine.rdf.RDFTransform;
import com.google.refine.rdf.model.Util;
import com.google.refine.rdf.model.operation.SaveRDFTransformOperation;
import com.google.refine.model.Project;
import com.google.refine.process.Process;
import com.google.refine.util.ParsingUtilities;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SaveRDFTransformCommand extends RDFTransformCommand {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:SaveRDFTransCmd");

    public SaveRDFTransformCommand() {
		super();
	}

	@Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ( Util.isDebugMode() ) SaveRDFTransformCommand.logger.info("DEBUG: Reconstructing for Save...");
        if ( ! this.hasValidCSRFToken(request) ) {
            if ( Util.isDebugMode() ) SaveRDFTransformCommand.logger.info("  No CSRF Token.");
            SaveRDFTransformCommand.respondCSRFError(response);
            return;
        }

        try {
            // Get the project...
            Project theProject = this.getProject(request);

            // Get the RDF Transform...
            String strTransform = request.getParameter(RDFTransform.KEY);
            if (strTransform == null) {
                if ( Util.isDebugMode() ) SaveRDFTransformCommand.logger.info("  No Transform JSON.");
                SaveRDFTransformCommand.respondJSON(response, CodeResponse.error);
                return;
            }
            JsonNode jnodeTransform = ParsingUtilities.evaluateJsonStringToObjectNode(strTransform);
            if ( jnodeTransform == null || jnodeTransform.isNull() || jnodeTransform.isEmpty() ) {
                if ( Util.isDebugMode() ) SaveRDFTransformCommand.logger.info("  No Transform.");
                SaveRDFTransformCommand.respondJSON(response, CodeResponse.error);
                return;
            }
            RDFTransform theTransform = RDFTransform.reconstruct(theProject, jnodeTransform);

            // Process the "save" operations...
            SaveRDFTransformOperation opSave = new SaveRDFTransformOperation(theTransform);
            Process procSave = opSave.createProcess(theProject, new Properties());
            SaveRDFTransformCommand.performProcessAndRespond(request, response, theProject, procSave);
        }
        catch (Exception ex) {
            SaveRDFTransformCommand.respondJSON(response, CodeResponse.error);
        }
    }
}
