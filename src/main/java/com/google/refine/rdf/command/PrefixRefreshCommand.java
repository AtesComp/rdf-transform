package com.google.refine.rdf.command;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.rdf.ApplicationContext;
import com.google.refine.rdf.RDFTransform;
import com.google.refine.rdf.model.Util;
import com.google.refine.rdf.model.vocab.VocabularyImportException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrefixRefreshCommand extends RDFTransformCommand {
	private final static Logger logger = LoggerFactory.getLogger("RDFT:PfxRefreshCmd");

	public PrefixRefreshCommand(ApplicationContext context) {
		super(context);
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Content-Type", "application/json");
		if ( ! this.hasValidCSRFToken(request) ) {
			PrefixRefreshCommand.respondCSRFError(response);
			return;
		}
		String strPrefix    = request.getParameter(Util.gstrPrefix);
		String strNamespace = request.getParameter(Util.gstrNamespace);
		String strProjectID = request.getParameter(Util.gstrProject); // NOT this.getProject(request);

		RDFTransform theTransform = this.getRDFTransform(request);

		// Remove the namespace...
		theTransform.removePrefix(strPrefix);

		Exception except = null;
		boolean bError = false;
		String strError = null;
		try{
			// Remove related vocabulary...
			this.getContext().
				getVocabularySearcher().
					deleteTermsOfVocab(strPrefix, strProjectID);

			// Re-add related vocabulary...
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
				PrefixRefreshCommand.logger.error("ERROR: " + strError + " vocabulary: ", except);
				if ( Util.isVerbose() || Util.isDebugMode() ) except.printStackTrace();
			}
			else { // ...warning...
				if ( Util.isVerbose() ) PrefixRefreshCommand.logger.warn("Prefix exists: ", except);
			}

			PrefixRefreshCommand.respondJSON(response, CodeResponse.error);
			return;
		}

		// Otherwise, all good...

		// Re-add the namespace...
		theTransform.addPrefix(strPrefix, strNamespace);

		PrefixRefreshCommand.respondJSON(response, CodeResponse.ok);
	}
}
