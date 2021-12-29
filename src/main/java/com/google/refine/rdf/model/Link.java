package com.google.refine.rdf.model;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerationException;

// Class Link: A Predicate-Object collection
//		The Predicate is stored as:
//			Absolute IRI
//			Prefixed IRI
//		The Object is stored as:
//			a data strucure defining the object node:
//				a Column Value, natural or calculated, resource or literal
//				a Constant Value, natural or calculated, resource or literal
//
//		NOTE: Blank node resources have distinct processing requirements
//				not found here.
//
public class Link {
	// The Predicate: An Absolute IRI
	@JsonProperty("iri")
    private final String strProperty;

	// The Predicate: A Prefixed IRI (CIRIE: Condensed IRI Expression)
	@JsonProperty("cirie")
    private final String strPrefixedProperty;

	// The Target: A source's target "node" connected via this Predicate
	@JsonProperty("target")
	@JsonInclude(JsonInclude.Include.NON_NULL)
    private final Node nodeObject;

    @JsonCreator
    public Link(	@JsonProperty("iri")    String strIRI,
					@JsonProperty("cirie")  String strCIRIE,
					@JsonProperty("target") Node nodeObject	) {
        this.strProperty = strIRI;
        this.strPrefixedProperty = strCIRIE;
        this.nodeObject = nodeObject;
    }

	@JsonProperty("iri")
	public String getProperty() {
		return this.strProperty;
	}

	@JsonProperty("cirie")
	public String getPrefixedProperty() {
		return this.strPrefixedProperty;
	}

	@JsonProperty("target")
	public Node getObject() {
		return this.nodeObject;
	}

	public void write(JsonGenerator writer) throws JsonGenerationException, IOException{
		writer.writeStartObject();

		writer.writeStringField("iri", strProperty);
		writer.writeStringField("cirie", strPrefixedProperty);
		if (this.nodeObject != null) {
			writer.writeFieldName("target");
			this.nodeObject.write(writer);
		}
		writer.writeEndObject();
	}
}
