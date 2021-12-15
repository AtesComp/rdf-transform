package com.google.refine.rdf;

import java.io.IOException;
import java.util.List;

import com.google.refine.model.Project;
import com.google.refine.rdf.utils.RecordModel;

import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    static private final Logger logger = LoggerFactory.getLogger("RDFT:Node");

    @JsonIgnore
    protected ParsedIRI baseIRI = null;
    @JsonIgnore
    protected ValueFactory theFactory = null;
    @JsonIgnore
    protected RepositoryConnection theConnection = null;
    @JsonIgnore
    protected Project theProject = null;

    @JsonIgnore
    public RecordModel theRec = null;

    @JsonIgnore
    public Node() {
        theRec = new RecordModel(this);
    }

    @JsonIgnore
    abstract public String getNodeName();

    @JsonProperty("nodeType")
    abstract public String getNodeType();

    @JsonIgnore
    public Project getProject() {
        return theProject;
    }

    protected String expandPrefixedIRI(String strObjectIRI) {
        String strExpanded = strObjectIRI;
        if (Util.isDebugMode()) logger.info("DEBUG: expandPrefixedIRI: string = " + strObjectIRI);
        if ( !strObjectIRI.contains("://") ) {
            if (Util.isDebugMode()) logger.info("DEBUG: expandPrefixedIRI: checking prefix...");
            int iIndex = strObjectIRI.indexOf(':'); // ...get index of first ':'...
            if (Util.isDebugMode()) logger.info("DEBUG: expandPrefixedIRI: index = " + iIndex);
            if (iIndex >= 0) { // ...a ':' exists...
                String strPrefix = strObjectIRI.substring(0, iIndex); // ...including blank ("") prefix...
                if (Util.isDebugMode()) logger.info("DEBUG: expandPrefixedIRI: strPrefix = " + strPrefix);
                if (Util.isDebugMode()) logger.info("DEBUG: expandPrefixedIRI: connection = " +
                                                    ( this.theConnection == null ? "null" : "connected" ) );
                String strNamespace = this.theConnection.getNamespace(strPrefix);
                if (Util.isDebugMode()) logger.info("DEBUG: expandPrefixedIRI: strNamespace = " + strNamespace);
                if (strNamespace != null) {
                     // Get the string just beyond the first ':'...
                    strExpanded = strNamespace + strObjectIRI.substring(iIndex+1);
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
    abstract protected List<Value> createObjects(ResourceNode nodeProperty);

    abstract public void write(JsonGenerator writer)
            throws JsonGenerationException, IOException;
}
