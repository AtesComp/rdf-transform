package com.google.refine.rdf.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerationException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConstantLiteralNode extends LiteralNode implements ConstantNode {
    static private final Logger logger = LoggerFactory.getLogger("RDFT:ConstLitNode");

    static private final String strNODETYPE = "literal";

    private final String strConstant;

    @JsonCreator
    public ConstantLiteralNode(String strConstant, ConstantResourceNode nodeDatatype, String strLanguage )
    {
        this.strConstant = strConstant; // ..no stripping here!
        this.nodeDatatype = nodeDatatype;
        this.strLanguage = strLanguage;
    }

    static String getNODETYPE() {
        return ConstantLiteralNode.strNODETYPE;
    }

	@Override
	public String getNodeName() {
        String strName = "Constant Literal: <[" + this.strConstant +  "]";

        // If there is a datatype...
        if (this.nodeDatatype != null) {
            strName += "^^" + this.nodeDatatype.normalizeResourceAsString();
        }

        // If there is NOT a datatype BUT there is a language...
        else if (this.strLanguage != null) {
            strName += "@" + strLanguage;
        }

		return strName + ">";
	}

	@Override
	public String getNodeType() {
		return ConstantLiteralNode.strNODETYPE;
	}

    public String getConstant() {
        return this.strConstant;
    }

    @Override
	protected void createRecordLiterals() {
        // For a Constant Literal Node, we only need one constant literal per record,
        // so process as a row...
        this.createRowLiterals();
    }

    /*
     *  Method createRowLiterals() creates the object list for triple statements
     *  from this node on a Row.
     */
    @Override
	protected void createRowLiterals() {
		if (Util.isDebugMode()) ConstantLiteralNode.logger.info("DEBUG: createRowLiterals...");

		this.listValues = null;

        // If there is no value to work with...
        if ( this.strConstant == null || this.strConstant.isEmpty() ) {
            return;
        }

        this.listValues = new ArrayList<Value>();
        this.normalizeLiteral(this.strConstant);
    }

	@Override
    public void writeNode(JsonGenerator writer) throws JsonGenerationException, IOException {
        writer.writeStringField("nodeType", ConstantLiteralNode.strNODETYPE);
        writer.writeStringField("value", strConstant);
        if (nodeDatatype != null) {
            writer.writeStringField("valueType", nodeDatatype);
        }
        if (strLanguage != null) {
            writer.writeStringField("lang", strLanguage);
        }
    }
}
