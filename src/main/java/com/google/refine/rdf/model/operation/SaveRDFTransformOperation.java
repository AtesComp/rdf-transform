package com.google.refine.rdf.model.operation;

import com.google.refine.rdf.RDFTransform;
import com.google.refine.rdf.model.Util;
//import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import com.google.refine.history.HistoryEntry;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SaveRDFTransformOperation extends AbstractOperation {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:SaveRDFTransOp");

    /*
        Class Variables
    */
    //@JsonProperty("description")
    @JsonIgnore
    private final static String strSaveRDFTransform = "Save RDF Transform";

    /*
        Instance Variables
    */
    //@JsonProperty(RDFTransform.KEY)
    @JsonIgnore
    private RDFTransform theTransform;

    public SaveRDFTransformOperation() {
        if ( Util.isVerbose(3) || Util.isDebugMode() ) SaveRDFTransformOperation.logger.info("Created empty save operation.");
    };

    public SaveRDFTransformOperation(RDFTransform theTransform) {
        this.theTransform = theTransform;
        if ( Util.isVerbose(3) || Util.isDebugMode() ) SaveRDFTransformOperation.logger.info("Created save operation with transform.");
    }

    @Override
    @JsonIgnore
    public String getJsonDescription() {
    	return SaveRDFTransformOperation.strSaveRDFTransform;
    }

    @JsonProperty(RDFTransform.KEY)
    public RDFTransform getTransform() {
    	return this.theTransform;
    }

    @JsonProperty(RDFTransform.KEY)
    public void setTransform(JsonNode jnodeTransform)
            throws Exception {
        if ( Util.isVerbose(3) || Util.isDebugMode() ) SaveRDFTransformOperation.logger.info("Reconstructing for Save Operation...");
        if ( jnodeTransform == null || jnodeTransform.isNull() || jnodeTransform.isEmpty() ) {
            SaveRDFTransformOperation.logger.info("No RDFTransform in JSON!");
            return;
        }
        this.theTransform = RDFTransform.reconstruct(jnodeTransform);
        if ( Util.isDebugMode() ) SaveRDFTransformOperation.logger.info("...reconstructed for Save Operation.");
    }

    @Override
    protected String getBriefDescription(Project theProject) {
        return SaveRDFTransformOperation.strSaveRDFTransform;
    }

    @Override
    protected HistoryEntry createHistoryEntry(Project theProject, long liHistoryEntryID)
            throws Exception {
        if ( Util.isDebugMode() ) SaveRDFTransformOperation.logger.info("Creating save operation's change history entry...");
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

    static public AbstractOperation reconstruct(Project theProject, JsonNode jnodeElement)
            throws Exception {
        if ( Util.isVerbose(3) || Util.isDebugMode() ) SaveRDFTransformOperation.logger.info("Reconstructing from Save Operation...");
        JsonNode jnodeTransform = jnodeElement.get(RDFTransform.KEY);
        if (jnodeTransform == null || jnodeTransform.isNull() || jnodeTransform.isEmpty() ) {
            return null;
        }
        RDFTransform theTransform = RDFTransform.reconstruct(theProject, jnodeTransform);
        return new SaveRDFTransformOperation(theTransform);
    }
}
