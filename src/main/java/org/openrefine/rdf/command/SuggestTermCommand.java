package org.openrefine.rdf.command;

//import com.google.refine.ProjectManager;
//import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;

import org.openrefine.rdf.RDFTransform;
//import org.openrefine.rdf.model.Util;
import org.openrefine.rdf.model.vocab.SearchResultItem;
import org.openrefine.rdf.model.vocab.Vocabulary;
import org.openrefine.rdf.model.vocab.VocabularyList;

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
        // The "prefix" holds the prefix of the query search value...
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
            if ( strType.strip().equals("class") ) {
                listSearchResults =
                    RDFTransform.getGlobalContext().
                        getVocabularySearcher().
                            searchClasses(strQueryPrefix, strProjectID);
            }
            else if ( strType.strip().equals("property") ) {
                listSearchResults =
                    RDFTransform.getGlobalContext().
                        getVocabularySearcher().
                            searchProperties(strQueryPrefix, strProjectID);
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

    private boolean isPrefixedIRI(String strIRI) {
        boolean bIsPrefixed = false;
        if (strIRI != null) {
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
            //       that contains a host component.  Also, the path component includes the '/'.
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

            int iIndex = strIRI.indexOf(":");
             // If we have a possible prefix but not a base IRI reference (where iIndex == 0)...
             // NOTE: The ':' could also be in the path
            if (iIndex > 0) {
                // Is there is a possible path...
                //    iIndex + 1 = the length of strQuery to the ':' inclusive
                //    Is there anything after...
                if (strIRI.length() > iIndex + 1) {
                    try {
                        ParsedIRI tempIRI = new ParsedIRI(strIRI);
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
                else if ( strIRI.matches("\\S+") ) { // ...contains no whitespace...
                        bIsPrefixed = true; // ...accept it
                }
            }
            // Else, we might have a possible base IRI reference (starts with ':")...
            // ...don't accept...
            /*
            else if (iIndex == 0 && strIRI.length() > 1) {
                // Create Absolute IRI with Relative IRI using Base IRI...
                try {
                    Project theProject = this.getProject(this.theRequest);
                    String strBaseIRI =
                        RDFTransform.getRDFTransform(theProject).getBaseIRI().toString();
                    ParsedIRI tempIRI = new ParsedIRI(strBaseIRI + strIRI.substring(1));
                    // It parses with the Base IRI...
                    bIsPrefixed = true; // ...accept it
                }
                catch (Exception ex) {
                    // ...continue...
                }
            }
            */
        }
        return bIsPrefixed;
    }

    private List<SearchResultItem> search(RDFTransform theTransform, String strQueryPrefix) {
        // NOTE: The Vocabulary List is a local store that only contains
        //      a Prefix and a Namespace.  We will calculate other components.
        List<SearchResultItem> result = new ArrayList<SearchResultItem>();
        String strNotImported = "From local curated namespaces--not imported.";
        VocabularyList theVocabList = theTransform.getNamespaces();

        if ( this.isPrefixedIRI(strQueryPrefix) ) {
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
