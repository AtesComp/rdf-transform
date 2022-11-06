/*
 *  Class NamespaceRemoveCommand
 *
 *  Removes a known Namespace in the current RDF Transform including removing
 *  the current ontology terms from the Lucene indexer.
 *
 *  Copyright 2022 Keven L. Ates
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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openrefine.rdf.RDFTransform;
import org.openrefine.rdf.model.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NamespaceRemoveCommand extends RDFTransformCommand {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:NSRemoveCmd");

    public NamespaceRemoveCommand() {
        super();
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ( Util.isDebugMode() ) NamespaceRemoveCommand.logger.info("Removing prefix...");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json");
        if ( ! this.hasValidCSRFToken(request) ) {
            NamespaceRemoveCommand.respondCSRFError(response);
            return;
        }
        // For Project, DO NOT USE this.getProject(request) as we only need the string...
        String strProjectID = request.getParameter(Util.gstrProject);

        String strPrefix = request.getParameter(Util.gstrPrefix);

        if ( ! this.getRDFTransform(request).removeNamespace(strPrefix) ) {
            if ( Util.isDebugMode() ) NamespaceRemoveCommand.logger.info("...failed.");
            NamespaceRemoveCommand.respondJSON(response, CodeResponse.error);
            return;
        }

        try {
            RDFTransform.getGlobalContext().
                getVocabularySearcher().
                    deleteTermsOfVocab(strPrefix, strProjectID);
        }
        catch (Exception ex) {
            if ( Util.isDebugMode() ) NamespaceRemoveCommand.logger.info("...vocabulary removal problems...");
        }

        if ( Util.isDebugMode() ) NamespaceRemoveCommand.logger.info("...removed.");
        NamespaceRemoveCommand.respondJSON(response, CodeResponse.ok);
    }
}
