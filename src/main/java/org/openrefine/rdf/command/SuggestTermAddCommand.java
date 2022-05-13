package org.openrefine.rdf.command;

import org.openrefine.rdf.RDFTransform;
import org.openrefine.rdf.model.vocab.IVocabularySearcher;
import org.openrefine.rdf.model.vocab.RDFTNode;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class SuggestTermAddCommand extends RDFTransformCommand {
    //private final static Logger logger = LoggerFactory.getLogger("RDFT:SuggTermCmd");

    public SuggestTermAddCommand() {
        super();
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ( ! this.hasValidCSRFToken(request) ) {
            SuggestTermAddCommand.respondCSRFError(response);
            return;
        }

        // Parameters names are defined in the Suggest Term (rdf-transform-suggest-term.js) JavaScript code.
        // The "project" holds the project ID of the project to search...
        String strProjectID = request.getParameter("project");
        String strType = request.getParameter("type");

        String [] astrLoader = new String[6];
        astrLoader[RDFTNode.iIRI] = request.getParameter("iri");
        astrLoader[RDFTNode.iLabel] = request.getParameter("label");
        astrLoader[RDFTNode.iDesc] = request.getParameter("desc");
        astrLoader[RDFTNode.iPrefix] = request.getParameter("prefix");
        astrLoader[RDFTNode.iNamespace] = request.getParameter("namespace");
        astrLoader[RDFTNode.iLocalPart] = request.getParameter("localPart");
        String strLongDescription = request.getParameter("description");
        if (strLongDescription != null){
            astrLoader[RDFTNode.iDesc] = strLongDescription;
        }

        RDFTNode node = new RDFTNode(astrLoader);

        if (strType != null) {
            IVocabularySearcher theSearcher = RDFTransform.getGlobalContext().getVocabularySearcher();
            try {
                theSearcher.addTerm(node, strType, strProjectID);
            }
            catch (Exception ex) {
                SuggestTermAddCommand.respondJSON(response, CodeResponse.error);
                return;
            }
        }
        SuggestTermAddCommand.respondJSON(response, CodeResponse.ok);
    }
}
