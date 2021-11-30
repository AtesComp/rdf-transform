package com.google.refine.rdf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RDFType {
    private String strIRI;
	private String strCIRIE;

    @JsonCreator
    public RDFType( @JsonProperty("iri")   String strIRI,
    		        @JsonProperty("cirie") String strCIRIE ) {
        this.strIRI   = strIRI;
        this.strCIRIE = strCIRIE;
    }

    @JsonProperty("iri")
    public String getResource() {
		return strIRI;
	}

    @JsonProperty("cirie")
    public String getPrefixedResource() {
    	return strCIRIE;
    }
}