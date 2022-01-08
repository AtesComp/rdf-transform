package com.google.refine.rdf.model;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerationException;

abstract public class LiteralNode extends Node {
    protected final String strDatatype;
    protected final String strLanguage;

    public LiteralNode(String strDatatype, String strLanguage)
    {
        this.strDatatype = strDatatype;
        this.strLanguage = strLanguage;
    }

    @JsonProperty("datatype")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getDatatype() {
        return this.strDatatype;
    }

    @JsonProperty("language")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getLanguage() {
        return this.strLanguage;
    }

    /*
     *  Method setObjectParameters()
     * 
     *  Copy to this Literal object the parameters from the given Property resource.
     * 
     */
    @JsonIgnore
	protected void setObjectParameters(ResourceNode nodeProperty) {
        this.baseIRI = nodeProperty.baseIRI;
        this.theFactory = nodeProperty.theFactory;
        this.theConnection = nodeProperty.theConnection;
        this.theProject = nodeProperty.theProject;
    }

    abstract protected void writeNode(JsonGenerator writer) throws JsonGenerationException, IOException;

    public void write(JsonGenerator writer) throws JsonGenerationException, IOException {
        writer.writeStartObject();

        // Write node...
        this.writeNode(writer);

        writer.writeEndObject();
    }

}
