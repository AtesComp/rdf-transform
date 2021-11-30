package com.google.refine.rdf.command;

import java.io.IOException;
import org.eclipse.rdf4j.common.net.ParsedIRI;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.rdf.app.ApplicationContext;
import com.google.refine.rdf.RDFTransform;
import com.google.refine.rdf.Util;

import com.google.refine.model.Project;

public class SaveBaseIRICommand extends RDFTransformCommand {

    public SaveBaseIRICommand(ApplicationContext context) {
		super(context);
	}

	@Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ( !hasValidCSRFToken(request) ) {
            respondCSRFError(response);
            return;
        }
        try {
            Project theProject = this.getProject(request);
            String strIRI = request.getParameter("baseIRI");
            ParsedIRI baseIRI;
            try {
            	baseIRI = Util.buildIRI(strIRI);
            }
            catch (RuntimeException ex) {
            	respondException(response, ex);
            	return;
            }
            RDFTransform.getRDFTransform( this.getContext(), theProject ).setBaseIRI(baseIRI);

            theProject.getMetadata().updateModified();

            respond(response, "OK", "Base IRI saved");
        }
        catch (Exception ex) {
            respondException(response, ex);
        }
    }
}
