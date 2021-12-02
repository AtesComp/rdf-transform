package com.google.refine.rdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.refine.expr.EvalError;
import com.google.refine.model.Project;
import com.google.refine.model.Record;

import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerationException;

public class CellBlankNode extends ResourceNode {

	final static private String NODETYPE = "cell-as-blank";

    final private String strColumnName;
    final boolean bIsRowNumberCell;
    final private String strExpression;

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

	@Override
	public String getNodeName() {
		return "<BNode>:" +
				( bIsRowNumberCell ? "<ROW#>" : this.strColumnName ) + 
				( "<" + this.strExpression + ">" );
	}

	@Override
	public String getNodeType() {
		return CellBlankNode.NODETYPE;
	}

	@JsonProperty("columnName")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public String getColumnName() {
		return strColumnName;
	}

	@JsonProperty("expression")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public String getExpression() {
		return "value".equals(strExpression) ? null : strExpression;
	}

	@JsonProperty("isRowNumberCell")
	public boolean isRowNumberCellNode() {
		return bIsRowNumberCell;
	}

    @Override
    protected List<Value> createResources(ParsedIRI baseIRI, ValueFactory factory,
											RepositoryConnection connection, Project project ) {
		this.baseIRI = baseIRI;
		this.theFactory = factory;
		this.theConnection = connection;
		this.theProject = project;

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
			bnodesNew =
				this.createRowResources(iRowIndex);
			if (bnodesNew != null) {
				bnodes.addAll(bnodesNew);
			}
		}
        if (bnodes.size() == 0)
			return null;
		return bnodes;
    }

	private List<Value> createRowResources(int iRowIndex) {
        List<Value> bnodes = null;
    	try {
    		Object results =
				Util.evaluateExpression(this.theProject, strExpression, strColumnName, iRowIndex);

            // Results cannot be classed...
			if (results.getClass() == EvalError.class) {
    			bnodes = null;
    		}
            // Results are an array...
    		else if ( results.getClass().isArray() ) {
				bnodes = new ArrayList<Value>();

				int iSize = Arrays.asList(results).size();
				for (int iIndex = 0; iIndex < iSize; iIndex++) {
    				bnodes.add( this.theFactory.createBNode() );
    			}
    		}
            // Results are singular...
			else {
				String strResult = results.toString();
				if (strResult != null && strResult.length() > 0 ) {
                	bnodes = new ArrayList<Value>();
                	bnodes.add( this.theFactory.createBNode() );
				}
            }
    	}
		catch (Exception e) {
            // An empty cell might result in an exception out of evaluating IRI expression,
            //   so it is intended to eat the exception...
			bnodes = null;
    	}

		if (bnodes != null && bnodes.size() == 0)
			bnodes = null;
		return bnodes;
    }

    @Override
    protected void writeNode(JsonGenerator writer) throws JsonGenerationException, IOException {
        writer.writeStringField("nodeType", CellBlankNode.NODETYPE);
        if (strColumnName != null) {
        	writer.writeStringField("columnName", this.strColumnName);
        }
        writer.writeStringField("expression", this.strExpression);
        writer.writeBooleanField("isRowNumberCell", this.bIsRowNumberCell);
    }
}
