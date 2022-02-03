package com.google.refine.rdf.command;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.rdf.ApplicationContext;
import com.google.refine.rdf.model.Util;
import com.google.refine.rdf.model.vocab.VocabularyImportException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RefreshPrefixCommand extends RDFTransformCommand {
	private final static Logger logger = LoggerFactory.getLogger("RDFT:RefPrefixCmd");

	public RefreshPrefixCommand(ApplicationContext context) {
		super(context);
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Content-Type", "application/json");
		if (!this.hasValidCSRFToken(request)) {
			RefreshPrefixCommand.respondCSRFError(response);
			return;
		}
		String strPrefix    = request.getParameter("prefix");
		String strNamespace = request.getParameter("namespace");
		String strProjectID = request.getParameter("project");
		this.getRDFTransform(request).removePrefix(strPrefix);

		Exception except = null;
		boolean bError = false;
		String strError = null;
		try{
			this.getContext().
				getVocabularySearcher().
					deleteTermsOfVocab(strPrefix, strProjectID);

			this.getContext().
				getVocabularySearcher().
					importAndIndexVocabulary(strPrefix, strNamespace, strNamespace, strProjectID);
        }
		catch (VocabularyImportException ex) {
			bError = true;
			strError = "Importing";
			except = ex;
		}
		catch (Exception ex) {
			bError = true;
			strError = "Processing";
			except = ex;
		}

		// Some problem occurred....
		if (except != null) {
			if (bError) {// ...error...
				RefreshPrefixCommand.logger.error("ERROR: " + strError + " vocabulary: ", except);
				if ( Util.isVerbose() || Util.isDebugMode() ) except.printStackTrace();
			}
			else { // ...warning...
				if ( Util.isVerbose() ) RefreshPrefixCommand.logger.warn("Prefix exists: ", except);
			}

			RefreshPrefixCommand.respondException(response, except);
			return;
		}

		// Otherwise, all good...
		RefreshPrefixCommand.respondJSON(response, CodeResponse.ok);
	}
}
