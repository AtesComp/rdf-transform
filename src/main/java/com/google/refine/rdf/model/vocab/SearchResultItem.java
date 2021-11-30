package com.google.refine.rdf.model.vocab;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.refine.rdf.model.Util;

public class SearchResultItem {
	private String strIRI;
	private String strLabel;
	private String strDesc;
	private String strPrefix;
	private String strNamespace;
	private String strLocalPart;

	public SearchResultItem(
				String strIRI, String strLabel, String strDesc,
				String strPrefix, String strNamespace, String strLocalPart) {
		this.strIRI       = strIRI;
		this.strLabel     = strLabel;
		this.strDesc      = strDesc;
		this.strPrefix    = strPrefix;
		this.strNamespace = strNamespace;
		this.strLocalPart = strLocalPart;
	}

	public String getIRI() {
		return this.strIRI;
	}

	public String getLabel() {
		return this.strLabel;
	}

	public String getDescription() {
		return this.strDesc;
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
		if (this.strIRI != null) {
			theWriter.writeStringField(Util.gstrIRI, this.strIRI);
		}
		if (this.strLabel != null) {
			theWriter.writeStringField(Util.gstrLabel, this.strLabel);
		}
		if (this.strDesc != null) {
			theWriter.writeStringField(Util.gstrDesc, this.strDesc);
		}
		if (this.strPrefix != null) {
			theWriter.writeStringField(Util.gstrPrefix, this.strPrefix);
		}
		if (this.strNamespace != null) {
			theWriter.writeStringField(Util.gstrNamespace, this.strNamespace);
		}
		if (this.strLocalPart != null) {
			theWriter.writeStringField(Util.gstrLocalPart, this.strLocalPart);
		}
		// The long "description" contains everything:
		//		the full IRI,
		//		the Label,
		//		the stored Description,
		//		the Prefix,
		//		the Namespace, and
		//		the Local Part
		// for searchTerm display purposes.
		String strDescription =
			this.strIRI +
			((this.strDesc != null) ?
				("<br/>" + "<em>Label</em>: " + this.strLabel) : "") +
			((this.strDesc != null) ?
				("<br/>" + "<em>Description</em>: " + this.strDesc) : "") +
			((this.strPrefix != null) ?
				("<br/>" + "<em>Prefix</em>: " + this.strPrefix) : "") +
			((this.strNamespace != null) ?
				("<br/>" + "<em>Namespace</em>: " + this.strNamespace) : "") +
			((this.strLocalPart != null) ?
				("<br/>" + "<em>LocalPart</em>: " + this.strLocalPart) : "");
		theWriter.writeStringField(Util.gstrDescription, strDescription);
		theWriter.writeEndObject();
	}

}
