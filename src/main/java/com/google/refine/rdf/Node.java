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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.CUSTOM,
    include = JsonTypeInfo.As.PROPERTY,
    property = "nodeType")
@JsonTypeIdResolver(NodeResolver.class)
abstract public class Node {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:Node");

    protected ParsedIRI baseIRI = null;
    protected ValueFactory theFactory = null;
    protected RepositoryConnection theConnection = null;
    protected Project theProject = null;

    public abstract String getNodeName();

    @JsonProperty("nodeType")
    public abstract String getNodeType();

    protected String expandPrefixedIRI(String strObjectIRI) {
        String strExpanded = strObjectIRI;
        if (Util.isDebugMode()) logger.info("DEBUG: expandPrefixedIRI: string = " + strObjectIRI);
        if ( !strObjectIRI.contains("://") ) {
            if (Util.isDebugMode()) logger.info("DEBUG: expandPrefixedIRI: checking prefix...");
            int iIndex = strObjectIRI.indexOf(':');
            if (Util.isDebugMode()) logger.info("DEBUG: expandPrefixedIRI: index = " + iIndex);
            if (iIndex >= 0) {
                String strPrefix = strObjectIRI.substring(0, iIndex);
                if (Util.isDebugMode()) logger.info("DEBUG: expandPrefixedIRI: strPrefix = " + strPrefix);
                if (Util.isDebugMode()) logger.info("DEBUG: expandPrefixedIRI: connection = " +
                                                    ( this.theConnection == null ? "null" : "connected" ) );
                String strNamespace = this.theConnection.getNamespace(strPrefix);
                if (Util.isDebugMode()) logger.info("DEBUG: expandPrefixedIRI: strNamespace = " + strNamespace);
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
