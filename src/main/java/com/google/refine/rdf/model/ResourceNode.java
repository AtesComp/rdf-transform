package com.google.refine.rdf.model;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

import com.google.refine.model.Project;
import com.google.refine.model.Record;
import com.google.refine.rdf.model.Util.IRIParsingException;
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

    @JsonProperty("propertyMappings")
    private List<Property> listProperties = new ArrayList<Property>();

	@JsonProperty("typeMappings")
    private List<RDFType> listTypes = new ArrayList<RDFType>();

    @JsonIgnore
    protected List<Value> listResources = null;

    @JsonIgnore
    public void addType(RDFType typeNew) {
        this.listTypes.add(typeNew);
    }

    @JsonIgnore
    public void addProperty(Property propNew) {
        this.listProperties.add(propNew);
    }

    @JsonProperty("typeMappings")
    public List<RDFType> getTypes() {
        return this.listTypes;
    }

    @JsonProperty("propertyMappings")
    public List<Property> getProperties() {
		return this.listProperties;
	}

    /*
     *  Method normalizeResource() for Resource Node to IRI
     */
    protected void normalizeResource(Object objResult) {
        String strIRI = Util.toSpaceStrippedString(objResult);
        if ( Util.isDebugMode() ) ResourceNode.logger.info("DEBUG: normalizeResource: Given IRI: " + strIRI);
        if ( ! ( strIRI == null || strIRI.isEmpty() ) ) {
            try {
                String strResource = Util.resolveIRI(this.baseIRI, strIRI);
                if (strResource != null) {
                    strResource = this.expandPrefixedIRI(strResource);
                    if (Util.isDebugMode()) ResourceNode.logger.info("DEBUG: normalizeResource: Processed IRI: " + strResource);
                    this.listResources.add( this.theFactory.createIRI(strResource) );
                }
            }
            catch (IRIParsingException | IllegalArgumentException ex) {
                // An IRIParsingException from Util.resolveIRI() means a bad IRI.
                // An IllegalArgumentException from theFactory.createIRI() means a bad IRI.
                // In either case, record error and eat the exception...
                ResourceNode.logger.error( "ERROR: Bad IRI: " + strIRI, ex);
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
     *  Return: void
     * 
     *  Stores the Resources as generic Values since these are "object" elements in
     *    ( source, predicate, object ) triples and need to be compatible with literals.
     */
    private void createStatementsWorker()
            throws RuntimeException {
        if (Util.isDebugMode()) logger.info("DEBUG: createStatementsWorker...");

        //
        // Transition from Record to Row processing...
        //
        if ( this.theRec.isRecordPerRow() ) {
            List<Value> listResourcesAll = new ArrayList<Value>();
            while ( this.theRec.rowNext() ) {
                this.createRowResources(); // ...Row only
                if ( ! ( this.listResources == null || this.listResources.isEmpty() ) ) {
                    this.createResourceStatements(); // ...relies on this.listResources iteration
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
            this.createResources(); // ...Record or Row
            if ( ! ( this.listResources == null || this.listResources.isEmpty() ) ) {
                this.createResourceStatements();
            }
            else {
                this.listResources = null;
            }
        }
    }

    /*
     *  Method createResource() for Resource Node types
     *
     *  Return: List<Value>
     *    Returns the Resources as generic Values since these are "object" elements in
     *    ( source, predicate, object ) triples and need to be compatible with literals.
     */
    protected void createResources() {
        if (Util.isDebugMode()) logger.info("DEBUG: createResources...");

        // TODO: Create process for Sub-Records

        //
        // Record Mode
        //
        if ( this.theRec.isRecordMode() ) {
            // If a column node, the node should iterate all records in the Record group...
            if ( ! this.bIsIndex ) {
                this.createRecordResources();
            }
            // Otherwise, we only need to get a single "Record Number" resource for the Record group...
            else {
                this.theRec.rowNext(); // ...set index for first (or any) row in the Record
                this.createRowResources(); // ...get the one resource
                this.theRec.rowReset(); // ...reset for any other row run on the Record
            }
        }
        //
        // Row Mode
        //
        else {
            this.createRowResources();
        }
    }

    protected void createRecordResources() {
        // TODO: For blank nodes, one per Record+Column is enough?  Review to limit! HINT: see createResources() -> (! this.bIsIndex)
        if (Util.isDebugMode()) logger.info("DEBUG: createRecordResources...");

        this.listResources = null;
		List<Value> listResourcesAll = new ArrayList<Value>();
		while ( this.theRec.rowNext() ) {
			this.createRowResources();
            if ( ! ( this.listResources == null || this.listResources.isEmpty() ) ) {
				listResourcesAll.addAll(this.listResources);
			}
		}
        if ( listResourcesAll.isEmpty() ) {
            listResourcesAll = null;
        }
        this.listResources = listResourcesAll;
}

    abstract protected void createRowResources();

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
            this.createPropertyStatements();
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
                catch (IRIParsingException | IllegalArgumentException ex) {
                    logger.error( "ERROR: Bad Property IRI: " + strResult, ex);
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
    private void createPropertyStatements()
            throws RepositoryException {
        if ( Util.isDebugMode() ) {
            String strLinkCount = "DEBUG: Link Count: {}";
            int iLinkCount = 0;
            if (this.listProperties != null) {
                iLinkCount = listProperties.size();
            }
            ResourceNode.logger.info(strLinkCount, iLinkCount);
        }
        if (this.listProperties == null) {
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
            private IRI iriProp;
            private List<Value> listObjs;

            PropertyObjectList(IRI iriProp, List<Value> listObjs) {
                this.iriProp = iriProp;
                this.listObjs = listObjs;
            }
            public IRI getProperty() {
                return this.iriProp;
            }
            public List<Value> getObjects() {
                return this.listObjs;
            }
        }

        //
        // Process one set of properties
        //
        List<PropertyObjectList> listPOL = new ArrayList<PropertyObjectList>();
        for (Property propItem : this.listProperties) {
            //
            // OBJECTS
            //
            nodeObject = propItem.getObject();
            if (nodeObject == null) {
                continue;
            }
            listObjects = nodeObject.createObjects(this);
            if (listObjects == null) {
                continue;
            }

            //
            // PROPERTY
            //

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
            strTypeProperty = propItem.getProperty();
            if (Util.isDebugMode()) ResourceNode.logger.info("DEBUG: Prop: " + strTypeProperty);
            String strResult = Util.toSpaceStrippedString(strTypeProperty);
            if (Util.isDebugMode()) ResourceNode.logger.info("DEBUG: Prop Result: " + strResult);
            if (strResult != null & strResult.length() > 0 ) {
                try {
                    strProperty = Util.resolveIRI(this.baseIRI, strResult);
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
                        listPOL.add( new PropertyObjectList(iriProperty, listObjects) );
                    }
                }
                catch (IRIParsingException | IllegalArgumentException ex) {
                    logger.error( "ERROR: Bad Property IRI: " + strResult, ex);
                }
            }
        }

        //
        // Process statements...
        //
        for (Value valSource : this.listResources) {
            for ( PropertyObjectList polObj : listPOL )
            {
                iriProperty = polObj.getProperty();
                listObjects = polObj.getObjects();
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

        // Record Mode...
		if ( nodeProperty.theRec.isRecordMode() ) {
			// ...set to Row Mode and process on current row as set by rowNext()...
			this.theRec.setMode(nodeProperty, true);
        }
        // Row Mode...
        else {
			// ...process on current row as set by rowNext()...
			this.theRec.setMode(nodeProperty);
        }
        this.createStatementsWorker();
        this.theRec.clear();

        // Return the collected resources from the statement processing as Objects
        // to the given Property...
        return this.listResources;
    }

    abstract protected void writeNode(JsonGenerator writer) throws JsonGenerationException, IOException;

    public void write(JsonGenerator writer) throws JsonGenerationException, IOException {
        writer.writeStartObject();

        // Write node...
        this.writeNode(writer);

        // Write Type Mappings...
        writer.writeFieldName("typeMappings");
        writer.writeStartArray();
        for ( RDFType typeObj : this.listTypes ) {
            writer.writeStartObject();
            writer.writeStringField("iri",   typeObj.getResource());
            writer.writeStringField("cirie", typeObj.getPrefixedResource());
            writer.writeEndObject();
        }
        writer.writeEndArray();

        // Write Property Mappings...
        writer.writeFieldName("propertyMappings");
        writer.writeStartArray();
        for (Property propObj : this.listProperties) {
            propObj.write(writer);
        }
        writer.writeEndArray();

        writer.writeEndObject();
    }
}
