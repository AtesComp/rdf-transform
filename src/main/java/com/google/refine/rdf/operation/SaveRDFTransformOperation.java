package com.google.refine.rdf.operation;

import com.google.refine.rdf.RDFTransform;
import com.google.refine.rdf.Util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import com.google.refine.history.Change;
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
    @JsonProperty("description")
    private final static String strSaveRDFTransform = "Save RDF Transform";

    /*
        Instance Variables
    */
    @JsonProperty(RDFTransform.KEY)
    private RDFTransform theTransform;

    @JsonCreator
    public SaveRDFTransformOperation( @JsonProperty(RDFTransform.KEY) RDFTransform theTransform ) {
        this.theTransform = theTransform;
        if ( Util.isVerbose(3) )
            logger.info("Created op");
    }

    static public AbstractOperation reconstruct(JsonNode jnodeElement)
            throws Exception {
        if ( Util.isVerbose(3) )
            logger.info("Reconstructing transform...");
        JsonNode jnodeTransform = jnodeElement.get(RDFTransform.KEY);
        if (jnodeTransform == null) {
            return null;
        }
        RDFTransform theTransform = RDFTransform.reconstruct( jnodeTransform );
        return new SaveRDFTransformOperation( theTransform );
    }

    @JsonProperty("description")
    public String getDescription() {
    	return SaveRDFTransformOperation.strSaveRDFTransform;
    }

    @JsonProperty(RDFTransform.KEY)
    public RDFTransform getTransform() {
    	return this.theTransform;
    }

    @JsonProperty(RDFTransform.KEY)
    public void setTransform(JsonNode jnodeTransform)
            throws Exception {
        if ( Util.isVerbose(3) )
            logger.info("Setting transform from JSON...");
        if (jnodeTransform == null) {
            return;
        }
        this.theTransform = RDFTransform.reconstruct( jnodeTransform );
    }

    @Override
    protected String getBriefDescription(Project theProject) {
        return SaveRDFTransformOperation.strSaveRDFTransform;
    }

    @Override
    protected HistoryEntry createHistoryEntry(Project theProject, long liHistoryEntryID)
            throws Exception {
        Change changeHistory = new RDFTransformChange(this.theTransform);

        return new HistoryEntry(liHistoryEntryID, theProject,
                                SaveRDFTransformOperation.strSaveRDFTransform,
                                this, changeHistory);
    }
}
