package com.google.refine.rdf.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.io.IOException;

import com.google.refine.expr.ExpressionUtils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerationException;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CellLiteralNode extends LiteralNode implements CellNode {
    static private final Logger logger = LoggerFactory.getLogger("RDFT:CellLitNode");

	static private final String strNODETYPE = "cell-as-literal";

    private final String strColumnName;
    private final String strExpression;

    @JsonCreator
    public CellLiteralNode(
    		@JsonProperty("columnName")  String strColumnName,
    		@JsonProperty("expression")  String strExp,
    		@JsonProperty("isIndex")     boolean bIsIndex,
    		@JsonProperty("datatype")    String strDatatype,
    		@JsonProperty("language")    String strLanguage )
	{
        super(strDatatype, strLanguage);
    	this.strColumnName    = strColumnName;
        this.strExpression    = ( strExp == null ? "value" : strExp );
        this.bIsIndex = bIsIndex;
    }

    static String getNODETYPE() {
        return CellLiteralNode.strNODETYPE;
    }

	@Override
	public String getNodeName() {
		String strName =
			( this.bIsIndex ? "<ROW#>" : this.strColumnName ) + 
            ( "<" + this.strExpression + ">" );

        // If there is a value type...
        if (this.strDatatype != null) {
            strName += "^^" + this.strDatatype;
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

	@JsonProperty("isIndex")
	public boolean isIndexNode() {
		return this.bIsIndex;
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
	protected List<Value> createObjects(ResourceNode nodeProperty) {
        this.setObjectParameters(nodeProperty);
		if (Util.isDebugMode()) CellLiteralNode.logger.info("DEBUG: createObjects...");

        // TODO: Create process for Sub-Records

        List<Value> literals = null;

        //
        // Record Mode
        //
		if ( nodeProperty.theRec.isRecordMode() ) { // ...property is Record based, 
			// ...set to Row Mode and process on current row as set by rowNext()...
			this.theRec.setMode(nodeProperty, true);
            literals = this.createRecordObjects();
        }
        //
        // Row Mode
        //
        else {
			// ...process on current row as set by rowNext()...
			this.theRec.setMode(nodeProperty);
            literals = this.createRowObjects();
        }
        this.theRec.clear();

        return literals;
    }

    /*
     *  Method createRecordObjects() creates the object list for triple statements
     *  from this node on Records
     */
    private List<Value> createRecordObjects() {
		if (Util.isDebugMode()) CellLiteralNode.logger.info("DEBUG: createRecordObjects...");
		List<Value> literals = new ArrayList<Value>();
		List<Value> literalsNew = null;
		while ( this.theRec.rowNext() ) {
			literalsNew = this.createRowObjects();
			if (literalsNew != null) {
				literals.addAll(literalsNew);
			}
		}
        if ( literals.isEmpty() )
			return null;
		return literals;
	}

    /*
     *  Method createRowLiterals() creates the object list for triple statements
     *  from this node on Rows
     */
	private List<Value> createRowObjects() {
		if (Util.isDebugMode()) CellLiteralNode.logger.info("DEBUG: createRowObjects...");
		Object results = null;
        try {
            results =
				Util.evaluateExpression( this.theProject, this.strExpression, this.strColumnName, this.theRec.row() );
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
		List<Value> literals = new ArrayList<Value>();
		for (String strValue : listStrings) {
			Literal literal;
			if (this.strDatatype != null) {
				literal = this.theFactory.createLiteral( strValue, this.theFactory.createIRI(this.strDatatype) );
			}
			else if (this.strLanguage != null) {
				literal = this.theFactory.createLiteral( strValue, this.strLanguage );
			}
			else {
				literal = this.theFactory.createLiteral( strValue );
			}
			literals.add(literal);
		}

        if ( literals.isEmpty() )
			literals = null;
		return literals;
    }

	@Override
	public void writeNode(JsonGenerator writer)
			throws JsonGenerationException, IOException {
		writer.writeStringField("nodeType", CellLiteralNode.strNODETYPE);
		if (this.strColumnName != null) {
			writer.writeStringField("columnName", this.strColumnName);
		}
		if (this.strExpression != null) {
			writer.writeStringField("expression", this.strExpression);
		}
		if (this.strDatatype != null ) {
			writer.writeStringField("valueType", this.strDatatype);
		}
		else if (this.strLanguage != null) {
			writer.writeStringField("language", this.strLanguage);
		}
		if (this.bIsIndex) {
			writer.writeBooleanField("isIndex", this.bIsIndex);
		}
	}
}
