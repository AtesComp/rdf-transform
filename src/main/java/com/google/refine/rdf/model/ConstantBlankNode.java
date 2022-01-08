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
	static private final String strBNodePrefix = "_:";
	static private final String strNotFirstLast = "[\\.]+";
	static private final String strNotFirst = "^[-\u00B7\u0300\u036F\u203F\u2040]+";

	private final BNode bnode;
	private final String strConstant;

	@JsonCreator
	public ConstantBlankNode(String strConstant) {
		// NOTE: A Constant Blank Node is a singular blank node base on the supplied constant value.
		this.strConstant = strConstant;

		//
		// Validate the supplied constant value as a BNode ID based on Turtle limits...
		//
		String strBNodeValue = Util.toSpaceStrippedString(strConstant).replaceAll("[\\p{Whitespace}", "_");
		while ( strBNodeValue.startsWith(ConstantBlankNode.strBNodePrefix) ) {
			strBNodeValue = strBNodeValue.substring(2);
		}

		// Not First or Last...
		strBNodeValue = strBNodeValue.replaceFirst("^" + ConstantBlankNode.strNotFirstLast, "");
		strBNodeValue = strBNodeValue.replaceFirst(ConstantBlankNode.strNotFirstLast + "$", "");

		// Not First...
		strBNodeValue = strBNodeValue.replaceFirst(ConstantBlankNode.strNotFirst, "");

		this.bnode = this.theFactory.createBNode(ConstantBlankNode.strBNodePrefix + strBNodeValue);
	}

    static String getNODETYPE() {
        return ConstantBlankNode.strNODETYPE;
    }

	@Override
	public String getNodeName() {
		return "<BNode>:" + "<" + this.strConstant + ">" + this.bnode.getID();
	}

	@Override
	public String getNodeType() {
		return ConstantBlankNode.strNODETYPE;
	}

	@Override
	protected void createResources() {
        // For a Constant Blank Node, we only need one common blank node resource per record,
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
		this.listResources.add(bnode);
    }

	@Override
	protected void writeNode(JsonGenerator writer) throws JsonGenerationException, IOException {
		writer.writeStringField("nodeType", ConstantBlankNode.strNODETYPE);
	}
}
