package com.google.refine.rdf.command;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.refine.rdf.RDFTransform;
import com.google.refine.rdf.model.Util;
import com.google.refine.rdf.model.vocab.Vocabulary;
import com.google.refine.rdf.model.vocab.VocabularyList;
import com.google.refine.util.ParsingUtilities;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;

public class PrefixesSaveCommand extends RDFTransformCommand {

	public PrefixesSaveCommand() {
		super();
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Content-Type", "application/json");
		if ( ! this.hasValidCSRFToken(request) ) {
			PrefixesSaveCommand.respondCSRFError(response);
			return;
		}
        try {
			// Update prefixes...
			VocabularyList listVocabs = new VocabularyList();
			ObjectNode thePrefixes =
				ParsingUtilities.evaluateJsonStringToObjectNode( request.getParameter(Util.gstrNamespaces) );

			Iterator< Entry < String, JsonNode > > fields = thePrefixes.fields();
			fields.forEachRemaining(prefix -> {
				String strPrefix = prefix.getKey();
				String strNamespace = prefix.getValue().asText();
				listVocabs.add( new Vocabulary( strPrefix, strNamespace ) );
			});
			this.getRDFTransform(request).setPrefixes(listVocabs);

			// ...and the prefixes' vocabulary searcher...
			String projectID = request.getParameter(Util.gstrProject);
			RDFTransform.getGlobalContext().
				getVocabularySearcher().
					synchronize( projectID, listVocabs.getPrefixSet() );
		}
		catch (Exception ex) {
            PrefixesSaveCommand.respondJSON(response, CodeResponse.error);
			return;
        }

		PrefixesSaveCommand.respondJSON(response, CodeResponse.ok);
	}
}
