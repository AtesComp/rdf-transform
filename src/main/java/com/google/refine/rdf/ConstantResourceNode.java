package com.google.refine.rdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.refine.model.Project;

import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConstantResourceNode extends ResourceNode {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:ConstResNode");

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
    public List<Value> createResources(ParsedIRI baseIRI, ValueFactory factory,
                                        RepositoryConnection connection,Project project)
    {
        this.baseIRI = baseIRI;
        this.theFactory = factory;
        this.theConnection = connection;
        this.theProject = project;

        List<Value> listResources = null;
        String strResource = null;
        if (Util.isDebugMode()) logger.info("DEBUG: Given IRI: " + this.strIRI);
        String strResult = Util.toSpaceStrippedString(this.strIRI);
        if (Util.isDebugMode()) logger.info("DEBUG: strResult: " + strResult);
        if (strResult != null & strResult.length() > 0 ) {
            try {
                strResource = Util.resolveIRI(this.baseIRI, this.strIRI);
            }
            catch (Util.IRIParsingException ex) {
                // ...continue...
            }
            if (strResource != null) {
                strResource = this.expandPrefixedIRI(strResource);
                if (Util.isDebugMode()) logger.info("DEBUG: strResource: " + strResource);
                listResources = new ArrayList<Value>();
                listResources.add( this.theFactory.createIRI(strResource) );
            }
        }
        return listResources;
    }

	@Override
	protected void writeNode(JsonGenerator writer) throws JsonGenerationException, IOException {
		writer.writeStringField("nodeType", ConstantResourceNode.NODETYPE);
        writer.writeStringField("value", strIRI);
	}
}
