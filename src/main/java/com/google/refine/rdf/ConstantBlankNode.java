package com.google.refine.rdf;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.common.net.ParsedIRI;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerationException;
import org.eclipse.rdf4j.model.BNode;
//import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.refine.model.Project;

public class ConstantBlankNode extends ResourceNode {

	final static private String NODETYPE = "blank";

	private BNode bnode = null;

	@JsonCreator
	ConstantBlankNode() {}

	@Override
	public String getNodeName() {
		return "<BNode>";
	}

	@Override
	public String getNodeType() {
		return ConstantBlankNode.NODETYPE;
	}

	@Override
	public List<Value> createResources(
								ParsedIRI baseIRI, ValueFactory factory, Project project )
	{
		if (bnode == null) {
    		bnode = factory.createBNode();
    	}
		List<Value> bnodes = new ArrayList<Value>();
		bnodes.add(bnode);
        return bnodes;
    }

	@Override
	protected void writeNode(JsonGenerator writer) throws JsonGenerationException, IOException {
		writer.writeStringField("nodeType", ConstantBlankNode.NODETYPE);
	}
}
