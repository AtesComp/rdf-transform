package com.google.refine.rdf.model;

import java.util.ArrayList;
import java.util.List;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerationException;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Value;

import com.fasterxml.jackson.annotation.JsonCreator;

public class ConstantBlankNode extends ResourceNode implements ConstantNode {

	static private final String strNODETYPE = "blank";

	private final BNode bnode;

	@JsonCreator
	public ConstantBlankNode() {
		// A Constant Blank Node is a singular blank node...
		this.bnode = this.theFactory.createBNode();
	}

    static String getNODETYPE() {
        return ConstantBlankNode.strNODETYPE;
    }

	@Override
	public String getNodeName() {
		return "<BNode>";
	}

	@Override
	public String getNodeType() {
		return ConstantBlankNode.strNODETYPE;
	}

	@Override
	protected List<Value> createResources() {
        // For a Constant Blank Node, we only need one common blank node resource per record,
        // so process as a row...
        return createRowResources();
    }

	@Override
	protected List<Value> createRecordResources() {
        // NOT USED!
        return null;
    }

	@Override
	protected List<Value> createRowResources() {
		List<Value> bnodes = new ArrayList<Value>();
		bnodes.add(bnode);
        return bnodes;
    }

	@Override
	protected void writeNode(JsonGenerator writer) throws JsonGenerationException, IOException {
		writer.writeStringField("nodeType", ConstantBlankNode.strNODETYPE);
	}
}
