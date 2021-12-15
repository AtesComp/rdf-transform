package com.google.refine.rdf;

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

    /*
     *  Method createResource() for Resource Node types
     *
     *  Return: List<Value>
     *    Returns the Resources as generic Values since these are "object" elements in
     *    ( source, predicate, object ) triples and need to be compatible with literals.
     */
    @Override
    protected List<Value> createResources() {
        if (Util.isDebugMode()) logger.info("DEBUG: createResources...");
		List<Value> listResources = null;

        //
        // Record Mode
        //
		if ( this.theRec.isRecordMode() ) {
            // For Record Mode, a node should not represent a "Row/Record Number" cell...
            if ( ! this.isRowNumberCellNode() ) {
                listResources = this.createRecordResources();
            }
            // Otherwise, we only need to get a single "Record Index" resource for the Record group...
            else {
                this.theRec.rowNext(); // ...set index for first (or any) row in the Record
                listResources = this.createRowResources(); // ...get the one resource
                this.theRec.rowReset(); // ...reset for any other row run on the Record
            }
        }
        //
        // Row Mode
        //
        else {
            listResources = this.createRowResources();
        }

		return listResources;
    }

    @Override
    protected List<Value> createRecordResources() {
		// TODO: In general, for blank nodes, one per Record+Column is enough.  Review to limit!
        if (Util.isDebugMode()) logger.info("DEBUG: createRecordResources...");
        List<Value> listResources = new ArrayList<Value>();
		List<Value> listResourcesNew = null;
		while ( this.theRec.rowNext() ) {
			listResourcesNew = this.createRowResources();
			if (listResourcesNew != null) {
				listResources.addAll(listResourcesNew);
			}
		}
        if ( listResources.isEmpty() )
			return null;
		return listResources;
    }

	@Override
	protected List<Value> createRowResources() {
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
