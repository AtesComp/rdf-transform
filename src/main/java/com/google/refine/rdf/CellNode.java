package com.google.refine.rdf;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class CellNode extends Node {
	@JsonProperty("columnName")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public abstract String getColumnName();

	@JsonProperty("isRowNumberCell")
	public abstract boolean isRowNumberCellNode();
}
