package com.google.refine.rdf.command;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.rdf.app.ApplicationContext;
import com.google.refine.rdf.Util;
import com.google.refine.rdf.vocab.PrefixExistException;
import com.google.refine.rdf.vocab.VocabularyImportException;
import com.google.refine.rdf.vocab.VocabularyIndexException;

public class RefreshPrefixCommand extends RDFTransformCommand{

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
		String strPrefix    = request.getParameter("name");
		String strNamespace = request.getParameter("iri");
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
		catch (PrefixExistException ex) {
			except = ex;
		}
		catch (VocabularyIndexException ex) {
			bError = true;
			strError = "Indexing";
			except = ex;
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
				logger.error("ERROR: " + strError + " vocabulary: ", except);
				if ( Util.isVerbose(4) ) except.printStackTrace();
			}
			else { // ...warning...
				logger.warn("Prefix exists: ", except);
			}

			RefreshPrefixCommand.respondException(response, except);
			return;
		}

		// Otherwise, all good...
		RefreshPrefixCommand.respondJSON(response, CodeResponse.ok);
	}
}
