/*
 *  Class SuggestTermAddCommand
 *
 *  Add a suggestion term to the Lucene Indexer.
 *
 *  Copyright 2024 Keven L. Ates
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.openrefine.rdf.command;

import org.openrefine.rdf.RDFTransform;
import org.openrefine.rdf.model.Util;
import org.openrefine.rdf.model.vocab.IVocabularySearcher;
import org.openrefine.rdf.model.vocab.RDFTNode;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SuggestTermAddCommand extends RDFTransformCommand {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:SuggTermAddCmd");

    public SuggestTermAddCommand() {
        super();
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ( ! this.hasValidCSRFToken(request) ) {
            SuggestTermAddCommand.respondCSRFError(response);
            return;
        }
        if ( Util.isDebugMode() ) SuggestTermAddCommand.logger.info("DEBUG: Adding suggestion term...");

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
                if ( Util.isDebugMode() ) SuggestTermAddCommand.logger.error("ERROR: " + ex.getMessage(), ex);
                SuggestTermAddCommand.respondJSON(response, CodeResponse.error);
                return;
            }
        }
        SuggestTermAddCommand.respondJSON(response, CodeResponse.ok);
    }
}
