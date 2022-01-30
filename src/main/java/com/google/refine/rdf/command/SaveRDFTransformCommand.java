package com.google.refine.rdf.command;

import java.io.IOException;
import java.util.Properties;

import com.google.refine.rdf.ApplicationContext;
import com.google.refine.rdf.RDFTransform;
import com.google.refine.rdf.model.operation.SaveRDFTransformOperation;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.process.Process;
import com.google.refine.util.ParsingUtilities;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.JsonNode;

public class SaveRDFTransformCommand extends RDFTransformCommand {

    public SaveRDFTransformCommand(ApplicationContext context) {
		super(context);
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
                return;
            }
            JsonNode jnodeRoot = ParsingUtilities.evaluateJsonStringToObjectNode(strTransform);
            if (jnodeRoot == null) {
                return;
            }
            RDFTransform theTransform = RDFTransform.reconstruct(this.getContext(), jnodeRoot);

            // Process the "save" operations...
            AbstractOperation opSave = new SaveRDFTransformOperation(theTransform);
            Process procSave = opSave.createProcess(theProject, new Properties());
            SaveRDFTransformCommand.performProcessAndRespond(request, response, theProject, procSave);
        }
        catch (Exception ex) {
            SaveRDFTransformCommand.respondException(response, ex);
        }
    }
}
