/*
 *  Class SaveRDFTransformCommand
 *
 *  Saves the current RDF Transform as the last entry in the OpenRefine
 *  project history.
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
import java.util.Properties;

import org.openrefine.rdf.RDFTransform;
import org.openrefine.rdf.model.Util;
import org.openrefine.rdf.model.operation.SaveRDFTransformOperation;
import com.google.refine.model.Project;
import com.google.refine.process.Process;
import com.google.refine.util.ParsingUtilities;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SaveRDFTransformCommand extends RDFTransformCommand {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:SaveRDFTransCmd");

    public SaveRDFTransformCommand() {
        super();
        if ( Util.isDebugMode() ) SaveRDFTransformCommand.logger.info("DEBUG: SaveRDFTransformCommand(): Created.");
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ( Util.isDebugMode() ) SaveRDFTransformCommand.logger.info("DEBUG: doPost(): Reconstructing Transform for Save...");
        if ( ! this.hasValidCSRFToken(request) ) {
            if ( Util.isDebugMode() ) SaveRDFTransformCommand.logger.info("DEBUG: doPost(): No CSRF Token.");
            SaveRDFTransformCommand.respondCSRFError(response);
            return;
        }

        try {
            // Get the project...
            Project theProject = this.getProject(request);

            // Get the RDF Transform...
            String strTransform = request.getParameter(RDFTransform.KEY);
            if (strTransform == null) {
                SaveRDFTransformCommand.logger.error("ERROR: Missing RDF Transform ({}) parameter!", RDFTransform.KEY);
                SaveRDFTransformCommand.respondJSON(response, CodeResponse.error);
                return;
            }
            JsonNode jnodeTransform = ParsingUtilities.evaluateJsonStringToObjectNode(strTransform);
            if ( jnodeTransform == null || jnodeTransform.isNull() || jnodeTransform.isEmpty() ) {
                SaveRDFTransformCommand.logger.error("ERROR: Empty RDF Transform ({}) value ({})!", RDFTransform.KEY, strTransform);
                SaveRDFTransformCommand.respondJSON(response, CodeResponse.error);
                return;
            }

            // Process the "save" operations...
            // NOTE: The RDFTransform JSON should be complete, but use the Project anyway...
            SaveRDFTransformOperation opSave = new SaveRDFTransformOperation(theProject, jnodeTransform);
            Process procSave = opSave.createProcess( theProject, new Properties() );
            SaveRDFTransformCommand.performProcessAndRespond(request, response, theProject, procSave);
        }
        catch (Exception ex) {
            SaveRDFTransformCommand.logger.error("ERROR: Save RDF Transform ({}) failed!", RDFTransform.KEY);
            SaveRDFTransformCommand.respondJSON(response, CodeResponse.error);
        }
    }
}
