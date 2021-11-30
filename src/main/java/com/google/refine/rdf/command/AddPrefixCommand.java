package com.google.refine.rdf.command;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.rdf.app.ApplicationContext;
import com.google.refine.rdf.vocab.PrefixExistException;
import com.google.refine.rdf.vocab.VocabularyImportException;

public class AddPrefixCommand extends RDFTransformCommand {

	public AddPrefixCommand(ApplicationContext ctxt) {
		super(ctxt);
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		if ( ! this.hasValidCSRFToken(request) ) {
			AddPrefixCommand.respondCSRFError(response);
			return;
		}
		String strPrefix       = request.getParameter("name").strip();
        String strNamespace    = request.getParameter("iri").strip();
        String strProjectID    = request.getParameter("project");
        String strFetchOption  = request.getParameter("fetch").strip();
        try {
        	this.getRDFTransform(request).addPrefix(strPrefix, strNamespace);
			if ( strFetchOption.equals("web") ) {
        		String strFetchURL = request.getParameter("fetch-url");
        		if (strFetchURL == null || strFetchOption.isEmpty()) {
        			strFetchURL = strNamespace;
        		}
        		this.getContext().
					getVocabularySearcher().
						importAndIndexVocabulary(
							strPrefix, strNamespace, strFetchURL, strProjectID);
			}
			AddPrefixCommand.respondJSON(response, CodeResponse.ok);
        }
		catch (PrefixExistException | VocabularyImportException ex) {
			this.getRDFTransform(request).removePrefix(strPrefix);
			AddPrefixCommand.respondException(response, ex);
        }
		catch (Exception ex) {
            AddPrefixCommand.respondException(response, ex);
        }
    }
}
