package com.google.refine.rdf;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

import com.google.refine.model.Project;
import com.google.refine.model.Record;
import com.google.refine.rdf.Util.IRIParsingException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerationException;

import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract public class ResourceNode extends Node {
	private final static Logger logger = LoggerFactory.getLogger("RDFT:ResNode");

	@JsonProperty("rdfTypes")
    private List<RDFType> listRDFTypes = new ArrayList<RDFType>();

    @JsonProperty("links")
    private List<Link> listLinks = new ArrayList<Link>();

    @JsonIgnore
    private List<Value> listResources = null;

    @JsonIgnore
    public void addType(RDFType type) {
        this.listRDFTypes.add(type);
    }

    @JsonIgnore
    public void addLink(Link link) {
        this.listLinks.add(link);
    }

    @JsonProperty("rdfTypes")
    public List<RDFType> getTypes() {
        return this.listRDFTypes;
    }

    @JsonProperty("links")
    public List<Link> getLinks() {
		return this.listLinks;
	}

    /*
     *  Method normalizeResource() for Resource Node to IRI
     */
    protected void normalizeResource(Object objResult, List<Value> listResource) {
        String strIRI = Util.toSpaceStrippedString(objResult);
        if (Util.isDebugMode()) ResourceNode.logger.info("DEBUG: normalizeResource: Given IRI: " + strIRI);
        if ( strIRI != null && ! strIRI.isEmpty() ) {
            try {
                String strResource = Util.resolveIRI(this.baseIRI, strIRI);
                if (strResource != null) {
                    strResource = this.expandPrefixedIRI(strResource);
                    if (Util.isDebugMode()) ResourceNode.logger.info("DEBUG: normalizeResource: Processed IRI: " + strResource);
                    listResource.add( this.theFactory.createIRI(strResource) );
                }
            }
            catch (IRIParsingException | IllegalArgumentException ex) {
                // An IRIParsingException from Util.resolveIRI() means a bad IRI.
                // An IllegalArgumentException from theFactory.createIRI() means a bad IRI.
                // In either case, record error and eat the exception...
                ResourceNode.logger.error( "ERROR: Bad IRI: " + strIRI );
                ResourceNode.logger.error( "ERROR: Bad IRI: " + ex.getMessage() );
            }
        }
    }

    /*
     *  Method createStatements() for Root Resource Node types on OpenRefine Rows
     */
    public void createStatements(
                        ParsedIRI baseIRI, ValueFactory theFactory, RepositoryConnection theConnection,
                        Project theProject, int iRowIndex )
            throws RuntimeException
    {
        this.baseIRI = baseIRI;
        this.theFactory = theFactory;
        this.theConnection = theConnection;
        this.theProject = theProject;

        this.theRec.setRootRow(iRowIndex);
        this.createStatementsWorker();
        this.theRec.clear();
    }

    /*
     *  Method createStatements() for Root Resource Node types on OpenRefine Records
     */
    public void createStatements(
                        ParsedIRI baseIRI, ValueFactory theFactory, RepositoryConnection theConnection,
                        Project theProject, Record theRecord )
            throws RuntimeException
    {
        this.baseIRI = baseIRI;
        this.theFactory = theFactory;
        this.theConnection = theConnection;
        this.theProject = theProject;

        this.theRec.setRootRecord(theRecord);
        this.createStatementsWorker();
        this.theRec.clear();
    }

    /*
     *  Method createStatementsWorker() for Resource Node types
     *
     *  Return: List<Value>
     *    Returns the Resources as generic Values since these are "object" elements in
     *    ( source, predicate, object ) triples and need to be compatible with literals.
     */
    private List<Value> createStatementsWorker() {
        this.listResources = this.createResources();
        if ( Util.isDebugMode() ) {
            String strResCount = "DEBUG: Resources Count: {}";
            int iResCount = 0;
            if (this.listResources != null) {
                iResCount = this.listResources.size();
            }
            ResourceNode.logger.info(strResCount, iResCount);
        }
        if ( this.listResources == null || this.listResources.isEmpty() ) {
            return null;
        }

        try {
            this.createTypeStatements();
            this.createLinkStatements();
        }
        catch (RepositoryException ex) {
            throw new RuntimeException(ex);
        }

        return this.listResources;
    }

    /*
     *  Method createResource() for Resource Node types
     *
     *  Return: List<Value>
     *    Returns the Resources as generic Values since these are "object" elements in
     *    ( source, predicate, object ) triples and need to be compatible with literals.
     */
    abstract protected List<Value> createResources();

    /*
     *  Method createTypeStatements() for Resource Node types
     * 
     *    Given a set of source resources, create the (source, rdf:type, object) triple statements
     *    for each of the sources.
     */
    private void createTypeStatements()
            throws RepositoryException {
        String strResource = null;
        String strTypeObject = null;
        //String strIRI = null;
        //String strCIRIE = null;
        //String strPrefix = null;
        //String strNamespace = null;
        //String strLocalName = null;
        //boolean bNamespace = false;
        IRI iriResource = null;
        for ( RDFType typeObject : this.getTypes() ) {
            //bNamespace = false;
            //strIRI = typeObject.getResource();
            //strCIRIE = typeObject.getPrefixedResource();
            //strTypeObject = strCIRIE;
            //if (strTypeObject == null) {
            //    strTypeObject = strIRI;
            //    //strPrefix = "";
            //    strLocalName = "";
            //    strNamespace = "";
            //}
            //else { // ...prefixed...
            //    int iIndex = strCIRIE.indexOf(":") + 1;
            //    //strPrefix = strCIRIE.substring(0, iIndex);
            //    strLocalName = strCIRIE.substring(iIndex);
            //    strNamespace = strCIRIE.replace(strLocalName, "");
            //    bNamespace = true;
            //}
            iriResource = null;
            strResource = null;
            strTypeObject = typeObject.getResource();
            if (Util.isDebugMode()) ResourceNode.logger.info("DEBUG: Given Type: " + strTypeObject);
            String strResult = Util.toSpaceStrippedString(strTypeObject);
            if (Util.isDebugMode()) ResourceNode.logger.info("DEBUG: strResult: " + strResult);
            if (strResult != null & strResult.length() > 0 ) {
                try {
                    strResource = Util.resolveIRI(this.baseIRI, strResult);
                }
                catch (Util.IRIParsingException ex) {
                    // ...continue...
                }
                if (strResource != null) {
                    strResource = this.expandPrefixedIRI(strResource);
                    if (Util.isDebugMode()) ResourceNode.logger.info("DEBUG: strResource: " + strResource);
                    //if (bNamespace) {
                    //    iriResource = factory.createIRI(strNamespace, strLocalName);
                    //}
                    //else {
                    //    iriResource = this.theFactory.createIRI(strResource);
                    //}
                    iriResource = this.theFactory.createIRI(strResource);

                    for (Value valSource : this.listResources) {
                        this.theConnection.add(
                            this.theFactory.createStatement(
                                (Resource) valSource, RDF.TYPE, iriResource
                            )
                        );
                    }
                }
    		}
    	}
    }

    /*
     *  Method createLinkStatements() for Root Resource Node types on OpenRefine Rows
     * 
     *    Given a set of source resources, create the (source, property, object) triple statements
     *    for each of the sources.
     */
    private void createLinkStatements()
            throws RepositoryException {
        if ( Util.isDebugMode() ) {
            String strLinkCount = "DEBUG: Link Count: {}";
            int iLinkCount = 0;
            if (this.listLinks != null) {
                iLinkCount = listLinks.size();
            }
            ResourceNode.logger.info(strLinkCount, iLinkCount);
        }
        if (this.listLinks == null) {
            return;
        }
        String strProperty = null;
        String strTypeProperty = null;
        //String strIRI = null;
        //String strCIRIE = null;
        //String strPrefix = null;
        //String strNamespace = null;
        //String strLocalName = null;
        //boolean bNamespace = false;
        Node nodeObject = null;
        IRI iriProperty = null;
        for (Link link : this.listLinks) {
            nodeObject = link.getObject();
            if (nodeObject == null) {
                continue;
            }
            List<Value> listObjects = nodeObject.createObjects(this);
            if (listObjects == null) {
                continue;
            }
            //bNamespace = false;
            //strIRI = link.getProperty();
            //strCIRIE = link.getPrefixedProperty();
            //strTypeProperty = strCIRIE;
            //if (strTypeProperty == null) {
            //    strTypeProperty = strIRI;
            //}
            //else {
            //    int iIndex = strCIRIE.indexOf(":") + 1;
            //    //strPrefix = strCIRIE.substring(0, iIndex);
            //    strLocalName = strCIRIE.substring(iIndex);
            //    strNamespace = strCIRIE.replace(strLocalName, "");
            //    bNamespace = true;
            //}
            iriProperty = null;
            strProperty = null;
            strTypeProperty = link.getProperty();
            if (Util.isDebugMode()) ResourceNode.logger.info("DEBUG: Given Property: " + strTypeProperty);
            String strResult = Util.toSpaceStrippedString(strTypeProperty);
            if (Util.isDebugMode()) ResourceNode.logger.info("DEBUG: strResult: " + strResult);
            if (strResult != null & strResult.length() > 0 ) {
                try {
                    strProperty = Util.resolveIRI(this.baseIRI, strResult);
                }
                catch (Util.IRIParsingException ex) {
                    // ...continue...
                }
                if (strProperty != null) {
                    strProperty = this.expandPrefixedIRI(strProperty);
                    if (Util.isDebugMode()) ResourceNode.logger.info("DEBUG: strProperty: " + strProperty);
                    //if (bNamespace) {
                    //    iriProperty = factory.createIRI(strNamespace, strLocalName);
                    //}
                    //else {
                    //    iriProperty = this.theFactory.createIRI(strProperty);
                    //}
                    iriProperty = this.theFactory.createIRI(strProperty);

                    for (Value valSource : this.listResources) {
                        for (Value valObject : listObjects) {
                            this.theConnection.add(
                                this.theFactory.createStatement(
                                    (Resource) valSource, iriProperty, valObject
                                )
                            );
                        }
                    }
                }
            }
       	}
    }

    /*
     *  Method createObjects() for Resource Node types on OpenRefine Rows
     *
     *  Return: List<Value>
     *    Returns the Resources as generic Values since these are "object" elements in
     *    ( source, predicate, object ) triples and need to be compatible with literals.
     */
    protected List<Value> createObjects(ResourceNode nodeParent)
            throws RuntimeException {
        this.baseIRI = nodeParent.baseIRI;
        this.theFactory = nodeParent.theFactory;
        this.theConnection = nodeParent.theConnection;
        this.theProject = nodeParent.theProject;

        // TODO: Convert from Record to Row unless specifed as a Sub-Record
        // TODO: Create findSubRecord()

        this.theRec.setRowRecord(nodeParent);
        List<Value> listObjects = this.createStatementsWorker();
        this.theRec.clear();

        return listObjects;
    }

    abstract protected void writeNode(JsonGenerator writer) throws JsonGenerationException, IOException;

    public void write(JsonGenerator writer) throws JsonGenerationException, IOException {
        writer.writeStartObject();

        // Write node...
        this.writeNode(writer);

        // Write types...getClass
        writer.writeFieldName("rdfTypes");
        writer.writeStartArray();
        for ( RDFType type : this.getTypes() ) {
            writer.writeStartObject();
            writer.writeStringField("iri",   type.getResource());
            writer.writeStringField("cirie", type.getPrefixedResource());
            writer.writeEndObject();
        }
        writer.writeEndArray();

        // Write links...
        writer.writeFieldName("links");
        writer.writeStartArray();
        for (Link link : this.listLinks) {
            link.write(writer);
        }
        writer.writeEndArray();

        writer.writeEndObject();
    }
}
