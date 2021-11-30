package com.google.refine.rdf;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

import com.google.refine.model.Project;
import com.google.refine.model.Record;
import com.google.refine.model.Row;

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

    private Row theRow = null;
    private int iRowIndex = -1;
    private Record theRecord = null;

    public void addType(RDFType type) {
        this.listRDFTypes.add(type);
    }

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

    public Row getRow() {
        return this.theRow;
    }

    public int getRowIndex() {
        return this.iRowIndex;
    }

    public Record getRecord() {
        return this.theRecord;
    }

    private void setRowRecord(ResourceNode nodeParent) {
        this.theRow = nodeParent.getRow();
        this.iRowIndex = nodeParent.getRowIndex();
        this.theRecord = nodeParent.getRecord();
    }

    private void resetRowRecord() {
        resetRow();
        resetRecord();
    }

    private void resetRow() {
        this.theRow = null;
        this.iRowIndex = -1;
    }

    private void resetRecord() {
        this.theRecord = null;
    }

    /*
     *  Method createTypeStatements() for Resource Node types
     * 
     *    Given a set of source resources, create the (source, rdf:type, object) triple statements
     *    for each of the sources.
     */
    private void createTypeStatements(
                        List<Value> listSources, ValueFactory factory, RepositoryConnection connection,
                        ParsedIRI baseIRI )
            throws RepositoryException {
        String strObjectIRI = null;
        String strTypeObject = null;
        String strIRI = null;
        String strCIRIE = null;
        //String strPrefix = null;
        String strNamespace = null;
        String strLocalName = null;
        boolean bNamespace = false;
        IRI iriObject = null;
        for ( RDFType typeObject : this.getTypes() ) {
            bNamespace = false;
            strIRI = typeObject.getResource();
            strCIRIE = typeObject.getPrefixedResource();
            strTypeObject = strCIRIE;
            if (strTypeObject == null) {
                strTypeObject = strIRI;
                //strPrefix = "";
                strLocalName = "";
                strNamespace = "";
            }
            else { // ...prefixed...
                int iIndex = strCIRIE.indexOf(":") + 1;
                //strPrefix = strCIRIE.substring(0, iIndex);
                strLocalName = strCIRIE.substring(iIndex);
                strNamespace = strCIRIE.replace(strLocalName, "");
                bNamespace = true;
            }
            strObjectIRI = null;
            try {
                strObjectIRI = Util.resolveIRI(baseIRI, strTypeObject);
            }
            catch (Util.IRIParsingException ex) {
                // ...continue...
            }
            if (strObjectIRI == null) {
                continue; // ...to next type
            }
            if ( Util.isVerbose(4) )
                logger.info("Type IRI: " + strObjectIRI);

            if (bNamespace) {
                iriObject = factory.createIRI(strNamespace, strLocalName);
            }
            else {
                iriObject = factory.createIRI(strObjectIRI);
            }
            for (Value valSource : listSources) {
                connection.add( factory.createStatement( (Resource) valSource, RDF.TYPE, iriObject ) );
    		}
    	}
    }

    /*
     *  Method createLinkStatements() for Root Resource Node types on OpenRefine Rows
     * 
     *    Given a set of source resources, create the (source, property, object) triple statements
     *    for each of the sources.
     */
    private void createLinkStatements(
                        List<Value> listSources, ParsedIRI baseIRI, ValueFactory factory,
                        RepositoryConnection connection, Project project)
            throws RepositoryException {
        if ( Util.isVerbose(4) ) {
            if (this.listLinks == null)
                logger.info("Link Count: NULL (No Links)");
            else
                logger.info("Link Count: " + listLinks.size());
        }
        String strPropertyIRI = null;
        String strTypeProperty = null;
        String strIRI = null;
        String strCIRIE = null;
        //String strPrefix = null;
        String strNamespace = null;
        String strLocalName = null;
        boolean bNamespace = false;
        Node nodeObject = null;
        IRI iriProperty = null;
        for (Link link : this.listLinks) {
            bNamespace = false;
            strIRI = link.getProperty();
            strCIRIE = link.getPrefixedProperty();
            strTypeProperty = strCIRIE;
            if (strTypeProperty == null) {
                strTypeProperty = strIRI;
            }
            else {
                int iIndex = strCIRIE.indexOf(":") + 1;
                //strPrefix = strCIRIE.substring(0, iIndex);
                strLocalName = strCIRIE.substring(iIndex);
                strNamespace = strCIRIE.replace(strLocalName, "");
                bNamespace = true;
            }
            strPropertyIRI = null;
            try {
                strPropertyIRI = Util.resolveIRI(baseIRI, strTypeProperty);
            }
            catch (Util.IRIParsingException ex) {
                // ...continue...
            }
            if (strPropertyIRI == null) {
                continue; // ...to next property
            }
            if ( Util.isVerbose(4) )
                logger.info("Link IRI: " + strPropertyIRI);

            if (bNamespace) {
                iriProperty = factory.createIRI(strNamespace, strLocalName);
            }
            else {
                iriProperty = factory.createIRI(strPropertyIRI);
            }
            nodeObject = link.getObject();
            if (nodeObject != null)
            {
                List<Value> listObjects = nodeObject.createObjects(baseIRI, factory, connection, project, this);
                if (listObjects != null) {
                    for (Value valSource : listSources) {
                        for (Value valObject : listObjects) {
           		    		connection.add( factory.createStatement( (Resource) valSource, iriProperty, valObject ) );
           			    }
           		    }
           	    }
            }
       	}
    }

    /*
     *  Method createStatements() for Root Resource Node types on OpenRefine Rows
     */
    public void createStatements(
                        ParsedIRI baseIRI, ValueFactory factory, RepositoryConnection connection,
                        Project project, Row theRow, int iRowIndex )
            throws RuntimeException
    {
        this.theRow = theRow;
        this.iRowIndex = iRowIndex;
        this.createStatementsWorker(baseIRI, factory, connection, project);
        this.resetRow();
    }

    /*
     *  Method createStatements() for Root Resource Node types on OpenRefine Records
     */
    public void createStatements(
                        ParsedIRI baseIRI, ValueFactory factory, RepositoryConnection connection,
                        Project project, Record theRecord )
            throws RuntimeException
    {
        this.theRecord = theRecord;
        this.createStatementsWorker(baseIRI, factory, connection, project);
        this.resetRecord();
    }

    /*
     *  Method createStatementsWorker() for Resource Node types
     *
     *  Return: List<Value>
     *    Returns the Resources as generic Values since these are "object" elements in
     *    ( source, predicate, object ) triples and need to be compatible with literals.
     */
    private List<Value> createStatementsWorker(
                        ParsedIRI baseIRI, ValueFactory factory, RepositoryConnection connection,
                        Project project) {
        List<Value> listResources = createResources(baseIRI, factory, project);
        if ( Util.isVerbose(4) ) {
            if (listResources == null)
                logger.info("Resources Count: NULL (No Statements)");
            else
                logger.info("Resources Count: " + listResources.size());
        }
        if (listResources != null) {
            try {
        	    this.createTypeStatements(listResources, factory, connection, baseIRI);
        	    this.createLinkStatements(listResources, baseIRI, factory, connection, project);
            }
            catch (RepositoryException ex) {
            	throw new RuntimeException(ex);
            }
        }
        return listResources;
    }

    /*
     *  Method createResource() for Resource Node types
     *
     *  Return: List<Value>
     *    Returns the Resources as generic Values since these are "object" elements in
     *    ( source, predicate, object ) triples and need to be compatible with literals.
     */
    protected abstract List<Value> createResources(
                                        ParsedIRI baseIRI, ValueFactory factory, Project project);

    /*
     *  Method createObjects() for Resource Node types on OpenRefine Rows
     *
     *  Return: List<Value>
     *    Returns the Resources as generic Values since these are "object" elements in
     *    ( source, predicate, object ) triples and need to be compatible with literals.
     */
    protected List<Value> createObjects(
                        ParsedIRI baseIRI, ValueFactory factory, RepositoryConnection connection,
                        Project project, ResourceNode nodeParent )
            throws RuntimeException
    {
        this.setRowRecord(nodeParent);
        List<Value> listResources = this.createStatementsWorker(baseIRI, factory, connection, project);
        this.resetRowRecord();

        return listResources;
    }

    protected abstract void writeNode(JsonGenerator writer) throws JsonGenerationException, IOException;

    public void write(JsonGenerator writer) throws JsonGenerationException, IOException {
        writer.writeStartObject();

        // Write node...
        writeNode(writer);

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
