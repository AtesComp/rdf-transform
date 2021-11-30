package com.google.refine.rdf.command;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import com.google.refine.rdf.RDFTransform;
import com.google.refine.rdf.Util;
import com.google.refine.rdf.app.ApplicationContext;
import com.google.refine.rdf.vocab.VocabularyIndexException;

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

	public RDFTransform getRDFTransform(HttpServletRequest request) throws ServletException {
		Util.setVerbosityByPreferenceStore(); // ...reset verbosity as it might have changed since last call
		try {
            Project project = getProject(request);
		    return RDFTransform.getRDFTransform(this.context, project);
		}
		catch (ServletException ex) {
			throw new ServletException("Servlet: Unable to retrieve Project!", ex);
		}
		catch (VocabularyIndexException ex) {
			throw new ServletException("Vocab: Unable to retrieve RDF Transform!", ex);
		}
		catch (IOException ex) {
            throw new ServletException("IO: Unable to retrieve RDF Transform!", ex);
		}
		catch (Exception ex) {
			throw new ServletException("Other: Unable to retrieve Project or RDF Transform!", ex);
		}
	}

	protected boolean hasValidCSRFTokenAsHeader(HttpServletRequest request) throws ServletException {
		if (request == null) {
			throw new IllegalArgumentException("Parameter 'request' should not be null!");
		}
		try {
			String token = request.getHeader("X-CSRF-TOKEN");
			return token != null && csrfFactory.validToken(token);
		}
		catch (Exception e) {
			throw new ServletException("Can't find CSRF token: missing or bad URL parameter!", e);
		}
	}
}
