package com.google.refine.rdf.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.ParsingException;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Value;

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
	//private final BNode bnode;

    @JsonCreator
    public CellBlankNode(String strColumnName, String strExp, boolean bIsIndex, Util.NodeType eNodeType)
	{
        this.strColumnName    = strColumnName;
		// Prefix not required for blank nodes
        this.strExpression    = ( strExp == null ? "value" : strExp );
        this.bIsIndex = bIsIndex;
		this.eNodeType = eNodeType;
		//this.bnode = this.theFactory.createBNode();
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
		// TODO: Add prefix "\"_:\" + " here or elsewhere in the class?
		//      ( see createRowResources() and normalizeBNodeResource() )
		return this.strExpression.equals("value") ? null : this.strExpression;
	}

	@Override
	protected void createRowResources() {
        if (Util.isDebugMode()) logger.info("DEBUG: createRowResources...");

		this.listValues = null;
		Object results = null;
    	try {
			// NOTE: Currently, the expression just results in a "true" (some non-empty string is evaluated)
			//		or "false" (a null or empty string is evaluated).
			//		When "true", a BNode is automatically generated.
			results =
				Util.evaluateExpression( this.theProject, this.getExpression(), this.strColumnName, this.theRec.row() );
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
            if (Util.isDebugMode()) logger.info("DEBUG: Result is Array...");

			List<Object> listResult = Arrays.asList(results);
			for (Object objResult : listResult) {
				this.normalizeBNodeResource(objResult);
			}
		}
		// Results are singular...
		else {
			this.normalizeBNodeResource(results);
		}

		if ( this.listValues.isEmpty() ) {
			this.listValues = null;
		}
    }

	private void normalizeBNodeResource(Object objResult) {
		// TODO: Add prefix "\"_:\" + " and use it or just "true" or "false"?
		String strResult = objResult.toString();
		if ( ! ( strResult == null || strResult.isEmpty() ) ) {
			BNode bnode = this.theFactory.createBNode();
			this.listValues.add(bnode);
		}
	}

    @Override
    protected void writeNode(JsonGenerator writer) throws JsonGenerationException, IOException {
		// Prefix
		//	N/A

		// Source
        writer.writeObjectFieldStart(Util.gstrValueSource);
		String strType = Util.toNodeSourceString(this.eNodeType);
		writer.writeStringField(Util.gstrSource, strType);
        if ( ! ( this.bIsIndex || this.strColumnName == null ) ) {
        	writer.writeStringField(Util.gstrColumnName, this.strColumnName);
        }
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
		writer.writeStringField(Util.gstrType, Util.gstrValueBNode);
		writer.writeEndObject();
    }
}
