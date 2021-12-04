package com.google.refine.rdf.command;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.rdf.app.ApplicationContext;
import com.google.refine.rdf.Util;
import com.google.refine.rdf.vocab.PrefixExistException;
import com.google.refine.rdf.vocab.Vocabulary;
import com.google.refine.rdf.vocab.VocabularyImportException;
import com.google.refine.rdf.vocab.VocabularyIndexException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetDefaultPrefixesCommand extends RDFTransformCommand {
	private final static Logger logger = LoggerFactory.getLogger("RDFT:GetDefPrefCmd");

	public GetDefaultPrefixesCommand(ApplicationContext context) {
		super(context);
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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
		Collection<Vocabulary> collVocab = this.getRDFTransform(request).getPrefixes();
		for (Vocabulary vocab : collVocab) {
			Exception except = null;
			boolean bError = false;
			String strError = null;
			try {
				this.getContext().
					getVocabularySearcher().
						importAndIndexVocabulary(
							vocab.getPrefix(), vocab.getNamespace(), vocab.getNamespace(), projectId);
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
				// A Default Prefix vocabulary is not defined properly...
				//   Ignore the exception, but log it...
				if (bError) {// ...error...
					logger.error("ERROR: " + strError + " vocabulary: ", except);
					if ( Util.isVerbose() ) except.printStackTrace();
				}
				else { // ...warning...
					if ( Util.isVerbose() ) logger.warn("Prefix exists: ", except);
				}
				// ...continue processing the other vocabularies...
			}
		}
		GetDefaultPrefixesCommand.respondJSON(response, new PrefixesList(collVocab));
	}

	public class PrefixesList {

		@JsonProperty("prefixes")
		public Collection<Vocabulary> prefixes;

		@JsonCreator
		public PrefixesList(
				//@JsonProperty("prefixes")
				Collection<Vocabulary> prefixes) {
			this.prefixes = prefixes;
		}

		public Map<String, Vocabulary> getMap() {
			return prefixes.stream().collect( Collectors.toMap( Vocabulary::getPrefix, Function.identity() ) );
		}
	}
}
