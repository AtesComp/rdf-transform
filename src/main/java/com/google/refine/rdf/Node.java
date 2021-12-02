package com.google.refine.rdf;

import java.io.IOException;
import java.util.List;

import com.google.refine.model.Project;

import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerationException;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.CUSTOM,
    include = JsonTypeInfo.As.PROPERTY,
    property = "nodeType")
@JsonTypeIdResolver(NodeResolver.class)
abstract public class Node {
    protected ParsedIRI baseIRI;
    protected ValueFactory theFactory;
    protected RepositoryConnection theConnection;
    protected Project theProject;

    public abstract String getNodeName();

    @JsonProperty("nodeType")
    public abstract String getNodeType();

    protected String expandPrefixedIRI(String strObjectIRI) {
        String strExpanded = strObjectIRI;
        if ( !strObjectIRI.contains("://") ) {
            int iIndex = strObjectIRI.indexOf(":");
            if (iIndex >= 0) {
                String strPrefix = strObjectIRI.substring(0, iIndex);
                String strNamespace = this.theConnection.getNamespace(strPrefix);
                if (strNamespace != null) {
                    strExpanded = strNamespace + strObjectIRI.substring(iIndex);
                }
            }
        }
        return strExpanded;
    }

    /*
     *  Method createObjects()
     *
     *    Creates the object list for triple statements from this node.
     */
    protected abstract List<Value> createObjects(ParsedIRI baseIRI, ValueFactory theFactory,
                        RepositoryConnection theConnection, Project theProject,
                        ResourceNode nodeParent);

    public abstract void write(JsonGenerator writer)
            throws JsonGenerationException, IOException;
}
