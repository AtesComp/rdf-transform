package com.google.refine.rdf.command;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.rdf.RDFTransform;
import com.google.refine.rdf.model.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrefixRemoveCommand extends RDFTransformCommand {
	private final static Logger logger = LoggerFactory.getLogger("RDFT:PfxRemoveCmd");

	public PrefixRemoveCommand() {
		super();
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		if ( Util.isDebugMode() ) PrefixRemoveCommand.logger.info("Removing prefix...");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Content-Type", "application/json");
		if ( ! this.hasValidCSRFToken(request) ) {
			PrefixRemoveCommand.respondCSRFError(response);
			return;
		}
		// For Project, DO NOT USE this.getProject(request) as we only need the string...
		String strProjectID = request.getParameter(Util.gstrProject);

		String strPrefix = request.getParameter(Util.gstrPrefix);

		if ( ! this.getRDFTransform(request).removePrefix(strPrefix) ) {
			if ( Util.isDebugMode() ) PrefixRemoveCommand.logger.info("...failed.");
			PrefixRemoveCommand.respondJSON(response, CodeResponse.error);
			return;
		}

		try {
			RDFTransform.getGlobalContext().
				getVocabularySearcher().
					deleteTermsOfVocab(strPrefix, strProjectID);
		}
		catch (Exception ex) {
			if ( Util.isDebugMode() ) PrefixRemoveCommand.logger.info("...vocabulary removal problems...");
		}

		if ( Util.isDebugMode() ) PrefixRemoveCommand.logger.info("...removed.");
		PrefixRemoveCommand.respondJSON(response, CodeResponse.ok);
	}
}
