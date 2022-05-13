package org.openrefine.rdf.command;

//import com.google.refine.ProjectManager;
//import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;

import org.openrefine.rdf.RDFTransform;
import org.openrefine.rdf.model.Util;
import org.openrefine.rdf.model.vocab.IVocabularySearcher;
//import org.openrefine.rdf.model.Util;
import org.openrefine.rdf.model.vocab.SearchResultItem;
import org.openrefine.rdf.model.vocab.Vocabulary;
import org.openrefine.rdf.model.vocab.VocabularyList;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonGenerator;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class SuggestTermCommand extends RDFTransformCommand {
    //private final static Logger logger = LoggerFactory.getLogger("RDFT:SuggTermCmd");

    //private HttpServletRequest theRequest = null;

    public SuggestTermCommand() {
        super();
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        //theRequest = request;

        // Parameters names are defined in the Suggest Term (rdf-transform-suggest-term.js) JavaScript code.
        // The "project" holds the project ID of the project to search...
        String strProjectID = request.getParameter("project");
        // The "type" holds the value type to search ("class" or "property")...
        String strType = request.getParameter("type");
        // The "prefix" holds the prefix query to search the values...
        String strQueryPrefix = request.getParameter("prefix");

        response.setHeader("Content-Type", "application/json");
        Writer writerBase = response.getWriter();
        JsonGenerator theWriter = ParsingUtilities.mapper.getFactory().createGenerator(writerBase);

        theWriter.writeStartObject();

        // NOTE: The Query Prefix is needed by the response processor on the client side.
        theWriter.writeStringField("prefix", strQueryPrefix);

        // Get the imported vocabulary matches...
        List<SearchResultItem> listSearchResults = null;
        if (strType != null) {
            IVocabularySearcher theSearcher = RDFTransform.getGlobalContext().getVocabularySearcher();
            if ( strType.strip().equals("class") ) {
                listSearchResults = theSearcher.searchClasses(strQueryPrefix, strProjectID);
            }
            else if ( strType.strip().equals("property") ) {
                listSearchResults = theSearcher.searchProperties(strQueryPrefix, strProjectID);
            }
        }

        // Augment with the local curated namespaces vocabulary matches...
        RDFTransform theTransform = RDFTransform.getRDFTransform( this.getProject(request) );
        List<SearchResultItem> listVocabResults = this.search(theTransform, strQueryPrefix);
        if (listSearchResults == null) {
            listSearchResults = listVocabResults;
        }
        else {
            listSearchResults.addAll(listVocabResults);
        }

        // Write the results...
        theWriter.writeFieldName("result");
        theWriter.writeStartArray();
        for (SearchResultItem result : listSearchResults) {
            result.writeAsSearchResult(theWriter);
        }
        theWriter.writeEndArray();

        theWriter.writeEndObject();

        theWriter.flush();
        theWriter.close();
        writerBase.flush();
        writerBase.close();
    }

    private List<SearchResultItem> search(RDFTransform theTransform, String strQueryPrefix) {
        // NOTE: The Vocabulary List is a local store that only contains
        //      a Prefix and a Namespace.  We will calculate other components.
        List<SearchResultItem> result = new ArrayList<SearchResultItem>();
        String strNotImported = "From local curated namespaces--not imported.";
        VocabularyList theVocabList = theTransform.getNamespaces();

        if ( Util.isPrefixedIRI(strQueryPrefix) ) {
            int iIndex = strQueryPrefix.indexOf(":");
            String strPrefix = strQueryPrefix.substring(0, iIndex);
            String strLocalPart = "";
            iIndex++; // ...advance to start of LocalPart
            if (strQueryPrefix.length() > iIndex) { // ...if there is more...
                strLocalPart = strQueryPrefix.substring(iIndex);
            }
            for ( Vocabulary vocab : theVocabList ) {
                // Do the prefixes match?
                if ( vocab.getPrefix().equals(strPrefix) ) {
                    String strVocabNamespace = vocab.getNamespace();
                    String strIRI = strVocabNamespace + strLocalPart;
                    result.
                        add(
                            new SearchResultItem(
                                strIRI,
                                strIRI,
                                strNotImported,
                                strPrefix,
                                strVocabNamespace,
                                strLocalPart
                            )
                        );
                }
            }
        }
        else { // The Query does not have a defined Prefix, so try both Prefix and Namespace...
            for ( Vocabulary vocab : theVocabList ) {
                String strVocabNamespace = vocab.getNamespace();
                String strVocabPrefix = vocab.getPrefix();

                // Does the Prefix contain the Query?
                if ( strQueryPrefix.length() <= strVocabPrefix.length() &&
                     strVocabPrefix.startsWith(strQueryPrefix) )
                {
                    result.
                        add(
                            new SearchResultItem(
                                strVocabNamespace,
                                strVocabNamespace,
                                strNotImported,
                                strVocabPrefix,
                                strVocabNamespace,
                                ""
                            )
                        );
                }
                // Does the Namespace contain the Query?
                if ( strVocabNamespace.startsWith(strQueryPrefix) ) {
                    result.
                        add(
                            new SearchResultItem(
                                strQueryPrefix,
                                strQueryPrefix,
                                strNotImported,
                                strVocabPrefix,
                                strVocabNamespace,
                                ""
                            )
                        );
                }
                // Does the Query contain the Namespace?
                else if ( strQueryPrefix.startsWith(strVocabNamespace) ) {
                    String strVocabLocalPart = "";
                    if ( strQueryPrefix.length() > strVocabNamespace.length() ) {
                        strVocabLocalPart = strQueryPrefix.substring( strVocabNamespace.length() );
                    }
                    result.
                        add(
                            new SearchResultItem(
                                strQueryPrefix,
                                strQueryPrefix,
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
