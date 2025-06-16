/*
 *  Class SaveRDFTransformOperation
 *
 *  The RDF Transform Save Operation used to save the current RDF Transform as
 *  an OpenRefine history entry.
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

import java.util.Properties;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.JsonNode;

import org.openrefine.rdf.RDFTransform;
import org.openrefine.rdf.model.Util;

import com.google.refine.ProjectManager;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.process.Process;

import org.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SaveRDFTransformOperation extends AbstractOperation {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:SaveRDFTransOp");

    //
    // Class Variables
    //

    @JsonIgnore
    private final static String strSaveRDFTransform = "Save RDF Transform";

    //
    // Class Methods
    //

    /**
     * The reconstruct class method is specified by the OpenRefine API for all AbstractOperation classes.
     * @param theProject - the given OpenRefine Project instance.
     * @param jobj - the JSONObject used to reconstruct a SaveRDFTransformOperation instance presumably for
     *          history management.
     * @return SaveRDFTransformOperation cast as AbstractOperation
     * @throws Exception
     */
    static public AbstractOperation reconstruct(Project theProject, JSONObject jobj)
            throws Exception {
        if (theProject == null) {
            if ( Util.isDebugMode() ) SaveRDFTransformOperation.logger.info("DEBUG: reconstruct(): The Project is null.");
        }
        if ( Util.isVerbose(3) ) SaveRDFTransformOperation.logger.info("Reconstructing a Save Operation ({})...", theProject == null ? null : theProject.id);
        if ( Util.isDebugJSON() ) SaveRDFTransformOperation.logger.info( "DEBUG JSON: \n{}", jobj.toString() );

        JsonNode jnodeOp = null;
        try {
            jnodeOp = ParsingUtilities.evaluateJsonStringToObjectNode( jobj.toString() );
        }
        catch (Exception ex) {
            SaveRDFTransformOperation.logger.error("ERROR: Cannot convert JSONObject to JsonNode!", ex);
            if ( Util.isVerbose() ) ex.printStackTrace();
            return null;
        }

        JsonNode jnodeTransform = jnodeOp.get(RDFTransform.KEY);
        return new SaveRDFTransformOperation(theProject, jnodeTransform);
    }

    //
    // Instance Variables
    //

    //@JsonProperty(RDFTransform.KEY)
    @JsonIgnore
    private RDFTransform theTransform = null;

    /*
        Instance Methods
    */

    //@JsonIgnore
    public SaveRDFTransformOperation() {
        if ( Util.isVerbose(3) ) SaveRDFTransformOperation.logger.info("Creat Save Op by No-Arg.");
    };

    @JsonIgnore
    public SaveRDFTransformOperation(Project theProject, JsonNode jnodeTransform) {
        if ( Util.isVerbose(3) ) SaveRDFTransformOperation.logger.info("Create Save Op by Project and Transform JSON...");

        this.setTransform(theProject, jnodeTransform);
    }

    /**
     * Constructor for deserialization via Jackson
     */
    @JsonCreator
    public SaveRDFTransformOperation(
        //@JacksonInject("projectID") long iProjectID,
        @JsonProperty("op") String strOpCode,
        @JsonProperty(RDFTransform.KEY) JsonNode jnodeTransform )
    {
        if ( Util.isVerbose(3) ) SaveRDFTransformOperation.logger.info("Create Save Op by Transform JSON ({})...", strOpCode);

        //if (theTransform == null) {
        //    if ( Util.isDebugMode() ) SaveRDFTransformOperation.logger.info("DEBUG: SaveRDFTransformOperation(): Transform is empty.");
        //}
        //this.theTransform = theTransform;

        if ( Util.isDebugJSON() ) SaveRDFTransformOperation.logger.info( "  JSON:\n" + jnodeTransform.toPrettyString() );

        this.setTransform(jnodeTransform);
    }

    @JsonGetter(RDFTransform.KEY)
    public RDFTransform getTransform() {
        return this.theTransform;
    }

    @JsonIgnore
    public void setTransform(Project theProject, JsonNode jnodeTransform) {
        if (theProject == null) {
            if ( Util.isDebugMode() ) SaveRDFTransformOperation.logger.info("DEBUG: setTransform(): No Project.");
            return;
        }
        if ( Util.isVerbose(3) ) SaveRDFTransformOperation.logger.info("Reconstructing Transform from JSON...");
        if ( jnodeTransform == null || jnodeTransform.isNull() || jnodeTransform.isEmpty() ) {
            SaveRDFTransformOperation.logger.info("  No JSON Transform.");
            return;
        }
        this.theTransform = RDFTransform.reconstruct(theProject, jnodeTransform);

        if ( Util.isDebugMode() ) SaveRDFTransformOperation.logger.info("DEBUG: setTransform(): ...reconstructed Transform from JSON.");
    }

    @JsonSetter(RDFTransform.KEY)
    public void setTransform(JsonNode jnodeTransform) {
        if ( Util.isVerbose(3) ) SaveRDFTransformOperation.logger.info("Reconstructing Transform from JSON by Jackson...");
        if ( jnodeTransform == null || jnodeTransform.isNull() || jnodeTransform.isEmpty() ) {
            SaveRDFTransformOperation.logger.info("  No JSON Transform.");
            return;
        }
        this.theTransform = RDFTransform.reconstruct(jnodeTransform);

        if ( Util.isDebugMode() ) SaveRDFTransformOperation.logger.info("DEBUG: setTransform(): ...reconstructed Transform from JSON by Jackson.");
    }

    @Override
    @JsonGetter("description")
    public String getJsonDescription() {
        if ( Util.isDebugMode() ) SaveRDFTransformOperation.logger.info("DEBUG: getJsonDescription(): called.");
        return this.getBriefDescription(null);
    }

    @Override
    @JsonIgnore
    protected String getBriefDescription(Project theProject) {
        if ( Util.isDebugMode() ) SaveRDFTransformOperation.logger.info("DEBUG: getBriefDescription(): called.");

        return SaveRDFTransformOperation.strSaveRDFTransform;
    }

    @Override
    @JsonIgnore
    public Process createProcess(Project theProject, Properties options) throws Exception {
        if ( Util.isDebugMode() ) SaveRDFTransformOperation.logger.info("DEBUG: createProcess(): Setting project...");
        return super.createProcess(theProject, options);
    }

    @Override
    @JsonIgnore
    protected HistoryEntry createHistoryEntry(Project theProject, long liHistoryEntryID)
            throws Exception {
        if ( Util.isDebugMode() ) SaveRDFTransformOperation.logger.info("DEBUG: createHistoryEntry(): Creating Save Op's change history entry...");

        // Create a new Change object with the transform sent by the command...
        //   NOTE: The previous transform will be set later when the HistoryEntry
        //         causes the Change to apply()'s this transform as the current transform.
        RDFTransformChange changeHistory = new RDFTransformChange(this.theTransform, null);

        // Create a new HistoryEntry to cause the Change to apply()...
        // NOTE: This will also cause the Change to save() after the apply()
        return new HistoryEntry(liHistoryEntryID, theProject,
                                SaveRDFTransformOperation.strSaveRDFTransform,
                                this, changeHistory);
    }
}
