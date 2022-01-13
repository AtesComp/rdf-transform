package com.google.refine.rdf.model;

import java.util.ArrayList;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerationException;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Value;

import com.fasterxml.jackson.annotation.JsonCreator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConstantBlankNode extends ResourceNode implements ConstantNode {
	static private final Logger logger = LoggerFactory.getLogger("RDFT:ConstBlankNode");

	static private final String strNODETYPE = "blank";
	static private final String strNotLast = "[\\.]+";
	static private final String strNotFirst = "[-\\.\\u00B7\\u0300\\u036F\\u203F\\u2040]+";

	private final BNode bnode;
	private final String strConstant;

	@JsonCreator
	public ConstantBlankNode(String strConstant) {
		// NOTE: A Constant Blank Node is a singular blank node base on the supplied constant value.
		this.strConstant = strConstant;
		this.eNodeType = Util.NodeType.CONSTANT;

		// When there is nothing to evaluate...
		if ( strConstant == null || strConstant.isEmpty() ) {
			// ...produce a generic blank node...
			ConstantBlankNode.logger.warn("WARNING: The ConstantBlankNode constant is empty! Creating generic BNode.");
			this.bnode = this.theFactory.createBNode();
			return;
		}

		//
		// Validate the supplied constant value as a BNode ID based on Turtle limits...
		//
		String strBNodeValue = Util.toSpaceStrippedString(strConstant).replaceAll("[\\p{Whitespace}", "_");
		String strBNodeValueBegin;
		do {
			strBNodeValueBegin = strBNodeValue;
			while ( strBNodeValue.startsWith(ResourceNode.strBNodePrefix) ) {
				strBNodeValue = strBNodeValue.substring(2);
			}
			// Not First...
			strBNodeValue = strBNodeValue.replaceFirst("^" + ConstantBlankNode.strNotFirst, "");
			// Not Last...
			strBNodeValue = strBNodeValue.replaceFirst(ConstantBlankNode.strNotLast + "$", "");
			// On no change, break...
			if ( strBNodeValueBegin.equals(strBNodeValue) )
				break;
			// Otherwise, something was removed so recheck...
		} while (true);

		// When there is nothing to evaluate...
		if ( strConstant == null || strConstant.isEmpty() ) {
			ConstantBlankNode.logger.error("ERROR: The ConstantBlankNode constant evaluates to nothing! Creating generic BNode.");
			this.bnode = this.theFactory.createBNode();
			return;
		}

		this.bnode = this.theFactory.createBNode(ResourceNode.strBNodePrefix + strBNodeValue);
	}

    static String getNODETYPE() {
        return ConstantBlankNode.strNODETYPE;
    }

	@Override
	public String getNodeName() {
		return "Constant BNode: <[" + this.strConstant + "]" + this.bnode.getID() + ">";
	}

	@Override
	public String getNodeType() {
		return ConstantBlankNode.strNODETYPE;
	}

	@Override
	protected void createResources() {
        // For a Constant Blank Node, we only need one constant blank node resource per record,
        // so process as a row...
        this.createRowResources();
    }

	@Override
	protected void createRecordResources() {
        // NOT USED!
        this.listValues = null;
    }

	@Override
	protected void createRowResources() {
		if (Util.isDebugMode()) ConstantBlankNode.logger.info("DEBUG: createRowResources...");

		this.listValues = new ArrayList<Value>();
		this.listValues.add(bnode);
    }

	@Override
	protected void writeNode(JsonGenerator writer) throws JsonGenerationException, IOException {
		// Prefix
		//	N/A

		// Source
        writer.writeObjectFieldStart(Util.gstrValueSource);
		writer.writeStringField(Util.gstrSource, Util.gstrConstant);
        writer.writeStringField(Util.gstrConstant, this.strConstant);
		writer.writeEndObject();

		// Expression
        if ( ! ( this.strExpression == null || this.strExpression.equals("value") ) ) {
			writer.writeObjectFieldStart(Util.gstrExpression);
			writer.writeStringField(Util.gstrLanguage, Util.gstrGREL);
            writer.writeStringField(Util.gstrCode, this.strExpression);
			writer.writeEndObject();
        }

		// Value Type
        writer.writeObjectFieldStart(Util.gstrValueType);
		writer.writeStringField(Util.gstrType, Util.gstrBNode);
		writer.writeEndObject();
	}
}
