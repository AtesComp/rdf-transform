package com.google.refine.rdf.model.vocab;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;

public class SearchResultItem {
	private String strIRI;
	private String strLabel;
	private String strDescription;
	private String strPrefix;
	private String strNamespace;
	private String strLocalPart;

	public SearchResultItem(
				String strIRI, String strLabel, String strDesc,
				String strPrefix, String strNamespace, String strLocalPart) {
		this.strIRI         = strIRI;
		this.strLabel       = strLabel;
		this.strDescription = strDesc;
		this.strPrefix      = strPrefix;
		this.strNamespace   = strNamespace;
		this.strLocalPart   = strLocalPart;
	}

	public String getIRI() {
		return this.strIRI;
	}

	public String getLabel() {
		return this.strLabel;
	}

	public String getDescription() {
		return this.strDescription;
	}

	public String getPrefix() {
		return this.strPrefix;
	}

	public String getNamespace() {
		return this.strNamespace;
	}

	public String getLocalPart() {
		return this.strLocalPart;
	}

	public void writeAsSearchResult(JsonGenerator theWriter)
			throws IOException {
		theWriter.writeStartObject();
		// The "iri" is the absolute IRI...
		theWriter.writeStringField("iri", this.strIRI);
		// The "cirie" is the Condensed IRI Expression (CIRIE) using
		// the prefix and IRI's local part...
		theWriter.writeStringField("cirie", this.strPrefix + ":" + this.strLocalPart);
		// The "description" contains everything:
		//		the full IRI,
		//		the Label,
		//		the actual stored Description,
		//		the Prefix,
		//		the Namespace, and
		//		the Local Part
		theWriter.writeStringField(
			"description",
			this.strIRI + "<br/>" +
			"<em>Label</em>: " + this.strLabel + "<br/>" +
			"<em>Description</em>: " + this.strDescription + "<br/>" +
			"<em>Prefix</em>: " + this.strPrefix + "<br/>" +
			"<em>Namespace</em>: " + this.strNamespace + "<br/>" +
			"<em>LocalPart</em>: " + this.strLocalPart
		);
		theWriter.writeEndObject();
	}

}
