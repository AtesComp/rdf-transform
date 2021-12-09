package com.google.refine.rdf;

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
}
