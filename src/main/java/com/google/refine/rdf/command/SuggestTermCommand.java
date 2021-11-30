package com.google.refine.rdf.command;

import com.google.refine.ProjectManager;
import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.rdf.RDFTransform;
import com.google.refine.rdf.Util;
import com.google.refine.rdf.app.ApplicationContext;
import com.google.refine.rdf.vocab.SearchResultItem;
import com.google.refine.rdf.vocab.Vocabulary;
import com.google.refine.rdf.vocab.VocabularyIndexException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
//import java.util.regex.Pattern;

//import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
//import com.fasterxml.jackson.core.JsonGenerationException;

import org.eclipse.rdf4j.common.net.ParsedIRI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SuggestTermCommand extends RDFTransformCommand {
	private final static Logger logger = LoggerFactory.getLogger("RDFT:SuggTermCmd");

	//private static Pattern patternQName = Pattern.compile("^[_a-zA-Z][-._a-zA-Z0-9]*:([_a-zA-Z][-._a-zA-Z0-9]*)?");
	                                                      //"^((?=[a-z0-9-]{1,63}\.)(xn--)?[a-z0-9]+(-[a-z0-9]+)*\.)+[a-z]{2,63}?"
														  //"^(?!:\/\/)(?=.{1,255}$)((.{1,63}\.){1,127}(?![0-9]*$)[a-z0-9-]+\.?)$"
														  //"^(?!:\/\/)(?=.{1,255}$)([\p{L}\p{N}_$]{1,63}\.){1,127}[\p{L}_$][\p{L}\p{N}_$]*"

	public SuggestTermCommand(ApplicationContext context) {
		super(context);
	}

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

    	// Parameters names are defined in the Suggest Term (rdf-transform-suggest-term.js) JavaScript code.
    	// The "type" holds the project ID of the project to search...
        String strProjectID = request.getParameter("type");
    	// The "type_strict" holds the value type to search ("class" or "property")...
        String strType = request.getParameter("type_strict");
    	// The "prefix" holds the query search value...
        String strQuery = request.getParameter("prefix");

        response.setHeader("Content-Type", "application/json");
		Writer writerBase = response.getWriter();
        JsonGenerator theWriter = ParsingUtilities.mapper.getFactory().createGenerator(writerBase);

        theWriter.writeStartObject();
        theWriter.writeStringField("prefix", strQuery);

        List<SearchResultItem> listResults;
        if (strType != null && strType.strip().equals("class")) {
            listResults =
				getContext().
					getVocabularySearcher().
						searchClasses(strQuery, strProjectID);
        }
		else { // "property"
            listResults =
				getContext().
					getVocabularySearcher().
						searchProperties(strQuery, strProjectID);
        }

        if (listResults.size() == 0) {
            RDFTransform theTransform;
            try {
				theTransform =
					RDFTransform.getRDFTransform( this.getContext(), this.getProject(request) );
			}
			catch (VocabularyIndexException ex) {
				logger.error("ERROR: Could not get project's RDFTransform", ex);
				if ( Util.isVerbose(4) ) ex.printStackTrace();
				throw new ServletException(ex);
			}
			listResults = search(theTransform, strQuery);
        }

        theWriter.writeFieldName("result");
        theWriter.writeStartArray();
        for (SearchResultItem result : listResults) {
            result.writeAsSearchResult(theWriter);
        }
        theWriter.writeEndArray();

        theWriter.writeEndObject();

        theWriter.flush();
        theWriter.close();
        writerBase.flush();
        writerBase.close();
    }

	@Override
	protected Project getProject(HttpServletRequest request)
			throws ServletException {
    	String strProjectID = request.getParameter("type");
    	return ProjectManager.singleton.getProject(Long.parseLong(strProjectID));
	}

	private boolean isPrefixedQuery(String strQuery) {
		boolean bIsPrefixed = false;
		if (strQuery != null) {
			//
			// A Prefixed Qualified Name is by definition an IRI of the form:
			//    prefix:FQDN
			// where the FQDN is just a representation of a host, therefore:
			//    prefix:host
			//
			// IRIs for this context can be represented essentially 2 ways:
			//    1. schema://host/path (an IRI)
			//    2. prefix:path (a condensed IRI expression, CIRIE)
			// NOTE: For 1, the '//' after the schema always indicates an authority component
			//		 that contains a host component.  Also, the path component includes the '/'.
			// See:
			//   https://en.wikipedia.org/wiki/Internationalized_Resource_Identifier
			//   https://en.wikipedia.org/wiki/Uniform_Resource_Identifier
			//
			// NOTE: The second representation may contain a host component within the path.
			//       Without the '//', there is no definitive way for the IRI parser to know
			//       what is authority vs path.
			//
			//       Consider:
			//       1. If the path does not begin with a '/', then the content up to the first '/'
			//          can be interpreted as the host component.
			//          This also implies that there was no authority component (no '//').
			//       2. If the path does begin with a '/', it implies there is an authority component
			//          that contains a host component.
			//       Then, IRI parsing will interpret the Prefixed Qualified Name format as
			//         prefix:path
			//
			// We really don't care!  All we need to know is whether the text up to the first ':'
			// is a prefix for a CIRIE...

			int iIndex = strQuery.indexOf(":");
			if (iIndex > 0) { // ...we have a possible prefix (the ':' could also be in the path)...
				// Is there is a possible path...
				//    iIndex + 1 = the length of strQuery to the ':' inclusive
				//    Is there anything after...
				if (strQuery.length() > iIndex + 1) { 
					try {
						ParsedIRI tempIRI = new ParsedIRI(strQuery);
						// ...it parsed as an IRI...
						// If a scheme is present, but a host is not present...
						if (tempIRI.getScheme() != null && tempIRI.getHost() == null) {
							// There is no authority component:
							//    i.e., there was no "schema://...", just "schema:...", so
							//    the authority parsing that contains the host parsing was not
							//    performed.  The rest may parse as a path, query, fragment.
							// Then, the schema is a prefix and that is enough...
							bIsPrefixed = true; // ...accept it
						}
					}
					catch (Exception ex) {
						// ...continue: strQuery is NOT an IRI...yet...
					}
				}
				else { // ...we have a string "...:", so treat it as a possible prefix...
					bIsPrefixed = true; // ...accept it
				}
			}
		}
		return bIsPrefixed; 
    }

    private List<SearchResultItem> search(RDFTransform theTransform, String strQuery) {
    	List<SearchResultItem> result = new ArrayList<SearchResultItem>();
		String strNotImported = "Not in the imported vocabulary definition";

    	if ( isPrefixedQuery(strQuery) ) {
    		int iIndex = strQuery.indexOf(":");
    		String strPrefix = strQuery.substring(0, iIndex);
    		String strLocalPart = "";
			iIndex++; // ...advance to start of LocalPart
			if (strQuery.length() > iIndex) { // ...if there is more...
				strLocalPart = strQuery.substring(iIndex);
			}
    		for (Vocabulary vocab : theTransform.getPrefixesMap().values()) {
    			if ( vocab.getPrefix().equals(strPrefix) ) {
					String strNamespace = vocab.getNamespace();
					String strIRI = strNamespace + strLocalPart;
    				result.
						add(
							new SearchResultItem(
								strIRI,
								strIRI,
								strNotImported,
								strPrefix,
								strNamespace,
								strLocalPart
							)
						);
    			}
    		}
    	}
		else { // strQuery does not have a Prefix, so try Namespace...
    		for (Vocabulary vocab : theTransform.getPrefixesMap().values()) {
    			String strVocabNamespace = vocab.getNamespace();
				// Does the Namespace contain the Query...
    			if ( strVocabNamespace.startsWith(strQuery) ) {
    				result.
						add(
							new SearchResultItem(
								strQuery,
								strQuery,
								strNotImported,
								vocab.getPrefix(),
								strVocabNamespace,
								""
							)
						);
    			}
				// Does the Query contain the Namespace...
				else if ( strQuery.startsWith(strVocabNamespace) ) {
					String strVocabLocalPart = "";
					if (strQuery.length() > strVocabNamespace.length()) {
						strVocabLocalPart = strQuery.substring(strVocabNamespace.length());
					}
    				result.
						add(
							new SearchResultItem(
								strQuery,
								strQuery,
								strNotImported,
								vocab.getPrefix(),
								strVocabNamespace,
								strVocabLocalPart
							)
						);
    			}
    		}
    	}
    	return result;
    }
}

/*
class Result {

	class IdName {
		@JsonProperty("id")
		String id;
		@JsonProperty("name")
		String name;

		IdName(String i, String n) {
			id = i;
			name = n;
		}
	}

	@JsonProperty("results")
    private List<IdName> results = new ArrayList<IdName>();
    @JsonProperty("prefix")
    private String prefix;

    Result(String p) {
        this.prefix = p;
    }

	void addResult(String id, String name) {
        results.add( new IdName(id, name) );
    }
}
*/