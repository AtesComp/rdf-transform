package com.google.refine.rdf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.io.IOException;

import com.google.refine.expr.ExpressionUtils;
import com.google.refine.model.Project;
import com.google.refine.model.Record;

import org.eclipse.rdf4j.common.net.ParsedIRI;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerationException;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CellResourceNode extends ResourceNode {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:CellResNode");

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
    protected List<Value> createResources(ParsedIRI baseIRI, ValueFactory factory,
                                            RepositoryConnection connection, Project project) {
        this.baseIRI = baseIRI;
        this.theFactory = factory;
        this.theConnection = connection;
        this.theProject = project;

		List<Value> listResources = null;
        if (this.getRecord() != null) {
            listResources = createRecordResources();
        }
        else {
            listResources = createRowResources( this.getRowIndex() );
        }

		return listResources;
    }

    private List<Value> createRecordResources() {
        List<Value> listResources = new ArrayList<Value>();
		List<Value> listResourcesNew = null;
        Record theRecord = this.getRecord();
		for (int iRowIndex = theRecord.fromRowIndex; iRowIndex < theRecord.toRowIndex; iRowIndex++) {
			listResourcesNew = this.createRowResources(iRowIndex);
			if (listResourcesNew != null) {
				listResources.addAll(listResourcesNew);
			}
		}
        if ( listResources.isEmpty() )
			return null;
		return listResources;
    }

    private List<Value> createRowResources(int iRowIndex) {
        List<Value> listResources = null;
        try {
        	Object results =
                Util.evaluateExpression(this.theProject, this.strExpression, this.strColumnName, iRowIndex);

            String strResource = null;

            // Results cannot be classed...
            if ( ExpressionUtils.isError(results) ) {
            	listResources = null;
            }
            // Results are an array...
            else if ( results.getClass().isArray() ) {
                if (Util.isDebugMode()) logger.info("DEBUG: Result is Array...");

                listResources = new ArrayList<Value>();
                List<Object> listResult = Arrays.asList(results);
                for (Object objResult : listResult) {
                    String strResult = Util.toSpaceStrippedString(objResult);
                    if (Util.isDebugMode()) logger.info("DEBUG: strResult: " + strResult);
                    if ( strResult != null && ! strResult.isEmpty() ) {
                        strResource = Util.resolveIRI( this.baseIRI, strResult );
                        if (strResource != null) {
                            strResource = this.expandPrefixedIRI(strResource);
                            if (Util.isDebugMode()) logger.info("DEBUG: strResource: " + strResource);
                            listResources.add( this.theFactory.createIRI(strResource) );
                        }
                    }
            	}
            }
            // Results are singular...
            else {
                String strResult = Util.toSpaceStrippedString(results);
                if (Util.isDebugMode()) logger.info("DEBUG: strResult: " + strResult);
                if (strResult != null && ! strResult.isEmpty() ) {
                    strResource = Util.resolveIRI(this.baseIRI, strResult);
                    if (strResource != null) {
                        strResource = this.expandPrefixedIRI(strResource);
                        if (Util.isDebugMode()) logger.info("DEBUG: strResource: " + strResource);
                        listResources = new ArrayList<Value>();
                        listResources.add( this.theFactory.createIRI(strResource) );
                    }
                }
            }
        }
        catch (Exception ex) {
            // An empty cell might result in an exception out of evaluating IRI expression,
            //   so it is intended to eat the exception...
            listResources = null;
        }

        if ( listResources == null || listResources.isEmpty() )
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
