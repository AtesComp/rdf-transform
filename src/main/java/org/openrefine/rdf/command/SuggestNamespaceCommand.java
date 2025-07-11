/*
 *  Class SuggestNamespaceCommand
 *
 *  Suggest a Namespace from the user provided Prefix.
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

import org.openrefine.rdf.RDFTransform;
import org.openrefine.rdf.model.Util;

public class SuggestNamespaceCommand extends RDFTransformCommand {

    public SuggestNamespaceCommand() {
        super();
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String strPrefix = request.getParameter(Util.gstrPrefix);
        String strNamespace = RDFTransform.getGlobalContext().getNSManager().getNamespace(strPrefix);

        String strSuggestion = null;
        try {
            strSuggestion = "{ \"" + Util.gstrNamespace + "\" : \"" +  strNamespace + "\" }";

            // Send back to client...
            SuggestNamespaceCommand.respondJSON( response, new CodeResponse(strSuggestion) );
        }
        catch (Exception ex) {
            respondException(response, ex);
        }
    }

}
