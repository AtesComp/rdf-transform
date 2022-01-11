package com.google.refine.rdf.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;

//import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract public class LiteralNode extends Node {
	static private final Logger logger = LoggerFactory.getLogger("RDFT:LitNode");

    protected ConstantResourceNode nodeDatatype;
    protected String strLanguage;

    @JsonProperty("datatype")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getDatatype() {
        return this.nodeDatatype.normalizeResourceAsString();
    }

    @JsonProperty("language")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getLanguage() {
        return this.strLanguage;
    }

    /*
     *  Method createObjects() creates the object list for triple statements
     *  from this node on Rows / Records.
     */
    @Override
	protected List<Value> createObjects(ResourceNode nodeProperty) {
		if (Util.isDebugMode()) LiteralNode.logger.info("DEBUG: createObjects...");

        this.setObjectParameters(nodeProperty);

        // TODO: Create process for Sub-Records

		this.listValues = null;

		//
        // Record Mode...
        //
		if ( nodeProperty.theRec.isRecordMode() ) { // ...property is Record based,
			// ...set to Row Mode and process on current row as set by rowNext()...
			this.theRec.setMode(nodeProperty, true);
            this.createRecordLiterals();
        }
        //
        // Row Mode...
        //
        else {
			// ...process on current row as set by rowNext()...
			this.theRec.setMode(nodeProperty);
            this.createRowLiterals();
        }

        this.theRec.clear();

        // Return the collected resources from the statement processing as Objects
        // to the given Property...
        return this.listValues;
    }

    /*
     *  Method createRecordLiterals() creates the object list for triple statements
     *  from this node on Records
     */
    protected void createRecordLiterals() {
		if (Util.isDebugMode()) LiteralNode.logger.info("DEBUG: createRecordLiterals...");

        List<Value> listLiterals = new ArrayList<Value>();
		while ( this.theRec.rowNext() ) {
			this.createRowLiterals();
			if (this.listValues != null) {
				listLiterals.addAll(this.listValues);
			}
		}
        if ( listLiterals.isEmpty() ) {
			listLiterals = null;
		}

		this.listValues = listLiterals;
	}

    abstract protected void createRowLiterals();

    /*
     *  Method normalizeLiteral() for Literal Node to Literal string
     */
    protected void normalizeLiteral(Object objResult) {
        String strResult = Util.toSpaceStrippedString(objResult);
        if ( Util.isDebugMode() ) LiteralNode.logger.info("DEBUG: normalizeLiteral: Result: " + strResult);

        if ( ! ( strResult == null || strResult.isEmpty() ) ) {
            //
            // Process each string as a Literal with the following preference:
            //    1. a given Datatype
            //    2. a given Language code
            //    3. nothing, just a simple string Literal
            //
            Literal literal = null;

            // If there is a datatype...
            if (this.nodeDatatype != null) {
                IRI iriDatatype = null;
                try {
                    iriDatatype =
                        this.theFactory.createIRI( this.nodeDatatype.normalizeResourceAsString() );
                }
                catch (IllegalArgumentException ex) {
                    // ...continue to get literal another way...
                }
                if (iriDatatype != null) {
                    literal = this.theFactory.createLiteral(strResult, iriDatatype);
                }
            }
            // Else if there is a language...
            else if (this.strLanguage != null) {
                literal = this.theFactory.createLiteral(strResult, this.strLanguage);
            }
            // Otherwise...
            else {
                // ...don't decorate the value...
                literal = this.theFactory.createLiteral(strResult);
            }

            // If there is a valid literal...
            if (literal != null) {
                this.listValues.add(literal);
            }
        }
    }

    abstract protected void writeNode(JsonGenerator writer) throws JsonGenerationException, IOException;

    public void write(JsonGenerator writer) throws JsonGenerationException, IOException {
        writer.writeStartObject();

        // Write node...
        this.writeNode(writer);

        writer.writeEndObject();
    }

}
