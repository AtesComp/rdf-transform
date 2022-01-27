package com.google.refine.rdf.command;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.rdf.ApplicationContext;
import com.google.refine.rdf.model.Util;
import com.google.refine.rdf.model.vocab.Vocabulary;
import com.google.refine.rdf.model.vocab.VocabularyImportException;
import com.google.refine.rdf.model.vocab.VocabularyList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetDefaultPrefixesCommand extends RDFTransformCommand {
	private final static Logger logger = LoggerFactory.getLogger("RDFT:GetDefPrefCmd");

	public GetDefaultPrefixesCommand(ApplicationContext context) {
		super(context);
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		if ( Util.isDebugMode() ) GetDefaultPrefixesCommand.logger.info("Getting default prefixes...");
		response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json");
        try{
			this.getDefaultPrefixes(request, response);
        }
		catch (Exception ex) {
            GetDefaultPrefixesCommand.respondException(response, ex);
        }
	}

	private void getDefaultPrefixes(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String projectId = request.getParameter("project");
		VocabularyList listVocabs = this.getRDFTransform(request).getPrefixes();
		if ( Util.isDebugMode() ) GetDefaultPrefixesCommand.logger.info("Existing prefixes: size=" + listVocabs.size());
		if ( listVocabs == null || listVocabs.isEmpty() ) {
			listVocabs = this.getContext().getPredefinedVocabularyManager().getPredefinedVocabularies().clone();
			if ( Util.isDebugMode() ) GetDefaultPrefixesCommand.logger.info("Predefined prefixes: size=" + listVocabs.size());
		}
		for (Vocabulary vocab : listVocabs) {
			if ( Util.isDebugMode() ) GetDefaultPrefixesCommand.logger.info("  Prefixes: " + vocab.getPrefix() + "  Namespace: " + vocab.getNamespace());
			Exception except = null;
			boolean bError = false;
			String strError = null;
			try {
				this.getContext().
					getVocabularySearcher().
						importAndIndexVocabulary(
							vocab.getPrefix(), vocab.getNamespace(), vocab.getNamespace(), projectId);
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
				// A Default Prefix vocabulary is not defined properly...
				//   Ignore the exception, but log it...
				if (bError) {// ...error...
					GetDefaultPrefixesCommand.logger.error("ERROR: " + strError + " vocabulary: ", except);
					if ( Util.isVerbose() ) except.printStackTrace();
				}
				else { // ...warning...
					if ( Util.isVerbose() ) GetDefaultPrefixesCommand.logger.warn("Prefix exists: ", except);
				}
				// ...continue processing the other vocabularies...
			}
		}
		GetDefaultPrefixesCommand.respondJSON(response, listVocabs);
	}
}
