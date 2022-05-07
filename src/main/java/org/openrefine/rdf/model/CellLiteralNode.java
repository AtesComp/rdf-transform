package org.openrefine.rdf.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.io.IOException;

import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.ParsingException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerationException;

import org.apache.jena.rdf.model.RDFNode;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CellLiteralNode extends LiteralNode implements CellNode {
    static private final Logger logger = LoggerFactory.getLogger("RDFT:CellLitNode");

    static private final String strNODETYPE = "cell-as-literal";

    private final String strColumnName;

    @JsonCreator
    public CellLiteralNode(
        String strColumnName, String strExp, boolean bIsIndex,
        ConstantResourceNode nodeDatatype, String strLanguage, Util.NodeType eNodeType )
    {
        this.strColumnName    = strColumnName;
        // Prefix not required for literal nodes
        this.strExpression    = ( strExp == null ? Util.gstrCodeValue : strExp );
        this.bIsIndex = bIsIndex;
        this.nodeDatatype = nodeDatatype;
        this.strLanguage = strLanguage;
        this.eNodeType = eNodeType;
    }

    static String getNODETYPE() {
        return CellLiteralNode.strNODETYPE;
    }

    @Override
    public String getNodeName() {
        String strName = "Cell Literal: <[" +
            ( this.bIsIndex ? "Index#" : this.strColumnName ) +
            "] on [" + this.strExpression + "]>";

        // If there is a value type...
        if (this.nodeDatatype != null) {
            strName += "^^" + this.nodeDatatype.normalizeResourceAsString();
        }

        // If there is not a value type AND there is a language...
        else if (this.strLanguage != null) {
            strName += "@" + strLanguage;
        }

        return strName;
    }

    @Override
    public String getNodeType() {
        return CellLiteralNode.strNODETYPE;
    }

    @JsonProperty("columnName")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getColumnName() {
        return this.strColumnName;
    }

    @JsonProperty("isIndex")
    public boolean isIndexNode() {
        return this.bIsIndex;
    }

    @JsonProperty("expression")
    public String getExpression() {
        return this.strExpression;
    }

    /*
     *  Method createRowLiterals() creates the object list for triple statements
     *  from this node on Rows
     */
    @Override
    protected void createRowLiterals() {
        if (Util.isDebugMode()) CellLiteralNode.logger.info("DEBUG: createRowLiterals...");

        this.listNodes = null;
        Object results = null;
        try {
            results =
                Util.evaluateExpression( this.theProject, this.strExpression, this.strColumnName, this.theRec.row() );
        }
        catch (ParsingException ex) {
            // An cell might result in a ParsingException when evaluating an IRI expression.
            // Eat the exception...
            return;
        }

        // Results cannot be classed...
        if ( results == null || ExpressionUtils.isError(results) || ! ExpressionUtils.isNonBlankData(results) ) {
            return;
        }

        this.listNodes = new ArrayList<RDFNode>();

        // Results are an array...
        if ( results.getClass().isArray() ) {
            if (Util.isDebugMode()) CellLiteralNode.logger.info("DEBUG: Result is Array...");

            List<Object> listResult = Arrays.asList(results);
            for (Object objResult : listResult) {
                this.normalizeLiteral(objResult);
            }
        }
        // Results are singular...
        else {
            this.normalizeLiteral(results);
        }

        if ( this.listNodes.isEmpty() ) {
            this.listNodes = null;
        }
    }

    @Override
    public void writeNode(JsonGenerator writer)
            throws JsonGenerationException, IOException {
        // Prefix
        //  N/A

        // Source
        writer.writeObjectFieldStart(Util.gstrValueSource);
        String strType = Util.toNodeSourceString(this.eNodeType);
        writer.writeStringField(Util.gstrSource, strType);
        if ( ! ( this.bIsIndex || this.strColumnName == null ) ) {
            writer.writeStringField(Util.gstrColumnName, this.strColumnName);
        }
        writer.writeEndObject();

        // Expression
        if ( ! ( this.strExpression == null || this.strExpression.equals(Util.gstrCodeValue) ) ) {
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
            if ( ! ( this.nodeDatatype.strExpression == null || this.nodeDatatype.strExpression.equals(Util.gstrCodeValue) ) ) {
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
