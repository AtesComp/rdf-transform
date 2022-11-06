/*
 *  Class PreferencesCommand
 *
 *  Gets the RDF Transform global preferences from the server.
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

import com.google.refine.commands.Command;
import org.openrefine.rdf.model.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreferencesCommand extends Command {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:PrefCmd");

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ( Util.isVerbose(3) ) PreferencesCommand.logger.info("Setting and Getting preferences...");
        // NOTE: No CSRFToken required for this command.

        String strPreferences = null;
        try {
            // // Set Preview Stream if given...
            // String strPreviewStream = request.getParameter("bPreviewStream");
            // if (strPreviewStream != null) {
            //     Util.setPreviewStream( Boolean.parseBoolean(strPreviewStream) );
            // }

            // // Set Sample Limit if given...
            // String strSampleLimit = request.getParameter("iSampleLimit");
            // if (strSampleLimit != null) {
            //     Util.setSampleLimit( Integer.parseInt(strSampleLimit) );
            // }

            // Get Preferences...
            strPreferences =
                "{ \"iVerbosity\" : " +     Util.getVerbose() + ", " +
                  "\"iExportLimit\" : " +   Util.getExportLimit() + ", " +
                  "\"bPreviewStream\" : " + Util.isPreviewStream() + ", " +
                  "\"bDebugMode\" : " +     Util.isDebugMode() + ", " +
                  "\"bDebugJSON\" : " +     Util.isDebugJSON() + ", " +
                  "\"iSampleLimit\" : " +   Util.getSampleLimit() + "" +
                  " }";
        }
        catch (Exception ex) { // ...any other exception...
            if ( Util.isDebugMode() ) PreferencesCommand.logger.error("DEBUG: Preferences: Exception: " + ex.getMessage(), ex);
            PreferencesCommand.respondException(response, ex);
            return;
        }
        if ( Util.isVerbose(3) ) PreferencesCommand.logger.info("...got preferences.");
        PreferencesCommand.respondJSON( response, new CodeResponse(strPreferences) );
    }
}
