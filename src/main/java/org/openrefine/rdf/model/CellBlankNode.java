package org.openrefine.rdf.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openrefine.rdf.model.expr.functions.ToIRIString;

import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.ParsingException;

import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CellBlankNode extends ResourceNode implements CellNode {
    static private final Logger logger = LoggerFactory.getLogger("RDFT:CellBlankNode");

    static private final String strNODETYPE = "cell-as-blank";

    private final String strColumnName;

    @JsonCreator
    public CellBlankNode(String strColumnName, String strExp, boolean bIsIndex, Util.NodeType eNodeType)
    {
        this.strColumnName    = strColumnName;
        // Prefix not required for blank nodes
        this.strExpression    = ( strExp == null ? Util.gstrCodeValue : strExp );
        this.bIsIndex = bIsIndex;
        this.eNodeType = eNodeType;
    }

    static String getNODETYPE() {
        return CellBlankNode.strNODETYPE;
    }

    @Override
    public String getNodeName() {
        return "Cell BNode: <[" +
            ( this.bIsIndex ? "Index#" : this.strColumnName ) +
            "] on [" + this.strExpression + "]>";
    }

    @Override
    public String getNodeType() {
        return CellBlankNode.strNODETYPE;
    }

    @JsonProperty("columnName")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getColumnName() {
        return this.strColumnName;
    }

    @JsonProperty("expression")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getExpression() {
        return this.strExpression;
    }

    @Override
    protected void createRowResources() {
        if (Util.isDebugMode()) CellBlankNode.logger.info("DEBUG: createRowResources...");

        this.listNodes = null;
        Object results = null;
        try {
            // NOTE: Currently, the expression just results in a "true" (some non-empty string is evaluated)
            //      or "false" (a null or empty string is evaluated).
            //      When "true", a BNode is automatically generated.
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
            if (Util.isDebugMode()) CellBlankNode.logger.info("DEBUG: Result is Array...");

            List<Object> listResult = Arrays.asList(results);
            for (Object objResult : listResult) {
                this.normalizeBNodeResource(objResult);
            }
        }
        // Results are singular...
        else {
            this.normalizeBNodeResource(results);
        }

        if ( this.listNodes.isEmpty() ) {
            this.listNodes = null;
        }
    }

    private void normalizeBNodeResource(Object objResult) {
        String strResult = Util.toSpaceStrippedString(objResult);
        // NOTE: The prefix "_:" is auto-added by createBNode()
        // TODO: Use strResult or just "true" or "false"?  Currently, "true" or "false".
        // If we have a good result...
        if ( ! ( strResult == null || strResult.isEmpty() ) ) {
            Resource bnode = null;
            // If this is a row / record index-based Blank Node...
            if (this.bIsIndex) {
                // ...produce a regular blank node...
                bnode = new ResourceImpl( new AnonId() );
            }
            // Otherwise, it's a column-based Blank Node...
            else {
                String strIRIColumnName = ToIRIString.toIRIString(this.strColumnName);
                // If the ColumnName does not produce a good IRI string...
                if (strIRIColumnName == null) {
                    // ...produce a regular blank node...
                    bnode = new ResourceImpl( new AnonId() );
                }
                // Otherwise, produce an Blank Node based on the ColumnName...
                else {
                    // Since we are processing by row (even in record mode for columns),
                    // the row number is set and we can use it with the ColumnName
                    String strIndex = Integer.toString( this.theRec.row() );
                    bnode = new ResourceImpl( new AnonId( strIRIColumnName + "_" + strIndex ) );
                }
            }

            if (bnode != null) {
                this.listNodes.add(bnode);
            }
        }
    }

    @Override
    protected void writeNode(JsonGenerator writer, boolean isRoot)
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

        // Value Type, Variable
        if (! isRoot) {
            writer.writeObjectFieldStart(Util.gstrValueType);
            writer.writeStringField(Util.gstrType, Util.gstrBNode);
            writer.writeEndObject();
        }
    }
}
