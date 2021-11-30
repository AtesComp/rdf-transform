package com.google.refine.rdf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.io.IOException;

import com.google.refine.expr.EvalError;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.model.Record;

import org.eclipse.rdf4j.common.net.ParsedIRI;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerationException;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CellResourceNode extends ResourceNode {
	final static private String NODETYPE = "cell-as-resource";

    final private String strColumnName;
    final private String strExpression;
    final private boolean bIsRowNumberCell;

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

	@Override
	public String getNodeName() {
		return ( bIsRowNumberCell ? "<ROW#>" : this.strColumnName ) + 
                ( "<" + this.strExpression + ">" );
	}

	@Override
	public String getNodeType() {
		return CellResourceNode.NODETYPE;
	}

	@JsonProperty("columnName")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public String getColumnName() {
		return this.strColumnName;
	}

    @JsonProperty("expression")
    public String getExpression() {
        return this.strExpression;
    }

	@JsonProperty("isRowNumberCell")
	public boolean isRowNumberCellNode() {
		return this.bIsRowNumberCell;
	}

    @Override
    protected List<Value> createResources(
                            ParsedIRI baseIRI, ValueFactory factory, Project project ) {
		List<Value> listResources = null;
        if (this.getRecord() != null) {
            listResources = createRecordResources(baseIRI, factory, project);
        }
        else {
            listResources =
                createRowResources(baseIRI, factory, project, this.getRow(), this.getRowIndex());
        }

		return listResources;
    }

    private List<Value> createRecordResources(
                            ParsedIRI baseIRI, ValueFactory factory, Project project ) {
        List<Value> listResources = new ArrayList<Value>();
		List<Value> listResourcesNew = null;
        Record theRecord = this.getRecord();
		for (int iRowIndex = theRecord.fromRowIndex; iRowIndex < theRecord.toRowIndex; iRowIndex++) {
			listResourcesNew =
                this.createRowResources(baseIRI, factory, project, project.rows.get(iRowIndex), iRowIndex);
			if (listResourcesNew != null) {
				listResources.addAll(listResourcesNew);
			}
		}
        if (listResources.size() == 0)
			return null;
		return listResources;
    }

    private List<Value> createRowResources(
                            ParsedIRI baseIRI, ValueFactory factory, Project project,
                            Row theRow, int iRowIndex ) {
        List<Value> listResources = null;
        try {
        	Object resultEval =
                Util.evaluateExpression(project, this.strExpression, this.strColumnName,
                                        project.rows.get(iRowIndex), iRowIndex);
            String strIRI = null;

            // Results cannot be the EvalError class...
            if (resultEval.getClass() == EvalError.class) {
            	listResources = null;
            }
            // Results are an array...
            else if ( resultEval.getClass().isArray() ) {
                listResources = new ArrayList<Value>();

                List<Object> listResult = Arrays.asList(resultEval);
                for (Object objResult : listResult) {
                    if ( Util.toSpaceStrippedString(objResult).length() > 0 ) {
                        strIRI = Util.resolveIRI( baseIRI, objResult.toString() );
                        if (strIRI != null) {
                            listResources.add( factory.createIRI(strIRI) );
                        }
                    }
            	}
            }
            // Results are singular...
            else {
                String strResult = Util.toSpaceStrippedString(resultEval);
                if (strResult != null && strResult.length() > 0 ) {
                    strIRI = Util.resolveIRI(baseIRI, strResult);
                    if (strIRI != null) {
                        listResources = new ArrayList<Value>();
                        listResources.add( factory.createIRI(strIRI) ); // TODO
                    }
                }
            }
        }
        catch (Exception ex) {
            // An empty cell might result in an exception out of evaluating IRI expression,
            //   so it is intended to eat the exception...
            listResources = null;
        }

        if (listResources != null && listResources.size() == 0)
            listResources = null;
        return listResources;
    }

	@Override
	protected void writeNode(JsonGenerator writer) throws JsonGenerationException, IOException {
		writer.writeStringField("nodeType", CellResourceNode.NODETYPE);
        if (strColumnName != null) {
        	writer.writeStringField("columnName", this.strColumnName);
        }
        writer.writeStringField("expression", this.strExpression);
        writer.writeBooleanField("isRowNumberCell", this.bIsRowNumberCell);
	}
}
