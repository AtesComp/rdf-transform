package com.google.refine.rdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.refine.model.Project;

import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerationException;

public class ConstantResourceNode extends ResourceNode {

    static private final String NODETYPE = "resource";

    private final String strIRI;

    @JsonCreator
    public ConstantResourceNode( @JsonProperty("value") String strIRI ) {
        this.strIRI = Util.toSpaceStrippedString(strIRI);
    }

	@Override
	public String getNodeName() {
		return this.strIRI;
	}

	@Override
	public String getNodeType() {
		return ConstantResourceNode.NODETYPE;
	}

    @JsonProperty("value")
    public String getIRI() {
        return this.strIRI;
    }

    @Override
    public List<Value> createResources(
                            ParsedIRI baseIRI, ValueFactory factory, Project project )
    {
        List<Value> listResources = null;
        String strResource = null;
        if (this.strIRI != null & this.strIRI.length() > 0 ) {
            try {
                strResource = Util.resolveIRI(baseIRI, this.strIRI);
            }
            catch (Util.IRIParsingException ex) {
                // ...continue...
            }
            if (strResource == null) {
                return listResources;
            }

            listResources = new ArrayList<Value>();
            listResources.add( factory.createIRI(strResource) );
        }
        return listResources;
    }

	@Override
	protected void writeNode(JsonGenerator writer) throws JsonGenerationException, IOException {
		writer.writeStringField("nodeType", ConstantResourceNode.NODETYPE);
        writer.writeStringField("value", strIRI);
	}
}
