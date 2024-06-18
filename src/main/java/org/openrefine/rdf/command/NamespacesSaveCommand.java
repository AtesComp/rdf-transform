/*
 *  Class NamespacesSaveCommand
 *
 *  Sets a set of Namespaces in the current RDF Transform overwriting
 *  the previous set.
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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;
import org.openrefine.rdf.RDFTransform;
import org.openrefine.rdf.model.Util;
import org.openrefine.rdf.model.vocab.Vocabulary;
import org.openrefine.rdf.model.vocab.VocabularyList;
import com.google.refine.util.ParsingUtilities;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;

public class NamespacesSaveCommand extends RDFTransformCommand {

    public NamespacesSaveCommand() {
        super();
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json");
        if ( ! this.hasValidCSRFToken(request) ) {
            NamespacesSaveCommand.respondCSRFError(response);
            return;
        }
        try {
            // Update namespaces...
            VocabularyList listVocabs = new VocabularyList();
            ObjectNode theNamespaces =
                ParsingUtilities.evaluateJsonStringToObjectNode( request.getParameter(Util.gstrNamespaces) );

            Iterator< Entry < String, JsonNode > > fields = theNamespaces.fields();
            fields.forEachRemaining(prefix -> {
                String strPrefix = prefix.getKey();
                String strNamespace = prefix.getValue().asText();
                listVocabs.add( new Vocabulary( strPrefix, strNamespace ) );
            });
            this.getRDFTransform(request).setNamespaces(listVocabs);

            // ...and the namespaces' vocabulary searcher...
            String projectID = request.getParameter(Util.gstrProject);
            RDFTransform.getGlobalContext().
                getVocabularySearcher().
                    synchronize( projectID, listVocabs.getPrefixSet() );
        }
        catch (Exception ex) {
            NamespacesSaveCommand.respondJSON(response, CodeResponse.error);
            return;
        }

        NamespacesSaveCommand.respondJSON(response, CodeResponse.ok);
    }
}
