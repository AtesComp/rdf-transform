package com.google.refine.rdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.refine.model.Project;
import com.google.refine.model.Record;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerationException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;

public class ConstantLiteralNode extends LiteralNode implements ConstantNode {

    static private final String strNODETYPE = "literal";

    private final String strValue;

    @JsonCreator
    public ConstantLiteralNode(
    		@JsonProperty("value")     String strValue,
    		@JsonProperty("valueType") String strValueType,
    		@JsonProperty("lang")      String strLanguage )
    {
        super(strValueType, strLanguage);
        this.strValue = strValue; // ..no stripping here!
    }

    static String getNODETYPE() {
        return ConstantLiteralNode.strNODETYPE;
    }

	@Override
	public String getNodeName() {
        String strName = "<NULL>";

        // If there is a value to work with...
        if (this.strValue != null && this.strValue.length() > 0) {
            strName = this.strValue;

            // If there is a value type...
            if (this.strValueType != null) {
                strName += "^^" + this.strValueType;
            }

            // If there is not a value type AND there is a language...
            else if (this.strLanguage != null) {
                strName += "@" + strLanguage;
            }
        }

		return strName;
	}

	@Override
	public String getNodeType() {
		return ConstantLiteralNode.strNODETYPE;
	}

    @JsonProperty("value")
    public String getValue() {
        return this.strValue;
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
            literals = createRowObjects();
        }

        return literals;
    }

    /*
     *  Method createRecordObjects() creates the object list for triple statements
     *  from this node on Records
     */
    private List<Value> createRecordObjects(Record theRecord) {
		List<Value> literals = new ArrayList<Value>();
		List<Value> literalsNew = null;
		for (int iRowIndex = theRecord.fromRowIndex; iRowIndex < theRecord.toRowIndex; iRowIndex++) {
			literalsNew = this.createRowObjects();
			if (literalsNew != null) {
				literals.addAll(literalsNew);
			}
		}
        if (literals.size() == 0)
			return null;
		return literals;
	}

    /*
     *  Method createRowbjects() creates the object list for triple statements
     *  from this node on Rows
     */
	private List<Value> createRowObjects()
    {
        List<Value> literals = null;

        // If there is a value to work with...
        if (this.strValue != null && this.strValue.length() > 0) {
            Literal literal = null;

            // If there is a value type...
            if (this.strValueType != null) {
                IRI iriValueType = null;
                try {
                    iriValueType =
                        this.theFactory.createIRI(
                            this.expandPrefixedIRI(this.strValueType)
                        );
                }
                catch (IllegalArgumentException ex) {
                    // ...continue to get literal another way...
                }
                if (iriValueType != null) {
                    literal = this.theFactory.createLiteral( this.strValue, iriValueType );
                }
            }

            // If there is not a value type OR there was an exception AND there is a language...
            if (literal == null && this.strLanguage != null) {
                    literal = this.theFactory.createLiteral(this.strValue, strLanguage);
            }

            // If there is NOT a value type OR language...
            if (literal == null) {
                // Don't decorate the value...
                literal = this.theFactory.createLiteral(this.strValue);
            }

            literals = new ArrayList<Value>();
            literals.add(literal);
        }
        return literals;
    }

	@Override
    public void write(JsonGenerator writer) throws JsonGenerationException, IOException {
        writer.writeStartObject();

        writer.writeStringField("nodeType", ConstantLiteralNode.strNODETYPE);
        writer.writeStringField("value", strValue);
        if (strValueType != null) {
            writer.writeStringField("valueType", strValueType);
        }
        if (strLanguage != null) {
            writer.writeStringField("lang", strLanguage);
        }

        writer.writeEndObject();
    }
}
