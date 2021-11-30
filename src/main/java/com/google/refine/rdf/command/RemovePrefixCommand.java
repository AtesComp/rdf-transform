package com.google.refine.rdf.command;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.rdf.app.ApplicationContext;

public class RemovePrefixCommand extends RDFTransformCommand{

	public RemovePrefixCommand(ApplicationContext context) {
		super(context);
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Content-Type", "application/json");
		if ( ! this.hasValidCSRFToken(request) ) {
			RemovePrefixCommand.respondCSRFError(response);
			return;
		}
		String name = request.getParameter("name");
		String projectId = request.getParameter("project");
		this.getRDFTransform(request).removePrefix(name);

		// NOTE: No try{} catch{}, let it fail...
		this.getContext().getVocabularySearcher().deleteTermsOfVocab(name, projectId);

		RemovePrefixCommand.respondJSON(response, CodeResponse.ok);
	}
}
