package com.google.refine.rdf;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.Value;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

abstract public class LiteralNode extends Node {
    protected final String strValueType;
    protected final String strLanguage;

    public LiteralNode(String strValueType, String strLanguage)
    {
        this.strValueType = strValueType;
        this.strLanguage = strLanguage;
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

    /*
     *  Method createObjects() creates the object list for triple statements
     *  from this node on Rows / Records.
     */
    @Override
	protected List<Value> createObjects(ResourceNode nodeParent) {
        this.baseIRI = nodeParent.baseIRI;
        this.theFactory = nodeParent.theFactory;
        this.theConnection = nodeParent.theConnection;
        this.theProject = nodeParent.theProject;

        // TODO: Convert from Record to Row unless specifed as a Sub-Record
        // TODO: Create findSubRecord()

        this.theRec.setRowRecord(nodeParent);
        List<Value> literals = null;
        if ( this.theRec.isRecordMode() ) {
            literals = createRecordObjects();
        } // Row Mode...
        else {
            literals = createRowObjects();
        }
        this.theRec.clear();

        return literals;
    }

    /*
     *  Method createRecordObjects() creates the object list for triple statements
     *  from this node on Records
     */
    private List<Value> createRecordObjects() {
		List<Value> literals = new ArrayList<Value>();
		List<Value> literalsNew = null;
		for (int iRowIndex = this.theRec.rowStart(); iRowIndex < this.theRec.rowEnd(); iRowIndex++) {
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
     *  from this node on a Row
     */
    abstract protected List<Value> createRowObjects();
}
