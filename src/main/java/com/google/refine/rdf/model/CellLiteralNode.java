package com.google.refine.rdf.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.io.IOException;

import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.ParsingException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerationException;

import org.eclipse.rdf4j.model.Value;

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
		ConstantResourceNode nodeDatatype, String strLanguage )
	{
    	this.strColumnName    = strColumnName;
		// Prefix not required for literal nodes
        this.strExpression    = ( strExp == null ? "value" : strExp );
        this.bIsIndex = bIsIndex;
        this.nodeDatatype = nodeDatatype;
        this.strLanguage = strLanguage;
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

		this.listValues = null;
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
		if ( results == null || ExpressionUtils.isError(results) ) {
			return;
		}

		this.listValues = new ArrayList<Value>();

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

        if ( this.listValues.isEmpty() ) {
			this.listValues = null;
		}
    }

	@Override
	public void writeNode(JsonGenerator writer)
			throws JsonGenerationException, IOException {
		writer.writeStringField("nodeType", CellLiteralNode.strNODETYPE);
		if (this.strColumnName != null) {
			writer.writeStringField("columnName", this.strColumnName);
		}
		if (this.strExpression != null) {
			writer.writeStringField("expression", this.strExpression);
		}
		if (this.nodeDatatype != null ) {
			writer.writeStringField("valueType", this.nodeDatatype);
		}
		else if (this.strLanguage != null) {
			writer.writeStringField("language", this.strLanguage);
		}
		if (this.bIsIndex) {
			writer.writeBooleanField("isIndex", this.bIsIndex);
		}
	}
}
