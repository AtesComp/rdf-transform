package com.google.refine.rdf.command;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import com.google.refine.rdf.ApplicationContext;
import com.google.refine.rdf.RDFTransform;
import com.google.refine.rdf.model.Util;
import com.google.refine.commands.Command;
import com.google.refine.model.Project;

public abstract class RDFTransformCommand extends Command {

	private ApplicationContext context;

	public RDFTransformCommand(ApplicationContext context) {
		super();
		this.context = context;
	}

	public ApplicationContext getContext() {
		return this.context;
	}

	public RDFTransform getRDFTransform(HttpServletRequest request)
			throws ServletException {
		// Reset the RDF Transform preferences via the OpenRefine Preference Store
		// as it may have changed since last call...
		Util.setPreferencesByPreferenceStore();
		try {
            Project project = this.getProject(request);
		    return RDFTransform.getRDFTransform(this.context, project);
		}
		catch (ServletException ex) {
			throw new ServletException("Unable to retrieve Project!", ex);
		}
		catch (IOException ex) {
            throw new ServletException("Unable to retrieve RDF Transform!", ex);
		}
		catch (Exception ex) {
			throw new ServletException("Unable to retrieve Project or RDF Transform! (Other)", ex);
		}
	}
}
