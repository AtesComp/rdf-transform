package com.google.refine.rdf.command;

import java.io.IOException;
import java.util.Properties;

import com.google.refine.rdf.RDFTransform;
import com.google.refine.rdf.app.ApplicationContext;
import com.google.refine.rdf.operation.ExportTemplateOperation;

import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.process.Process;
import com.google.refine.util.ParsingUtilities;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.JsonNode;

public class ExportTemplateCommand extends RDFTransformCommand {
    public ExportTemplateCommand(ApplicationContext ctxt) {
		super(ctxt);
	}

	@Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ( ! this.hasValidCSRFToken(request) ) {
            ExportTemplateCommand.respondCSRFError(response);
            return;
        }

        try {
            Project theProject = this.getProject(request);

            String strTransform = request.getParameter(RDFTransform.KEY);
            JsonNode jnodeRoot = ParsingUtilities.evaluateJsonStringToObjectNode(strTransform);
            RDFTransform theTransform = RDFTransform.reconstruct(jnodeRoot);

            AbstractOperation opExportStruct = new ExportTemplateOperation(theTransform);
            Process procExportStruct = opExportStruct.createProcess(theProject, new Properties());

            ExportTemplateCommand.performProcessAndRespond(request, response, theProject, procExportStruct);
        }
        catch (Exception ex) {
            ExportTemplateCommand.respondException(response, ex);
        }
    }
    
}
