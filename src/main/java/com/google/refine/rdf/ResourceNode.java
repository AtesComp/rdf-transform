package com.google.refine.rdf;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

import com.google.refine.model.Project;
import com.google.refine.model.Record;

import com.google.refine.rdf.Util.IRIParsingException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
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
    private List<Value> createStatementsWorker()
            throws RuntimeException {
        if (Util.isDebugMode()) logger.info("DEBUG: createStatementsWorker...");

        //
        // Transition from Record to Row processing...
        //
        if ( this.theRec.isRecordPerRow() ) {
            List<Value> listResourcesAll = new ArrayList<Value>();
            while ( this.theRec.rowNext() ) {
                this.listResources = this.createRowResources(); // ...Row only
                if ( ! ( this.listResources == null || this.listResources.isEmpty() ) ) {
                    this.createResourceStatements();
                    listResourcesAll.addAll(this.listResources);
                }
            }
            if ( listResourcesAll.isEmpty() ) {
                listResourcesAll = null;
            }
            this.listResources = listResourcesAll;
        }

        //
        // Standard Record or Row processing...
        //
        else {
            this.listResources = this.createResources(); // ...Record or Row
            if ( ! ( this.listResources == null || this.listResources.isEmpty() ) ) {
                this.createResourceStatements();
            }
            else {
                this.listResources = null;
            }
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
    abstract protected List<Value> createRecordResources();
    abstract protected List<Value> createRowResources();

    /*
     *  Method createStatements() for Resource Node types
     * 
     *    Given a set of source resources, create the (source, rdf:type, object) triple statements
     *    for each of the sources.
     */
    private void createResourceStatements()
            throws RuntimeException {
        try {
            this.createTypeStatements();
            this.createLinkStatements();
        }
        catch (RepositoryException ex) {
            throw new RuntimeException(ex);
        }
    }

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

        //
        // Process one set of object types
        //
        List<IRI> listTypes = new ArrayList<IRI>();
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
            if (Util.isDebugMode()) ResourceNode.logger.info("DEBUG: Type: " + strTypeObject);
            String strResult = Util.toSpaceStrippedString(strTypeObject);
            if (Util.isDebugMode()) ResourceNode.logger.info("DEBUG: Type Result: " + strResult);
            if (strResult != null & strResult.length() > 0 ) {
                try {
                    strResource = Util.resolveIRI(this.baseIRI, strResult);
                }
                catch (Util.IRIParsingException ex) {
                    // ...continue...
                }
                if (strResource != null) {
                    strResource = this.expandPrefixedIRI(strResource);
                    if (Util.isDebugMode()) ResourceNode.logger.info("DEBUG: Type Resource: " + strResource);
                    //if (bNamespace) {
                    //    iriResource = this.theFactory.createIRI(strNamespace, strLocalName);
                    //}
                    //else {
                    //    iriResource = this.theFactory.createIRI(strResource);
                    //}
                    iriResource = this.theFactory.createIRI(strResource);
                    listTypes.add(iriResource);
                }
    		}
    	}

        //
        // Process statements...
        //
        for (Value valSource : this.listResources) {
            for (IRI iriType : listTypes) {
                this.theConnection.add(
                    this.theFactory.createStatement(
                        (Resource) valSource, RDF.TYPE, iriType
                    )
                );
            }
        }
    }

    /*
     *  Method createLinkStatements() for Resource Node types on OpenRefine Rows
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
        List<Value> listObjects = null;

        @JsonIgnoreType
        class PropertyObjectList {
            private IRI prop;
            private List<Value> objs;

            PropertyObjectList(IRI prop, List<Value> objs) {
                this.prop = prop;
                this.objs = objs;
            }
            public IRI getProperty() {
                return this.prop;
            }
            public List<Value> getObjects() {
                return this.objs;
            }
        }

        //
        // Process one set of links
        //
        List<PropertyObjectList> polistLinks = new ArrayList<PropertyObjectList>();
        for (Link link : this.listLinks) {
            // OBJECTS
            nodeObject = link.getObject();
            if (nodeObject == null) {
                continue;
            }
            listObjects = nodeObject.createObjects(this);
            if (listObjects == null) {
                continue;
            }

            // PROPERTY

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
            if (Util.isDebugMode()) ResourceNode.logger.info("DEBUG: Prop: " + strTypeProperty);
            String strResult = Util.toSpaceStrippedString(strTypeProperty);
            if (Util.isDebugMode()) ResourceNode.logger.info("DEBUG: Prop Result: " + strResult);
            if (strResult != null & strResult.length() > 0 ) {
                try {
                    strProperty = Util.resolveIRI(this.baseIRI, strResult);
                }
                catch (Util.IRIParsingException ex) {
                    // ...continue...
                }
                if (strProperty != null) {
                    strProperty = this.expandPrefixedIRI(strProperty);
                    if (Util.isDebugMode()) ResourceNode.logger.info("DEBUG: Prop Resource: " + strProperty);
                    //if (bNamespace) {
                    //    iriProperty = this.theFactory.createIRI(strNamespace, strLocalName);
                    //}
                    //else {
                    //    iriProperty = this.theFactory.createIRI(strProperty);
                    //}
                    iriProperty = this.theFactory.createIRI(strProperty);
                    polistLinks.add( new PropertyObjectList(iriProperty, listObjects) );

                }
            }
        }

        //
        // Process statements...
        //
        for (Value valSource : this.listResources) {
            for ( PropertyObjectList polist : polistLinks )
            {
                iriProperty = polist.getProperty();
                listObjects = polist.getObjects();
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

    /*
     *  Method createObjects() for Resource Node types on OpenRefine Rows
     *
     *  Return: List<Value>
     *    Returns the Resources as generic Values since these are "object" elements in
     *    ( source, predicate, object ) triples and need to be compatible with literals.
     */
    protected List<Value> createObjects(ResourceNode nodeProperty)
            throws RuntimeException {
        this.baseIRI = nodeProperty.baseIRI;
        this.theFactory = nodeProperty.theFactory;
        this.theConnection = nodeProperty.theConnection;
        this.theProject = nodeProperty.theProject;

        // TODO: Create process for Sub-Records

        //
        // Record Mode
        //
		if ( nodeProperty.theRec.isRecordMode() ) {
			// ...set to Row Mode and process on current row as set by rowNext()...
			this.theRec.setLink(nodeProperty, true);
        }
        //
        // Row Mode
        //
        else {
			// ...process on current row as set by rowNext()...
			this.theRec.setLink(nodeProperty);
        }
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
