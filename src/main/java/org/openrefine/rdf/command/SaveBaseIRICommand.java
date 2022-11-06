/*
 *  Class SaveBaseIRICommand
 *
 *  Sets the Base IRI in the current RDF Transform.  Used for base prefixed
 *  IRIs.
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

import org.openrefine.rdf.model.Util;
import org.openrefine.rdf.RDFTransform;

import com.google.refine.model.Project;

import org.apache.jena.iri.IRI;

public class SaveBaseIRICommand extends RDFTransformCommand {

    public SaveBaseIRICommand() {
        super();
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ( ! this.hasValidCSRFToken(request) ) {
            SaveBaseIRICommand.respondCSRFError(response);
            return;
        }
        try {
            Project theProject = this.getProject(request);
            String strIRI = request.getParameter("baseIRI");
            IRI baseIRI = Util.buildIRI(strIRI);
            if (baseIRI == null) {
                SaveBaseIRICommand.respondJSON(response, CodeResponse.error);
                return;
            }
            RDFTransform.getRDFTransform(theProject).setBaseIRI(baseIRI);

            theProject.getMetadata().updateModified();
        }
        catch (Exception ex) {
            SaveBaseIRICommand.respondJSON(response, CodeResponse.error);
            return;
        }

        SaveBaseIRICommand.respondJSON(response, CodeResponse.ok);
    }
}
