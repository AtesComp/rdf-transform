/*
 *  Class RDFTransformBinder
 * 
 *  The RDF Transform Expression Binder used to add a baseIRI binding
 *  property.
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

package org.openrefine.rdf.model.expr;

import java.util.Properties;

import org.openrefine.rdf.model.Util;
import org.openrefine.rdf.RDFTransform;
import com.google.refine.expr.Binder;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Class RDFBinder
 * 
 *   This class is registered by the "controller.js" in this extension.
 *   The purpose of registering this "binder" is to push an instance of this class onto the
 *   HashSet managed by the ExpressionUtils class.
 * 
 *   This "binder" is used by the ExpressionUtils createBindings() method to create and add
 *   generic "bindings" properties.  It calls this "binder"'s initializeBindings() method to
 *   add a "baseIRI" binding to the "bindings" properties.
 * 
 *   The ExpressionUtils bind() method is used to bind a specific row (Row), row index (int),
 *   column name (String), and cell (Cell) to the "bindings".  It calls this "binder"'s bind()
 *   method to perform any additional work concerning the added "baseIRI" binding.
 */
public class RDFTransformBinder implements Binder {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:RDFBinder");

    private Project theProject;
    private String strLastBoundBaseIRI;
    //private final String strBindError = "Unable to bind baseIRI.";

    public RDFTransformBinder() {
        super();
        this.strLastBoundBaseIRI = null;
    }

    @Override
    public void initializeBindings(Properties theBindings, Project theProject) {
        this.theProject = theProject;
        if ( Util.isVerbose(3) ) RDFTransformBinder.logger.info("Bind baseIRI...");
        this.strLastBoundBaseIRI = RDFTransform.getRDFTransform(this.theProject).getBaseIRIAsString();
        theBindings.put("baseIRI", this.strLastBoundBaseIRI);
    }

    @Override
    public void bind(Properties theBindings, Row theRow, int iRowIndex, String strColumnName, Cell theCell) {
        //
        // Update the baseIRI
        //
        // The baseIRI is already added by the initializeBindings() above.
        // The put() call replaces it.
    
        // Get the current baseIRI...
        String strCurrentBaseIRI = RDFTransform.getRDFTransform(this.theProject).getBaseIRIAsString();
        // If the current baseIRI is new...
        if ( ! this.strLastBoundBaseIRI.equals(strCurrentBaseIRI) ) {
            // Replace the bound baseIRI...
            theBindings.put("baseIRI", strCurrentBaseIRI);
            strLastBoundBaseIRI = strCurrentBaseIRI;
        }
    }
}
