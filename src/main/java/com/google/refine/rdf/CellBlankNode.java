package com.google.refine.rdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.ParsingException;
import com.google.refine.model.Record;

import org.eclipse.rdf4j.model.Value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerationException;

public class CellBlankNode extends ResourceNode implements CellNode {

	static private final String strNODETYPE = "cell-as-blank";

    private final String strColumnName;
    final boolean bIsRowNumberCell;
    private final String strExpression;

    @JsonCreator
    public CellBlankNode(
    		@JsonProperty("columnName")      String strColumnName,
			@JsonProperty("expression")      String strExp,
			@JsonProperty("isRowNumberCell") boolean bIsRowNumberCell )
	{
        this.strColumnName    = strColumnName;
        this.strExpression    = ( strExp == null ? "value" : strExp );
        this.bIsRowNumberCell = bIsRowNumberCell;
    }

    static String getNODETYPE() {
        return CellBlankNode.strNODETYPE;
    }

	@Override
	public String getNodeName() {
		return "<BNode>:" +
				( this.bIsRowNumberCell ? "<ROW#>" : this.strColumnName ) + 
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

	@JsonProperty("isRowNumberCell")
	public boolean isRowNumberCellNode() {
		return this.bIsRowNumberCell;
	}

	@JsonProperty("expression")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public String getExpression() {
		return this.strExpression.equals("value") ? null : this.strExpression;
	}

    @Override
    protected List<Value> createResources() {

		List<Value> bnodes = null;
        if (this.getRecord() != null) {
            bnodes = createRecordResources();
        }
        else {
            bnodes =
				createRowResources( this.getRowIndex() );
        }

		return bnodes;
    }

    private List<Value> createRecordResources() {
        List<Value> bnodes = new ArrayList<Value>();
		List<Value> bnodesNew = null;
        Record theRecord = this.getRecord();
		for (int iRowIndex = theRecord.fromRowIndex; iRowIndex < theRecord.toRowIndex; iRowIndex++) {
			bnodesNew = this.createRowResources(iRowIndex);
			if (bnodesNew != null) {
				bnodes.addAll(bnodesNew);
			}
		}
        if ( bnodes.isEmpty() )
			return null;
		return bnodes;
    }

	private List<Value> createRowResources(int iRowIndex) {
		Object results = null;
    	try {
    		results =
				Util.evaluateExpression(this.theProject, this.strExpression, this.strColumnName, iRowIndex);
		}
		catch (ParsingException e) {
            // An empty cell might result in an exception out of evaluating IRI expression,
            //   so it is intended to eat the exception...
			return null;
    	}

		// Results cannot be classed...
		if ( results == null || ExpressionUtils.isError(results) ) {
			return null;
		}

        List<Value> bnodes = new ArrayList<Value>();

		// Results are an array...
		if ( results.getClass().isArray() ) {
			int iSize = Arrays.asList(results).size();
			for (int iIndex = 0; iIndex < iSize; iIndex++) {
				bnodes.add( this.theFactory.createBNode() );
			}
		}
		// Results are singular...
		else {
			String strResult = results.toString();
			if (strResult != null && ! strResult.isEmpty() ) {
				bnodes.add( this.theFactory.createBNode() );
			}
		}

		if ( bnodes.isEmpty() )
			bnodes = null;
		return bnodes;
    }

    @Override
    protected void writeNode(JsonGenerator writer) throws JsonGenerationException, IOException {
        writer.writeStringField("nodeType", CellBlankNode.strNODETYPE);
        if (this.strColumnName != null) {
        	writer.writeStringField("columnName", this.strColumnName);
        }
        writer.writeStringField("expression", this.strExpression);
        writer.writeBooleanField("isRowNumberCell", this.bIsRowNumberCell);
    }
}
