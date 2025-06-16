/*
 *  Class RDFTransformChange
 *
 *  The RDF Transform Change class used to manage OpenRefine history entry
 *  changes.
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

package org.openrefine.rdf.model.operation;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.Properties;

import org.openrefine.rdf.RDFTransform;
import org.openrefine.rdf.model.Util;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.refine.history.Change;
import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.Pool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RDFTransformChange implements Change {
    //
    // Class Variables
    //

    private final static Logger logger = LoggerFactory.getLogger("RDFT:RDFTransChange");
    private final static String strNew = "new";
    private final static String strOld = "old";

    //
    // Class Methods
    //

    /**
     * Load a Change whenever a HistoryEntry does not have its Change in memory.<br />
     * <br />
     *  TODO: OpenRefine's effing Change load() functions do not pass the Project
     *      unlike just about every other function! The Project is needed to properly
     *      reconstruct the Change's related RDFTransform.
     * @param theReader - a LineNumberReader instance
     * @param thePool - a Pool instance
     * @return a Change instance
     * @throws Exception
     */
    static public Change load(LineNumberReader theReader, Pool thePool)
            throws Exception {
        if ( Util.isDebugMode() ) RDFTransformChange.logger.info("DEBUG: load(): Reconstructing...");
        RDFTransform transformPrevious = null;
        RDFTransform transformCurrent = null;

        String strLine;
        while ( ( ( strLine = theReader.readLine() ) != null ) && ! ( "/ec/".equals(strLine) ) ) {
            int iEqualIndex = strLine.indexOf('=');
            String strField = strLine.substring(0, iEqualIndex);
            String strValue = strLine.substring(iEqualIndex + 1);
            JsonNode jnodeTransform = ParsingUtilities.evaluateJsonStringToObjectNode(strValue);
            if ( jnodeTransform == null || jnodeTransform.isNull() || jnodeTransform.isEmpty() ) {
                if ( Util.isDebugMode() ) RDFTransformChange.logger.info("  No Transform.");
                continue;
            }
            if ( Util.isDebugMode() ) RDFTransformChange.logger.info("  Found Transform...");
            // TODO: Change reconstruct() to use the Project when available.
            RDFTransform theTransform = RDFTransform.reconstruct(jnodeTransform);

            if ( strField.equals(RDFTransformChange.strNew) && ! strValue.isEmpty() ) {
                if ( Util.isDebugMode() ) RDFTransformChange.logger.info("DEBUG: NEW Change set.");
                transformCurrent = theTransform;
            }
            else if ( strField.equals(RDFTransformChange.strOld) && ! strValue.isEmpty() ) {
                if ( Util.isDebugMode() ) RDFTransformChange.logger.info("DEBUG: OLD Change set.");
                transformPrevious = theTransform;
            }
        }

        RDFTransformChange theChange = new RDFTransformChange(transformCurrent, transformPrevious);
        return theChange;
    }

    //
    // Instance Variables
    //

    final private RDFTransform theCurrentTransform;
    private RDFTransform thePreviousTransform;

    //
    // Instance Methods
    //

    public RDFTransformChange(RDFTransform theCurrentTransform, RDFTransform thePriorTransform) {
        this.theCurrentTransform = theCurrentTransform;
        this.thePreviousTransform = thePriorTransform;
        if ( Util.isDebugMode() ) RDFTransformChange.logger.info("DEBUG: apply(): Called.");
    }

    /**
     * Apply the lastest HistoryEntry.
     * @param theProject - the Project instance to which the Change applies.
     */
    public void apply(Project theProject) {
        if ( Util.isDebugMode() ) RDFTransformChange.logger.info("DEBUG: RDFTransformChange(): Created.");
        synchronized (theProject) {
            // Store the currently saved transform as the previous transform...
            this.thePreviousTransform = (RDFTransform) theProject.overlayModels.get(RDFTransform.EXTENSION);
            // Replace the saved transform with the current transform...
            theProject.overlayModels.put(RDFTransform.EXTENSION, this.theCurrentTransform);
        }
    }

    /**
     * Revert from the last HistoryEntry. Used by undo and failed save()s after an apply().
     * @param theProject - the Project instance to which the Change applies.
     */
    public void revert(Project theProject) {
        if ( Util.isDebugMode() ) RDFTransformChange.logger.info("DEBUG: revert(): Called.");
        synchronized (theProject) {
            // If the transform is NEW (no previous), remove the saved transform (reset to no transform)...
            if (this.thePreviousTransform == null) {
                theProject.overlayModels.remove(RDFTransform.EXTENSION);
            }
            // Otherwise, replace the saved transform with the previous transform...
            else {
                theProject.overlayModels.put(RDFTransform.EXTENSION, this.thePreviousTransform);
            }
        }
    }

    /**
     * Save the Change. Used by autosave and after an apply().
     * @param theWriter - the Writer instance used to store the Change.
     * @param theOptions - the Properties instance holding the Change's options.
     */
    public void save(Writer theWriter, Properties theOptions) throws IOException {
        if ( Util.isDebugMode() ) RDFTransformChange.logger.info("DEBUG: save(): Called.");
        try {
            JsonGenerator jsonWriter = ParsingUtilities.mapper.getFactory().createGenerator(theWriter);

            theWriter.write(RDFTransformChange.strNew + "=");
            if (this.theCurrentTransform != null) {
                this.theCurrentTransform.write(jsonWriter);
            }
            theWriter.write('\n');
            theWriter.write(RDFTransformChange.strOld + "=");
            if (this.thePreviousTransform != null) {
                this.thePreviousTransform.write(jsonWriter);
            }
            theWriter.write('\n');
            theWriter.write("/ec/\n"); // ...end of change marker

            jsonWriter.close();
        }
        catch (IOException ex) {
            RDFTransformChange.logger.error("ERROR: Writing change: ", ex);
            if ( Util.isVerbose() ) ex.printStackTrace();
        }
    }
}
