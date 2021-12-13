package com.google.refine.rdf;

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

public class CellResourceNode extends ResourceNode implements CellNode {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:CellResNode");

	static private final String strNODETYPE = "cell-as-resource";

    private final String strColumnName;
    private final String strExpression;
    private final boolean bIsRowNumberCell;

    @JsonCreator
    public CellResourceNode(
    		@JsonProperty("columnName")      String strColumnName,
    		@JsonProperty("expression")      String strExp,
    		@JsonProperty("isRowNumberCell") boolean bIsRowNumberCell )
    {
    	this.strColumnName = strColumnName;
        this.strExpression = strExp;
        this.bIsRowNumberCell = bIsRowNumberCell;
    }

    static String getNODETYPE() {
        return CellResourceNode.strNODETYPE;
    }

	@Override
	public String getNodeName() {
		return ( bIsRowNumberCell ? "<ROW#>" : this.strColumnName ) + 
                ( "<" + this.strExpression + ">" );
	}

	@Override
	public String getNodeType() {
		return CellResourceNode.strNODETYPE;
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
    public String getExpression() {
        return this.strExpression;
    }

    @Override
    protected List<Value> createResources() {
		List<Value> listResources = null;
        if ( this.theRec.isRecordMode() ) {
            listResources = createRecordResources();
        }
        else {
            listResources = createRowResources();
        }

		return listResources;
    }

    private List<Value> createRecordResources() {
        List<Value> listResources = new ArrayList<Value>();
		List<Value> listResourcesNew = null;
		for (int iRowIndex = this.theRec.rowStart(); iRowIndex < this.theRec.rowEnd(); iRowIndex++) {
			listResourcesNew = this.createRowResources();
			if (listResourcesNew != null) {
				listResources.addAll(listResourcesNew);
			}
		}
        if ( listResources.isEmpty() )
			return null;
		return listResources;
    }

    private List<Value> createRowResources() {
        Object results = null;
        try {
        	results =
                Util.evaluateExpression( this.theProject, this.strExpression, this.strColumnName, this.theRec.row() );
        }
        catch (ParsingException ex) {
            // An cell might result in a ParsingException when evaluating an IRI expression.
            // Report the error and return...
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
                this.normalizeResource(objResult, listResources);
            }
        }
        // Results are singular...
        else {
            this.normalizeResource(results, listResources);
        }

        if ( listResources.isEmpty() )
            listResources = null;
        return listResources;
    }

	@Override
	protected void writeNode(JsonGenerator writer) throws JsonGenerationException, IOException {
		writer.writeStringField("nodeType", CellResourceNode.strNODETYPE);
        if (strColumnName != null) {
        	writer.writeStringField("columnName", this.strColumnName);
        }
        writer.writeStringField("expression", this.strExpression);
        writer.writeBooleanField("isRowNumberCell", this.bIsRowNumberCell);
	}
}
