package com.google.refine.rdf.command;

//import com.google.refine.ProjectManager;
//import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.rdf.RDFTransform;
import com.google.refine.rdf.model.vocab.SearchResultItem;
import com.google.refine.rdf.model.vocab.Vocabulary;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;

import org.eclipse.rdf4j.common.net.ParsedIRI;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class SuggestTermCommand extends RDFTransformCommand {
	//private final static Logger logger = LoggerFactory.getLogger("RDFT:SuggTermCmd");

	public SuggestTermCommand() {
		super();
	}

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

    	// Parameters names are defined in the Suggest Term (rdf-transform-suggest-term.js) JavaScript code.
    	// The "project" holds the project ID of the project to search...
        String strProjectID = request.getParameter("project");
    	// The "type" holds the value type to search ("class" or "property")...
        String strType = request.getParameter("type");
    	// The "query" holds the query search value...
        String strQuery = request.getParameter("query");

        response.setHeader("Content-Type", "application/json");
		Writer writerBase = response.getWriter();
        JsonGenerator theWriter = ParsingUtilities.mapper.getFactory().createGenerator(writerBase);

        theWriter.writeStartObject();
        theWriter.writeStringField("query", strQuery);

        List<SearchResultItem> listResults = null;
		if (strType != null) {
			if ( strType.strip().equals("class") ) {
				listResults =
					RDFTransform.getGlobalContext().
						getVocabularySearcher().
							searchClasses(strQuery, strProjectID);
			}
			else if ( strType.strip().equals("property") ) {
				listResults =
					RDFTransform.getGlobalContext().
						getVocabularySearcher().
							searchProperties(strQuery, strProjectID);
			}
		}

        if (listResults != null && listResults.size() == 0) {
            RDFTransform theTransform =
				RDFTransform.getRDFTransform( this.getProject(request) );
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

	/*
	 * OVERRIDE
	 * Project getProject(HttpServletRequest request)
	 * 		Overridden from Command since the ProjectID is held in a "term" parameter
	 * 		instead of the normal "project" parameter.
	 */
	/*
	@Override
	protected Project getProject(HttpServletRequest request)
			throws ServletException {
        if (request == null) {
            throw new ServletException("Parameter 'request' should not be null");
        }
        String strProjectID = request.getParameter("term");
        if (strProjectID == null || "".equals(strProjectID)) {
            throw new ServletException("Can't find type: missing Project ID parameter");
        }
        Long liProjectID;
        try {
            liProjectID = Long.parseLong(strProjectID);
        }
		catch (NumberFormatException ex) {
            throw new ServletException("Can't find project: badly formatted Project ID #", ex);
        }
        Project theProject = ProjectManager.singleton.getProject(liProjectID);
        if (theProject != null) {
            return theProject;
        }
		else {
            throw new ServletException("Failed to find Project ID #" + strProjectID + " - may be corrupt");
        }
	}
	*/

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
			 // If we have a possible prefix (the ':' could also be in the path)...
			if (iIndex > 0) {
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
				// Otherwise, we have a string like "ccc:", so treat it as a possible prefix...
				else if ( strQuery.matches("\\S+") ) { // ...contains no whitespace...
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
    		for ( Vocabulary vocab : theTransform.getPrefixes() ) {
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
    		for ( Vocabulary vocab : theTransform.getPrefixes() ) {
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
					if ( strQuery.length() > strVocabNamespace.length() ) {
						strVocabLocalPart = strQuery.substring( strVocabNamespace.length() );
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
