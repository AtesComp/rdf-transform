package com.google.refine.rdf.command;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import com.google.refine.rdf.RDFTransform;
import com.google.refine.rdf.model.Util;
import com.google.refine.commands.Command;
import com.google.refine.model.Project;

public abstract class RDFTransformCommand extends Command {

    public RDFTransformCommand() {
        super();
    }

    public RDFTransform getRDFTransform(HttpServletRequest request)
            throws ServletException {
        // Reset the RDF Transform preferences via the OpenRefine Preference Store
        // as it may have changed since last call...
        Util.setPreferencesByPreferenceStore();
        try {
            Project theProject = this.getProject(request);
            return RDFTransform.getRDFTransform(theProject);
        }
        catch (ServletException ex) {
            throw new ServletException("Unable to retrieve Project!", ex);
        }
        catch (Exception ex) {
            throw new ServletException("Unable to retrieve Project or RDF Transform! (Other)", ex);
        }
    }
}
