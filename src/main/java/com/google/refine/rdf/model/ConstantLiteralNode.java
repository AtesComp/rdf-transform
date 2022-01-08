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

public class ConstantLiteralNode extends LiteralNode implements ConstantNode {

    static private final String strNODETYPE = "literal";

    private final String strValue;

    @JsonCreator
    public ConstantLiteralNode(
    		@JsonProperty("value")     String strValue,
    		@JsonProperty("datatype")  String strDatatype,
    		@JsonProperty("language")  String strLanguage )
    {
        super(strDatatype, strLanguage);
        this.strValue = strValue; // ..no stripping here!
    }

    static String getNODETYPE() {
        return ConstantLiteralNode.strNODETYPE;
    }

	@Override
	public String getNodeName() {
        String strName = "<NULL>";

        // If there is a value to work with...
        if (this.strValue != null && this.strValue.length() > 0) {
            strName = this.strValue;

            // If there is a value type...
            if (this.strDatatype != null) {
                strName += "^^" + this.strDatatype;
            }

            // If there is not a value type AND there is a language...
            else if (this.strLanguage != null) {
                strName += "@" + strLanguage;
            }
        }

		return strName;
	}

	@Override
	public String getNodeType() {
		return ConstantLiteralNode.strNODETYPE;
	}

    @JsonProperty("value")
    public String getValue() {
        return this.strValue;
    }

    /*
     *  Method createObjects() creates the object list for triple statements
     *  from this node on Rows / Records.
     */
    @Override
	protected List<Value> createObjects(ResourceNode nodeProperty) {
        this.setObjectParameters(nodeProperty);

        // If there is no value to work with...
        if ( this.strValue == null || this.strValue.isEmpty() ) {
            return null;
        }

        Literal literal = null;

        // If there is a value type...
        if (this.strDatatype != null) {
            IRI iriValueType = null;
            try {
                iriValueType =
                    this.theFactory.createIRI(
                        this.expandPrefixedIRI(this.strDatatype)
                    );
            }
            catch (IllegalArgumentException ex) {
                // ...continue to get literal another way...
            }
            if (iriValueType != null) {
                literal = this.theFactory.createLiteral( this.strValue, iriValueType );
            }
        }

        // If there is NOT a value type BUT there is a language...
        if (literal == null && this.strLanguage != null) {
                literal = this.theFactory.createLiteral(this.strValue, strLanguage);
        }

        // If there is NOT a value type OR language...
        if (literal == null) {
            // Don't decorate the value...
            literal = this.theFactory.createLiteral(this.strValue);
        }

        // If there is still no literal...
        if (literal == null) {
            return null;
        }

        List<Value> literals = new ArrayList<Value>();
        literals.add(literal);

        return literals;
    }

	@Override
    public void writeNode(JsonGenerator writer) throws JsonGenerationException, IOException {
        writer.writeStringField("nodeType", ConstantLiteralNode.strNODETYPE);
        writer.writeStringField("value", strValue);
        if (strDatatype != null) {
            writer.writeStringField("valueType", strDatatype);
        }
        if (strLanguage != null) {
            writer.writeStringField("lang", strLanguage);
        }
    }
}
