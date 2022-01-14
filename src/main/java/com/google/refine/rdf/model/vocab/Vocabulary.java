package com.google.refine.rdf.model.vocab;

//import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
//import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerationException;

import java.io.IOException;

@JsonIgnoreType
public class Vocabulary {
	private String strPrefix;		// Short name that represents the Namespace
	private String strNamespace;	// The fully qualified Namespace

    public Vocabulary(String strPrefix, String strNamespace )
	{
    	this.strPrefix = strPrefix;
    	this.strNamespace = strNamespace;
    }

	public String getPrefix() {
		return strPrefix;
	}

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

    public void write(JsonGenerator theWriter)
			throws JsonGenerationException, IOException {
        theWriter.writeStringField(strPrefix, strNamespace);
	}
}
