/*
 *  Class ValidateIRICommand
 *
 *  Validate (test) a string as an IRI.
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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.commands.Command;

import org.openrefine.rdf.model.Util;

import org.apache.jena.iri.IRI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidateIRICommand extends Command {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:ValidIRICmd");

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ( Util.isVerbose(3) ) ValidateIRICommand.logger.info("Validating IRI...");
        // NOTE: No CSRFToken required for this command.

        try {
            String strIRI = request.getParameter("iri").strip();

            IRI theIRI = Util.buildIRI(strIRI, true);
            if (theIRI == null) {
                if ( Util.isDebugMode() ) ValidateIRICommand.logger.error("DEBUG: Validating IRI: Failure [" + strIRI + "]");
                ValidateIRICommand.respondJSON(response, CodeResponse.error);
                return;
            }
        }
        catch (Exception ex) { // ...any other exception...
            if ( Util.isDebugMode() ) ValidateIRICommand.logger.error("DEBUG: Validating IRI: Exception: " + ex.getMessage(), ex);
            ValidateIRICommand.respondException(response, ex);
            return;
        }
        if ( Util.isVerbose(3) ) ValidateIRICommand.logger.info("...IRI validated.");
        ValidateIRICommand.respondJSON(response, CodeResponse.ok);
    }
}
