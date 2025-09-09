/*
 *  Class NamespaceAddFromURLCommand
 *
 *  Adds a Namespace to the current RDF Transform.
 *
 *  Copyright 2025 Keven L. Ates
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

import org.openrefine.rdf.ApplicationContext;
import org.openrefine.rdf.RDFTransform;
import org.openrefine.rdf.model.Util;
import org.openrefine.rdf.model.vocab.Vocabulary;
import org.openrefine.rdf.model.vocab.Vocabulary.LocationType;
import org.openrefine.rdf.model.vocab.VocabularyImportException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NamespaceAddFromURLCommand extends RDFTransformCommand {
    static private final Logger logger = LoggerFactory.getLogger("RDFT:NamespaceAddFromURLCmd");

    public NamespaceAddFromURLCommand() {
        super();
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ( Util.isDebugMode() ) NamespaceAddFromURLCommand.logger.info("DEBUG: doPost(): Starting ontology import...");
        if ( ! this.hasValidCSRFToken(request) ) {
            NamespaceAddFromURLCommand.respondCSRFError(response);
            return;
        }

        ApplicationContext theContext = RDFTransform.getGlobalContext();

        // For Project, DO NOT USE this.getProject(request) as we only need the string...
        String strProjectID = request.getParameter(Util.gstrProject).strip();
        String strPrefix    = request.getParameter(Util.gstrPrefix).strip();
        String strNamespace = request.getParameter(Util.gstrNamespace).strip();
        String strLocation  = request.getParameter(Util.gstrLocation).strip();
        String strLocType   = request.getParameter(Util.gstrLocType).strip();
        if ( Util.isDebugMode() ) {
            NamespaceAddFromURLCommand.logger.info(
                "DEBUG: doPost(): Prefix:[{}] Namespace:[{}] Location:[{}] LocType:[{}]",
                strPrefix, strNamespace, strLocation, strLocType
            );
        }

        LocationType theLocType = Vocabulary.fromLocTypeString(strLocType);

        if (strLocation == null) strLocation = "";
        if ( strLocation.isEmpty() ) theLocType = LocationType.NONE;

        Exception except = null;
        boolean bError = false; // ...not fetchable
        boolean bFormatted = false;
        try {
            if ( Util.isDebugMode() ) NamespaceAddFromURLCommand.logger.info("DEBUG: doPost():   Getting project's RDF Transform...");
            RDFTransform theTransform = this.getRDFTransform(request);

            // Remove the namespace...
            if ( Util.isDebugMode() ) NamespaceAddFromURLCommand.logger.info("DEBUG: doPost():   Removing Namespace [{}]", strPrefix);
            theTransform.removeNamespace(strPrefix);

            // Remove related vocabulary...
            if ( Util.isDebugMode() ) NamespaceAddFromURLCommand.logger.info("DEBUG: doPost():   Removing relate vocabulary...");
            theContext.getVocabularySearcher().deleteVocabularyTerms(strPrefix, strProjectID);

            // For URL location types ONLY...
            if (theLocType == LocationType.URL) {
                // (Re)Add related vocabulary...
                if ( Util.isDebugMode() ) NamespaceAddFromURLCommand.logger.info("DEBUG: doPost():   Importing vocabulary from URL...");
                theContext.getVocabularySearcher().importAndIndexVocabulary(strPrefix, strNamespace, strLocation, theLocType, strProjectID);
            }

            // (Re)Add the namespace...
            if ( Util.isDebugMode() ) NamespaceAddFromURLCommand.logger.info("DEBUG: doPost():   Adding Namespace [{}]", strPrefix);
            theTransform.addNamespace(strPrefix, strNamespace, strLocation, theLocType);
        }
        catch (VocabularyImportException ex) {
            bFormatted = true;
            except = ex;
        }
        catch (Exception ex) {
            bError = true;
            except = ex;
        }

        // Some problem occurred....
        if (except != null) {
            this.processException(except, bError, bFormatted, logger);
            NamespaceAddFromURLCommand.respondJSON(response, CodeResponse.error);
            return;
        }

        // Otherwise, all good...
        NamespaceAddFromURLCommand.respondJSON(response, CodeResponse.ok);

        if (theLocType == LocationType.URL) {
            if ( Util.isDebugMode() ) NamespaceAddFromURLCommand.logger.info("DEBUG: doPost(): ...Ended ontology import.");
        }
    }
}
