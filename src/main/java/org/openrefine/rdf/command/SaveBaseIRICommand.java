package org.openrefine.rdf.command;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openrefine.rdf.model.Util;
import org.openrefine.rdf.RDFTransform;

import com.google.refine.model.Project;

import org.apache.jena.iri.IRI;

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
            IRI baseIRI = Util.buildIRI(strIRI);
            if (baseIRI == null) {
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
