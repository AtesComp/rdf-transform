package com.google.refine.rdf.operation;

import com.google.refine.rdf.RDFTransform;
import com.google.refine.rdf.Util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportTemplateOperation extends AbstractOperation {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:ExpTemplateOp");

    /*
        Class Variables
    */
    @JsonProperty("description")
    private final static String strExportTemplate = "Export RDF Template";

    /*
        Instance Variables
    */
    @JsonProperty(RDFTransform.KEY)
    private RDFTransform theTransform;

    @JsonCreator
    public ExportTemplateOperation( @JsonProperty(RDFTransform.KEY) RDFTransform theTransform ) {
        this.theTransform = theTransform;
        if ( Util.isVerbose(3) ) logger.info("Created");
    }

    @JsonProperty("description")
    public String getDescription() {
    	return ExportTemplateOperation.strExportTemplate;
    }

    @JsonProperty(RDFTransform.KEY)
    public RDFTransform getTransform() {
    	return this.theTransform;
    }

    @JsonProperty(RDFTransform.KEY)
    public void setTransform(JsonNode jnodeTransform)
            throws Exception {
        if ( Util.isVerbose(3) ) logger.info("Setting transform from JSON...");
        if (jnodeTransform == null) {
            logger.error("ERROR: No RDFTransform in JSON!");
            return;
        }
        this.theTransform = RDFTransform.reconstruct(jnodeTransform);
    }

    @Override
    protected String getBriefDescription(Project theProject) {
        return ExportTemplateOperation.strExportTemplate;
    }

    static public AbstractOperation reconstruct(Project theProject, JsonNode jnodeElement)
            throws Exception {
        if ( Util.isVerbose(3) ) logger.info("Reconstructing transform...");
        JsonNode jnodeTransform = jnodeElement.get(RDFTransform.KEY);
        if (jnodeTransform == null) {
            return null;
        }
        RDFTransform theTransform = RDFTransform.reconstruct(jnodeTransform);
        return new ExportTemplateOperation(theTransform);
    }
}
