package com.google.refine.rdf.model;

import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.rdf4j.model.Value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerationException;

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
        this.eNodeType = Util.NodeType.CONSTANT;
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
    public void writeNode(JsonGenerator writer)
            throws JsonGenerationException, IOException {
        // Prefix
        //  N/A

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
        if (this.nodeDatatype != null) {
            // Datatype Literal
            writer.writeStringField(Util.gstrType, Util.gstrDatatypeLiteral);

            // Datatype
            writer.writeObjectFieldStart(Util.gstrDatatype);

            // Datatype: Prefix
            String strPrefix = this.nodeDatatype.getPrefix();
            if (strPrefix != null) {
                writer.writeStringField( Util.gstrPrefix, strPrefix );
            }

            // Datatype: Source
            writer.writeObjectFieldStart(Util.gstrValueSource);
            writer.writeStringField(Util.gstrSource, Util.gstrConstant);
            writer.writeStringField( Util.gstrConstant, this.nodeDatatype.getConstant() );
            writer.writeEndObject();

            // Datatype: Expression
            if ( ! ( this.nodeDatatype.strExpression == null || this.nodeDatatype.strExpression.equals("value") ) ) {
                writer.writeObjectFieldStart(Util.gstrExpression);
                writer.writeStringField(Util.gstrLanguage, Util.gstrGREL);
                writer.writeStringField(Util.gstrCode, this.nodeDatatype.strExpression);
                writer.writeEndObject();
            }

            writer.writeEndObject();
        }
        else if (this.strLanguage != null) {
            // Language Literal
            writer.writeStringField(Util.gstrType, Util.gstrLanguageLiteral);
            // Language (2 char code)
            writer.writeStringField(Util.gstrLanguage, this.strLanguage);
        }
        else {
            writer.writeStringField(Util.gstrType, Util.gstrLiteral);
        }
        writer.writeEndObject();
    }
}
