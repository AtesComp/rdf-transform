package com.google.refine.rdf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.io.IOException;

import com.google.refine.expr.ExpressionUtils;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CellLiteralNode extends LiteralNode implements CellNode {
    static private final Logger logger = LoggerFactory.getLogger("RDFT:CellResNode");

	static private final String strNODETYPE = "cell-as-literal";

    private final String strColumnName;
    private final String strExpression;
    private final boolean bIsRowNumberCell;

    private Record theRecord = null;

    @JsonCreator
    public CellLiteralNode(
    		@JsonProperty("columnName")      String strColumnName,
    		@JsonProperty("expression")      String strExp,
    		@JsonProperty("valueType")       String strValueType,
    		@JsonProperty("lang")            String strLanguage,
    		@JsonProperty("isRowNumberCell") boolean bIsRowNumberCell )
	{
        super(strValueType, strLanguage);
    	this.strColumnName    = strColumnName;
        this.strExpression    = ( strExp == null ? "value" : strExp );
        this.bIsRowNumberCell = bIsRowNumberCell;
    }

    static String getNODETYPE() {
        return CellLiteralNode.strNODETYPE;
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
		return CellLiteralNode.strNODETYPE;
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

    /*
     *  Method createObjects() creates the object list for triple statements
     *  from this node on Rows / Records.
     */
	@Override
    protected List<Value> createObjects(
							ParsedIRI baseIRI, ValueFactory factory, RepositoryConnection connection,
							Project project, ResourceNode nodeParent ) {
        this.baseIRI = baseIRI;
        this.theFactory = factory;
        this.theConnection = connection;
        this.theProject = project;

        this.theRecord = nodeParent.getRecord();

		List<Value> listLiterals = null;
		if (this.theRecord != null) {
            listLiterals = createRecordLiterals();
        }
        else {
            listLiterals = createRowLiterals( nodeParent.getRowIndex() );
        }

		return listLiterals;
	}

    /*
     *  Method createRecordLiterals() creates the object list for triple statements
     *  from this node on Records
     */
	private List<Value> createRecordLiterals() {
		List<Value> listLiterals = new ArrayList<Value>();
		List<Value> listLiteralsNew = null;
		for (int iRowIndex = this.theRecord.fromRowIndex; iRowIndex < this.theRecord.toRowIndex; iRowIndex++) {
			listLiteralsNew = this.createRowLiterals(iRowIndex);
			if (listLiteralsNew != null) {
				listLiterals.addAll(listLiteralsNew);
			}
		}
        if ( listLiterals.isEmpty() )
			return null;
		return listLiterals;
	}

    /*
     *  Method createRowLiterals() creates the object list for triple statements
     *  from this node on Rows
     */
	private List<Value> createRowLiterals(int iRowIndex) {
		Object results = null;
        try {
            results =
				Util.evaluateExpression(this.theProject, this.strExpression, this.strColumnName, iRowIndex);
		}
		catch (Exception e) {
			// An empty cell might result in an exception out of evaluating IRI expression,
			//   so it is intended to eat the exception...
			return null;
		}
	
		// Results cannot be classed...
		if ( results == null || ExpressionUtils.isError(results) ) {
			return null;
		}

		List<String> listStrings = new ArrayList<String>();

		// Results are an array...
		if ( results.getClass().isArray() ) {
			if (Util.isDebugMode()) CellLiteralNode.logger.info("DEBUG: Result is Array...");

			List<Object> listResult = Arrays.asList(results);
			for (Object objResult : listResult) {
				String strResult = Util.toSpaceStrippedString(objResult);
				if (Util.isDebugMode()) CellLiteralNode.logger.info("DEBUG: strResult: " + strResult);
				if ( strResult != null && ! strResult.isEmpty() ) {
					listStrings.add( strResult );
				}
			}
		}
		// Results are singular...
		else {
			String strResult = Util.toSpaceStrippedString(results);
			if (Util.isDebugMode()) logger.info("DEBUG: strResult: " + strResult);
			if (strResult != null && ! strResult.isEmpty() ) {
				listStrings.add(strResult);
			}
		}

		if ( listStrings.isEmpty() ) {
			return null;
		}

		//
		// Process each string as a Literal with the following preference:
		//    1. a given Datatype
		//    2. a given Language code
		//    3. nothing, just a simple string Literal
		//
		List<Value> listLiterals = new ArrayList<Value>();
		for (String strValue : listStrings) {
			Literal literal;
			if (this.strValueType != null) {
				literal = this.theFactory.createLiteral( strValue, this.theFactory.createIRI(this.strValueType) );
			}
			else if (this.strLanguage != null) {
				literal = this.theFactory.createLiteral( strValue, this.strLanguage );
			}
			else {
				literal = this.theFactory.createLiteral( strValue );
			}
			listLiterals.add(literal);
		}

        if ( listLiterals.isEmpty() )
			listLiterals = null;
		return listLiterals;
    }

	@Override
	public void write(JsonGenerator writer)
			throws JsonGenerationException, IOException {
		writer.writeStartObject();

		writer.writeStringField("nodeType", CellLiteralNode.strNODETYPE);
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
