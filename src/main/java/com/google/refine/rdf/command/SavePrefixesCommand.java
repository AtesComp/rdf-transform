package com.google.refine.rdf.command;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.refine.rdf.app.ApplicationContext;
import com.google.refine.rdf.vocab.Vocabulary;

import com.google.refine.util.ParsingUtilities;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SavePrefixesCommand extends RDFTransformCommand {

	public SavePrefixesCommand(ApplicationContext context) {
		super(context);
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Content-Type", "application/json");
		if ( !this.hasValidCSRFToken(request) ) {
			SavePrefixesCommand.respondCSRFError(response);
			return;
		}
        try {
			// Update prefixes...
			Map<String, Vocabulary> prefixesMap = new HashMap<String, Vocabulary>();
			ArrayNode aPrefixes =
				ParsingUtilities.evaluateJsonStringToArrayNode( request.getParameter("prefixes") );
			for (JsonNode prefix : aPrefixes) {
				String strPrefixName = prefix.get("name").asText();
				prefixesMap.put( strPrefixName, new Vocabulary( strPrefixName, prefix.get("iri").asText() ) );
			}
			this.getRDFTransform(request).setPrefixesMap(prefixesMap);

			// ...and the prefixes' vocabulary searcher...
			String projectID = request.getParameter("project");
			this.getContext().
				getVocabularySearcher().
					synchronize( projectID, prefixesMap.keySet() );

			SavePrefixesCommand.respondJSON(response, CodeResponse.ok);
		}
		catch (Exception ex) {
            SavePrefixesCommand.respondException(response, ex);
        }
	}
}
