package com.google.refine.rdf.command;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.rdf.ApplicationContext;
import com.google.refine.rdf.model.Util;

public class AddPrefixCommand extends RDFTransformCommand {

	public AddPrefixCommand(ApplicationContext context) {
		super(context);
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		if ( ! this.hasValidCSRFToken(request) ) {
			AddPrefixCommand.respondCSRFError(response);
			return;
		}
		String strPrefix       = request.getParameter(Util.gstrPrefix).strip();
        String strNamespace    = request.getParameter(Util.gstrNamespace).strip();
        String strProjectID    = request.getParameter("project");
        String strFetchOption  = request.getParameter("fetch").strip();

        this.getRDFTransform(request).addPrefix(strPrefix, strNamespace);
		if ( strFetchOption.equals("web") ) {
			String strFetchURL = request.getParameter("fetch-url");
			if (strFetchURL == null || strFetchOption.isEmpty()) {
				strFetchURL = strNamespace;
			}
			try {
				this.getContext().
					getVocabularySearcher().
						importAndIndexVocabulary(
							strPrefix, strNamespace, strFetchURL, strProjectID);
			}
			catch (Exception ex) { // VocabularyImportException | IOException
				this.getRDFTransform(request).removePrefix(strPrefix);
				AddPrefixCommand.respondException(response, ex);
				return;
        	}
		}
		AddPrefixCommand.respondJSON(response, CodeResponse.ok);
    }
}
