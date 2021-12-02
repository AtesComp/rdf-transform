package com.google.refine.rdf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.io.IOException;

import com.google.refine.expr.EvalError;
import com.google.refine.model.Project;
import com.google.refine.model.Record;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerationException;

import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CellLiteralNode extends CellNode {

	static private final String NODETYPE = "cell-as-literal";

    private final String strColumnName;
    private final String strExpression;
	private final String strValueType;
    private final String strLanguage;
    private final boolean bIsRowNumberCell;

    @JsonCreator
    public CellLiteralNode(
    		@JsonProperty("columnName")      String strColumnName,
    		@JsonProperty("expression")      String strExp,
    		@JsonProperty("valueType")       String strValueType,
    		@JsonProperty("lang")            String strLanguage,
    		@JsonProperty("isRowNumberCell") boolean bIsRowNumberCell )
	{
    	this.strColumnName    = strColumnName;
        this.strExpression    = ( strExp == null ? "value" : strExp );
        this.strValueType     = strValueType;
        this.strLanguage      = strLanguage;
        this.bIsRowNumberCell = bIsRowNumberCell;
    }

	@Override
	public String getNodeName() {
		String strName =
			( bIsRowNumberCell ? "<ROW#>" : this.strColumnName ) + 
            ( "<" + this.strExpression + ">" );

        // If there is a value type...
        if (this.strValueType != null) {
            strName += "^^" + this.strValueType;
        }

        // If there is not a value type AND there is a language...
        else if (this.strLanguage != null) {
            strName += "@" + strLanguage;
        }

		return strName;
	}

	@Override
	public String getNodeType() {
		return CellLiteralNode.NODETYPE;
	}

	@Override
	public String getColumnName() {
		return this.strColumnName;
	}

    @JsonProperty("expression")
    public String getExpression() {
    	return this.strExpression;
    }

    @JsonProperty("valueType")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getValueType() {
        return this.strValueType;
    }

    @JsonProperty("lang")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getLanguage() {
        return this.strLanguage;
    }

	@Override
	@JsonProperty("isRowNumberCell")
	public boolean isRowNumberCellNode() {
		return this.bIsRowNumberCell;
	}

    /*
     *  Method createObjects() creates the object list for triple statements
     *  from this node on Rows
     */
	@Override
    protected List<Value> createObjects(
							ParsedIRI baseIRI, ValueFactory factory, RepositoryConnection connection,
							Project project, ResourceNode nodeParent ) {
        this.baseIRI = baseIRI;
        this.theFactory = factory;
        this.theConnection = connection;
        this.theProject = project;

		List<Value> literals = null;
        Record theRecord = nodeParent.getRecord();
        if (theRecord != null) {
            literals = createRecordObjects(theRecord);
        }
        else {
            literals =
				createRowObjects( nodeParent.getRowIndex() );
        }

		return literals;
	}

    /*
     *  Method createRecordObjects() creates the object list for triple statements
     *  from this node on Records
     */
	private List<Value> createRecordObjects(Record theRecord ) {
		List<Value> literals = new ArrayList<Value>();
		List<Value> literalsNew = null;
		for (int iRowIndex = theRecord.fromRowIndex; iRowIndex < theRecord.toRowIndex; iRowIndex++) {
			literalsNew =
				this.createRowObjects(iRowIndex);
			if (literalsNew != null) {
				literals.addAll(literalsNew);
			}
		}
        if (literals.size() == 0)
			return null;
		return literals;
	}

    /*
     *  Method createObjects() creates the object list for triple statements
     *  from this node on Rows
     */
	private List<Value> createRowObjects(int iRowIndex) {
		List<String> astrValues = null;
        try {
            Object results =
				Util.evaluateExpression(this.theProject, strExpression, strColumnName, iRowIndex);

            if (results.getClass() == EvalError.class) {
            	astrValues = null;
            }
			else if ( results.getClass().isArray() ) {
				astrValues = new ArrayList<String>();
				List<Object> listResult = Arrays.asList(results);
           		for (Object objResult : listResult) {
           			astrValues.add(objResult.toString());
           		}
           	}
			else if (results.toString().length() > 0) {
				astrValues = new ArrayList<String>();
            	astrValues.add(results.toString());
            }
    	}
		catch (Exception e) {
    		// An empty cell might result in an exception out of evaluating IRI expression,
			//   so it is intended to eat the exception...
    		astrValues = null;
    	}

		List<Value> literals = null;
		if ( astrValues != null && ! astrValues.isEmpty() ) {
        	literals = new ArrayList<Value>();
        	for (String strValue : astrValues) {
        		Literal literal;
            	if (this.strValueType != null) {
                	literal = this.theFactory.createLiteral( strValue, this.theFactory.createIRI(strValueType) );
            	}
				else if (this.strLanguage != null) {
            		literal = this.theFactory.createLiteral( strValue, strLanguage );
            	}
				else {
            		literal = this.theFactory.createLiteral( strValue );
            	}
            	literals.add(literal);
        	}
        }

        if (literals == null || literals.size() == 0)
			return null;
		return literals;
    }

	@Override
	public void write(JsonGenerator writer)
			throws JsonGenerationException, IOException {
		writer.writeStartObject();

		writer.writeStringField("nodeType", CellLiteralNode.NODETYPE);
		if (strColumnName != null) {
			writer.writeStringField("columnName", this.strColumnName);
		}
		writer.writeStringField("expression", this.strExpression);
		if (strValueType != null ) {
			writer.writeStringField("valueType", this.strValueType);
		}
		if (strLanguage != null) {
			writer.writeStringField("lang", this.strLanguage);
		}
		writer.writeBooleanField("isRowNumberCell", this.bIsRowNumberCell);

		writer.writeEndObject();
	}
}
