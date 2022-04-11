package com.google.refine.rdf.command;

import java.io.IOException;
import org.eclipse.rdf4j.common.net.ParsedIRI;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.rdf.model.Util;
import com.google.refine.rdf.RDFTransform;
import com.google.refine.model.Project;

public class SaveBaseIRICommand extends RDFTransformCommand {

    public SaveBaseIRICommand() {
        super();
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ( ! this.hasValidCSRFToken(request) ) {
            SaveBaseIRICommand.respondCSRFError(response);
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
                SaveBaseIRICommand.respondJSON(response, CodeResponse.error);
                return;
            }
            RDFTransform.getRDFTransform(theProject).setBaseIRI(baseIRI);

            theProject.getMetadata().updateModified();
        }
        catch (Exception ex) {
            SaveBaseIRICommand.respondJSON(response, CodeResponse.error);
            return;
        }

        SaveBaseIRICommand.respondJSON(response, CodeResponse.ok);
    }
}
