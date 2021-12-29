package com.google.refine.rdf.model.vocab;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerationException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.IOException;

public class Vocabulary {
	private String strPrefix;		// Short name that represents the Namespace (Prefix for entities within the Namespace)
	private String strNamespace;	// The fully qualified Namespace

	@JsonCreator
    public Vocabulary(
				@JsonProperty("name") String strPrefix,
				@JsonProperty("iri")  String strNamespace )
	{
    	this.strPrefix = strPrefix;
    	this.strNamespace = strNamespace;
    }

    @JsonProperty("name")
	public String getPrefix() {
		return strPrefix;
	}

    @JsonProperty("iri")
	public String getNamespace() {
		return strNamespace;
	}

	@Override
	public int hashCode() {
		return strPrefix.hashCode();
	}

	@Override
	public boolean equals(Object object) {
		if ( object != null && object.getClass().equals( this.getClass() ) ) {
			return strPrefix.equals( ( (Vocabulary) object).getPrefix());
		}
		return false;
	}

    public void write(JsonGenerator writer)throws JsonGenerationException, IOException {
        writer.writeStartObject();

        writer.writeStringField("name", strPrefix);
        writer.writeStringField("iri", strNamespace);

        writer.writeEndObject();
    }
}
