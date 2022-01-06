package com.google.refine.rdf.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.ParsingException;

import org.eclipse.rdf4j.model.Value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CellBlankNode extends ResourceNode implements CellNode {
	private final static Logger logger = LoggerFactory.getLogger("RDFT:CellBlankNode");

	static private final String strNODETYPE = "cell-as-blank";

    private final String strColumnName;
    private final String strExpression;

    @JsonCreator
    public CellBlankNode(
    		@JsonProperty("columnName")  String strColumnName,
			@JsonProperty("expression")  String strExp,
			@JsonProperty("isIndex")     boolean bIsIndex )
	{
        this.strColumnName    = strColumnName;
        this.strExpression    = ( strExp == null ? "value" : strExp );
        this.bIsIndex = bIsIndex;
    }

    static String getNODETYPE() {
        return CellBlankNode.strNODETYPE;
    }

	@Override
	public String getNodeName() {
		return "<BNode>:" +
				( this.bIsIndex ? "<ROW#>" : this.strColumnName ) + 
				( "<" + this.strExpression + ">" );
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
		return this.strExpression.equals("value") ? null : this.strExpression;
	}

	@Override
	protected List<Value> createRowResources() {
        if (Util.isDebugMode()) logger.info("DEBUG: createRowResources...");
		Object results = null;
    	try {
    		results =
				Util.evaluateExpression( this.theProject, this.strExpression, this.strColumnName, this.theRec.row() );
		}
		catch (ParsingException ex) {
            // An cell might result in a ParsingException when evaluating an IRI expression.
            // Eat the exception...
			return null;
    	}

		// Results cannot be classed...
		if ( results == null || ExpressionUtils.isError(results) ) {
			return null;
		}

        List<Value> listResources = new ArrayList<Value>();

		// Results are an array...
		if ( results.getClass().isArray() ) {
            if (Util.isDebugMode()) logger.info("DEBUG: Result is Array...");

			List<Object> listResult = Arrays.asList(results);
			for (Object objResult : listResult) {
				this.normalizeBNodeResource(objResult, listResources);
			}
		}
		// Results are singular...
		else {
			this.normalizeBNodeResource(results, listResources);
		}

		if ( listResources.isEmpty() )
			listResources = null;
		return listResources;
    }

	private void normalizeBNodeResource(Object objResult, List<Value> listResources) {
		String strResult = objResult.toString();
		if ( ! ( strResult == null || strResult.isEmpty() ) ) {
			listResources.add( this.theFactory.createBNode() );
		}
	}

    @Override
    protected void writeNode(JsonGenerator writer) throws JsonGenerationException, IOException {
        writer.writeStringField("nodeType", CellBlankNode.strNODETYPE);
        if (this.strColumnName != null) {
        	writer.writeStringField("columnName", this.strColumnName);
        }
        if (this.strExpression != null) {
            writer.writeStringField("expression", this.strExpression);
        }
        if (this.bIsIndex) {
            writer.writeBooleanField("isIndex", this.bIsIndex);
        }
    }
}
