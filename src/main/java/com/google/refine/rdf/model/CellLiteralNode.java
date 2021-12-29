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
    private final boolean bIsRowNumberCell;

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
	protected List<Value> createObjects(ResourceNode nodeProperty) {
        this.setObjectParameters(nodeProperty);
		if (Util.isDebugMode()) CellLiteralNode.logger.info("DEBUG: createObjects...");

        // TODO: Create process for Sub-Records

        List<Value> literals = null;

        //
        // Record Mode
        //
		if ( nodeProperty.theRec.isRecordMode() ) { // ...link is Record, 
			// ...set to Row Mode and process on current row as set by rowNext()...
			this.theRec.setLink(nodeProperty, true);
            literals = this.createRecordObjects();
        }
        //
        // Row Mode
        //
        else {
			// ...process on current row as set by rowNext()...
			this.theRec.setLink(nodeProperty);
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
			if (this.strValueType != null) {
				literal = this.theFactory.createLiteral( strValue, this.theFactory.createIRI(this.strValueType) );
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
