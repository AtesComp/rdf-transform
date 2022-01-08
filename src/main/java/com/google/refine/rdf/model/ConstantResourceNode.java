package com.google.refine.rdf.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.Value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerationException;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class ConstantResourceNode extends ResourceNode implements ConstantNode {
    // private final static Logger logger = LoggerFactory.getLogger("RDFT:ConstResNode");

    static private final String strNODETYPE = "resource";

    private final String strIRI;

    @JsonCreator
    public ConstantResourceNode( @JsonProperty("value") String strIRI ) {
        // A Constant Resource Node is a singular IRI...
        this.strIRI = Util.toSpaceStrippedString(strIRI);
    }

    static String getNODETYPE() {
        return ConstantResourceNode.strNODETYPE;
    }

	@Override
	public String getNodeName() {
		return this.strIRI;
	}

	@Override
	public String getNodeType() {
		return ConstantResourceNode.strNODETYPE;
	}

    @JsonProperty("value")
    public String getIRI() {
        return this.strIRI;
    }

    @Override
    protected void createResources() {
        // For a Constant Resource Node, we only need one common resource per record,
        // so process as a row...
        this.createRowResources();
    }

	@Override
	protected void createRecordResources() {
        // NOT USED!
        this.listResources = null;
    }

	@Override
	protected void createRowResources() {
        this.listResources = new ArrayList<Value>();
        this.normalizeResource(this.strIRI);

        if ( this.listResources.isEmpty() ) {
            this.listResources = null;
        }
    }

	@Override
	protected void writeNode(JsonGenerator writer) throws JsonGenerationException, IOException {
		writer.writeStringField("nodeType", ConstantResourceNode.strNODETYPE);
        writer.writeStringField("value", strIRI);
	}
}
