package com.google.refine.rdf.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    @JsonIgnore
	protected void setObjectParameters(ResourceNode nodeProperty) {
        this.baseIRI = nodeProperty.baseIRI;
        this.theFactory = nodeProperty.theFactory;
        this.theConnection = nodeProperty.theConnection;
        this.theProject = nodeProperty.theProject;
    }
}
