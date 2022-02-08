package com.google.refine.rdf.command;

import java.io.IOException;
import java.util.Properties;

import com.google.refine.rdf.RDFTransform;
import com.google.refine.rdf.model.operation.SaveRDFTransformOperation;
import com.google.refine.model.Project;
import com.google.refine.process.Process;
import com.google.refine.util.ParsingUtilities;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.JsonNode;

public class SaveRDFTransformCommand extends RDFTransformCommand {

    public SaveRDFTransformCommand() {
		super();
	}

	@Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ( ! this.hasValidCSRFToken(request) ) {
            SaveRDFTransformCommand.respondCSRFError(response);
            return;
        }

        try {
            // Get the project...
            Project theProject = this.getProject(request);

            // Get the RDF Transform...
            String strTransform = request.getParameter(RDFTransform.KEY);
            if (strTransform == null) {
                SaveRDFTransformCommand.respondJSON(response, CodeResponse.error);
                return;
            }
            JsonNode jnodeRoot = ParsingUtilities.evaluateJsonStringToObjectNode(strTransform);
            if (jnodeRoot == null) {
                SaveRDFTransformCommand.respondJSON(response, CodeResponse.error);
                return;
            }
            RDFTransform theTransform = RDFTransform.reconstruct(theProject, jnodeRoot);

            // Process the "save" operations...
            SaveRDFTransformOperation opSave = new SaveRDFTransformOperation(theProject, theTransform);
            Process procSave = opSave.createProcess(theProject, new Properties());
            SaveRDFTransformCommand.performProcessAndRespond(request, response, theProject, procSave);
        }
        catch (Exception ex) {
            SaveRDFTransformCommand.respondJSON(response, CodeResponse.error);
        }
    }
}
