package com.google.refine.rdf.command;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.rdf.RDFTransform;
import com.google.refine.rdf.model.Util;

public class NamespaceAddCommand extends RDFTransformCommand {

	public NamespaceAddCommand() {
		super();
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		if ( ! this.hasValidCSRFToken(request) ) {
			NamespaceAddCommand.respondCSRFError(response);
			return;
		}
		// For Project, DO NOT USE this.getProject(request) as we only need the string...
		String strProjectID = request.getParameter(Util.gstrProject);

		String strPrefix       = request.getParameter(Util.gstrPrefix).strip();
        String strNamespace    = request.getParameter(Util.gstrNamespace).strip();
        String strFetchOption  = request.getParameter("fetch").strip();

		if ( strFetchOption.equals("web") ) {
			String strFetchURL = request.getParameter("fetch-url");
			if (strFetchURL == null || strFetchOption.isEmpty()) {
				strFetchURL = strNamespace;
			}
			try {
				RDFTransform.getGlobalContext().
					getVocabularySearcher().
						importAndIndexVocabulary(
							strPrefix, strNamespace, strFetchURL, strProjectID);
			}
			catch (Exception ex) { // VocabularyImportException | IOException
				NamespaceAddCommand.respondJSON(response, CodeResponse.error);
				return;
        	}
		}

        this.getRDFTransform(request).addNamespace(strPrefix, strNamespace);

		NamespaceAddCommand.respondJSON(response, CodeResponse.ok);
    }
}
