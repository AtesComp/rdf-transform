/*
 *  Class ResourceNode
 *
 *  The Resource Node base class use by other Resource based nodes in an RDF
 *  Transform.
 *
 *  Copyright 2024 Keven L. Ates
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.openrefine.rdf.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.io.IOException;

import com.google.refine.model.Project;
import com.google.refine.model.Record;

import org.openrefine.rdf.model.Util.IRIParsingException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerationException;

import org.apache.jena.iri.IRI;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.util.NodeUtils;
import org.apache.jena.vocabulary.RDF;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract public class ResourceNode extends Node {
    static private final Logger logger = LoggerFactory.getLogger("RDFT:ResNode");

    static protected final String strBNodePrefix = "_:";

    @JsonProperty(Util.gstrPropertyMappings)
    private List<Property> listProperties = new ArrayList<Property>();

    @JsonProperty(Util.gstrTypeMappings)
    private List<RDFType> listTypes = new ArrayList<RDFType>();

    @JsonIgnore
    public void addType(RDFType typeNew) {
        this.listTypes.add(typeNew);
    }

    @JsonIgnore
    public void addProperty(Property propNew) {
        this.listProperties.add(propNew);
    }

    @JsonProperty(Util.gstrTypeMappings)
    public List<RDFType> getTypes() {
        return this.listTypes;
    }

    @JsonProperty(Util.gstrPropertyMappings)
    public List<Property> getProperties() {
        return this.listProperties;
    }

    /*
     *  Method processResultsAsArray() for results to Resources
     */
    protected void processResultsAsArray(String strPrefix, Object results) {
        List<Object> listResult = Arrays.asList(results);
        for (Object objResult : listResult) {
            if ( objResult == null || objResult.toString().isEmpty() ) {
                continue;
            }
            if (strPrefix == null) {
                if ( processResultsAsSingle(objResult) ) {
                    continue;
                }
            }
            this.normalizeResource(strPrefix, objResult);
        }
    }

    /*
     *  Method processResultsAsArray() for a single result to a Resource
     */
    protected boolean processResultsAsSingle(Object objResult) {
        String strLocalPart = Util.toSpaceStrippedString(objResult);
        if ( strLocalPart == null || strLocalPart.isEmpty() ) {
            return false;
        }
        String strEmbeddedPrefix = null;

        try {
            IRI tempIRI = Util.buildIRI(strLocalPart);
            if (tempIRI != null) {
                // ...it parsed as an IRI...
                // If a scheme is present, but a host is not present...
                strEmbeddedPrefix = tempIRI.getScheme();
                if (strEmbeddedPrefix != null && tempIRI.getRawHost() == null) {
                    // There is no authority component:
                    //    i.e., there was no "schema://...", just "schema:...", so
                    //    the authority parsing that contains the host parsing was not
                    //    performed.  The rest may parse as a path, query, fragment.
                    // Then, the schema is a prefix and that is enough...
                    strLocalPart = strLocalPart.substring(strEmbeddedPrefix.length() + 1);
                    this.normalizeResource(strEmbeddedPrefix, strLocalPart);
                    return true;
                }
            }
        }
        catch (Exception ex) {
            // ...continue: try as non-prefixed IRI...
        }
        return false;
    }

    /*
     *  Method normalizeResource() for Resource Node to IRI
     */
    protected void normalizeResource(String strPrefix, Object objResult) {
        if (objResult == null) {
            return;
        }
        String strIRI = Util.toSpaceStrippedString(objResult); // ...Default: Full IRI
        if ( strIRI.isEmpty() ) {
            return;
        }
        String strNamespace = null;
        String strLocalPart = strIRI; // ...for "prefix:localPart" IRI
        if (strPrefix != null) { // ...on prefix, attempt namespace...
            strIRI = strPrefix + ":" + strLocalPart;
            //strNamespace = this.theModel.getNsPrefixURI(strPrefix);
            strNamespace = this.theDSGraph.prefixes().get(strPrefix);
        }
        if ( Util.isDebugMode() ) {
            String strDebug = "DEBUG: normalizeResource: Given: ";
            if (strPrefix == null) {
                strDebug += "IRI: " + strIRI;
            }
            else {
                strDebug += "Prefix: " + strPrefix + " LocalPart: " + strLocalPart;
            }
            ResourceNode.logger.info(strDebug);
        }

        try {
            String strResolvedIRI = Util.resolveIRI(this.baseIRI, strIRI);
            if (Util.isDebugMode()) ResourceNode.logger.info("DEBUG: normalizeResource: Resolved IRI: " + strResolvedIRI);
            if (strResolvedIRI != null) { // ...at least it's a good, basic IRI...
                String strFullIRI = strResolvedIRI; // ...Default: Full IRI
                ResourceImpl nodeResource;
                if (strNamespace == null) { // ...and both strPrefix == null and != null
                    nodeResource = new ResourceImpl(strFullIRI);
                }
                else {
                    strFullIRI = strNamespace + strLocalPart;
                    nodeResource = new ResourceImpl(strNamespace, strLocalPart);
                }
                if (Util.isDebugMode()) ResourceNode.logger.info("DEBUG: normalizeResource: Processed IRI: " + strFullIRI);
                this.listNodes.add( nodeResource );
            }
        }
        //catch (IRIParsingException | IllegalArgumentException ex) {
        catch (Exception ex) {
            // An IRIParsingException from Util.resolveIRI() means a bad IRI.
            // An IllegalArgumentException from theFactory.createIRI() means a bad IRI.
            // In either case, record error and eat the exception...
            ResourceNode.logger.error( "ERROR: Bad IRI: " + strIRI, ex);
        }
    }

    /*
     *  Method createStatements() for Root Resource Node types on OpenRefine Rows
     */
    public void createStatements(IRI baseIRI, DatasetGraph theDSGraph, Project theProject, int iRowIndex)
            throws RuntimeException
    {
        this.baseIRI = baseIRI;
        //this.theModel = theModel;
        this.theDSGraph = theDSGraph;
        this.theProject = theProject;

        this.theRec.setRootRow(iRowIndex);
        this.createStatementsWorker();
        this.theRec.clear();
    }

    /*
     *  Method createStatements() for Root Resource Node types on OpenRefine Records
     */
    public void createStatements(IRI baseIRI, DatasetGraph theDSGraph, Project theProject, Record theRecord)
            throws RuntimeException
    {
        this.baseIRI = baseIRI;
        //this.theModel = theModel;
        this.theDSGraph = theDSGraph;
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
            List<RDFNode> listResources = new ArrayList<RDFNode>();
            while ( this.theRec.rowNext() ) {
                this.createRowResources(); // ...Row only
                if ( ! ( this.listNodes == null || this.listNodes.isEmpty() ) ) {
                    this.createResourceStatements();
                    listResources.addAll(this.listNodes);
                }
            }
            if ( listResources.isEmpty() ) {
                listResources = null;
            }
            this.listNodes = listResources;
        }

        //
        // Standard Record or Row processing...
        //
        else {
            this.createResources(); // ...Record or Row
            if ( this.listNodes == null || this.listNodes.isEmpty() ) {
                this.listNodes = null;
            }
            else {
                this.createResourceStatements();
            }
        }
    }

    /*
     *  Method createResources() for Resource Node types
     *
     *  Return: void
     *
     *  Stores the Resources as generic Values since these are "object" elements in
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

    /*
     *  Method createRecordResources() creates the object list for triple statements
     *  from this node on Records
     */
    protected void createRecordResources() {
        if (Util.isDebugMode()) ResourceNode.logger.info("DEBUG: createRecordResources...");

        List<RDFNode> listResources = new ArrayList<RDFNode>();
        while ( this.theRec.rowNext() ) {
            this.createRowResources();
            if ( this.listNodes != null ) {
                listResources.addAll(this.listNodes);
            }
        }
        if ( listResources.isEmpty() ) {
            listResources = null;
        }

        this.listNodes = listResources;
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
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /*
     *  Method createTypeStatements() for Resource Node types
     *
     *    Given a set of source resources, create the (source, rdf:type, object) triple statements
     *    for each of the sources.
     */
    private void createTypeStatements() {
        if ( Util.isDebugMode() ) {
            String strPropertyCount = "DEBUG: createTypeStatements: Type Count: {}";
            int iPropertyCount = 0;
            if (this.listTypes != null) {
                iPropertyCount = listTypes.size();
            }
            ResourceNode.logger.info(strPropertyCount, iPropertyCount);
        }
        if (this.listTypes == null) {
            return;
        }

        String strPrefix = null;
        String strType = null;

        String strNamespace;
        String strLocalPart;
        String strFullType;
        ResourceImpl nodeType;

        //
        // Process one set of types
        //
        List<RDFNode> listTypesForStmts = new ArrayList<RDFNode>();
        for ( RDFType typeItem : this.listTypes ) {
            strPrefix = typeItem.getPrefix(); // Null indicated FULL IRI, Empty indicate BaseIRI
            strType = typeItem.getLocalPart(); // ...assume FULL IRI
            strLocalPart = null;
            strNamespace = null;
            if (strPrefix != null) { // ...prefixed...
                strLocalPart = strType;
                strType = strPrefix + ":" + strLocalPart; // ...CIRIE
                //strNamespace = this.theModel.getNsPrefixURI(strPrefix);
                strNamespace = this.theDSGraph.prefixes().get(strPrefix);
            }
            if (Util.isDebugMode()) ResourceNode.logger.info("DEBUG: Type: [" + strType + "]");
            if ( strType == null || strType.isEmpty() ) {
                continue;
            }

            try {
                // Resolve the IRI for Full IRI or CIRIE...
                strFullType = Util.resolveIRI(this.baseIRI, strType);
                if (strFullType != null) {
                    if (Util.isDebugMode()) ResourceNode.logger.info("DEBUG: Type Resource: [" + strFullType + "]");
                    if (strNamespace != null) {
                        nodeType = new ResourceImpl(strNamespace, strLocalPart);
                    }
                    else { // ...on no prefix or missing namespace, treat as Full...
                        nodeType = new ResourceImpl(strFullType);
                    }
                    listTypesForStmts.add(nodeType);
                }
            }
            catch (IRIParsingException | IllegalArgumentException ex) {
                logger.error( "ERROR: Bad Type IRI: " + strType, ex);
            }
        }

        //
        // Process statements...
        //
        org.apache.jena.graph.Node nodeGraph = NodeUtils.asNode( Util.getGraphIRIString( this.baseIRI.toString() ) );
        for (RDFNode theSource : this.listNodes) {
            for (RDFNode theType : listTypesForStmts) {
                //this.theModel.add( (Resource) theSource, RDF.type, theType );
                this.theDSGraph.add(
                    nodeGraph,
                    theSource.asNode(),
                    RDF.type.asNode(),
                    theType.asNode()
                );
            }
        }
    }

    /*
     *  Method createPropertyStatements() for Resource Node types on OpenRefine Rows
     *
     *    Given a set of source resources, create the (source, property, object) triple statements
     *    for each of the sources.
     */
    private void createPropertyStatements() {
        if ( Util.isDebugMode() ) {
            String strPropertyCount = "DEBUG: createPropertyStatements: Property Count: {}";
            int iPropertyCount = 0;
            if (this.listProperties != null) {
                iPropertyCount = this.listProperties.size();
            }
            ResourceNode.logger.info(strPropertyCount, iPropertyCount);
        }
        if (this.listProperties == null) {
            return;
        }

        String strPrefix = null;
        String strProperty = null;

        String strNamespace;
        String strLocalName;
        Node nodeObject;
        List<RDFNode> listObjects;
        String strFullProperty;
        PropertyImpl theProperty;

        @JsonIgnoreType
        class PropertyObjectList {
            private PropertyImpl nodeProp;
            private List<RDFNode> listObjs;

            PropertyObjectList(PropertyImpl nodeProp, List<RDFNode> listObjs) {
                this.nodeProp = nodeProp;
                this.listObjs = listObjs;
            }
            public PropertyImpl getProperty() {
                return this.nodeProp;
            }
            public List<RDFNode> getObjects() {
                return this.listObjs;
            }
        }

        //
        // Process one set of properties
        //
        List<PropertyObjectList> listPropsForStmts = new ArrayList<PropertyObjectList>();
        for (Property propItem : this.listProperties) {
            //
            // PROPERTY
            //
            strPrefix = propItem.getPrefix(); // Null indicated FULL IRI, Empty indicate BaseIRI
            strProperty = propItem.getPathProperty(); // ...assume FULL IRI
            strLocalName = null;
            strNamespace = null;
            if (strPrefix != null) { // ...prefixed...
                strLocalName = strProperty;
                strProperty = strPrefix + ":" + strLocalName; // ...CIRIE
                //strNamespace = this.theModel.getNsPrefixURI(strPrefix);
                strNamespace = this.theDSGraph.prefixes().get(strPrefix);
            }
            if (Util.isDebugMode()) ResourceNode.logger.info("DEBUG: Prop: [" + strProperty + "]");
            if ( strProperty == null || strProperty.isEmpty() ) {
                continue;
            }

            //
            // OBJECTS
            //
            nodeObject = propItem.getObject();
            if (nodeObject == null) { // ...no Object?
                if (Util.isDebugMode()) ResourceNode.logger.info("DEBUG: Cannot create statements: no Object for Property found.");
                continue; // ...then, no statement can be processed
            }
            listObjects = nodeObject.createObjects(this);
            if (listObjects == null) { // ...no Object List?
                if (Util.isDebugMode()) ResourceNode.logger.info("DEBUG: Cannot create statements: no Object List created on Object for Property.");
                continue; // ...then, no statements can be processed
            }

            try {
                // Resolve Property for Full IRI and CIRIE...
                strFullProperty = Util.resolveIRI(this.baseIRI, strProperty);
                if (strFullProperty != null) {
                    if (Util.isDebugMode()) ResourceNode.logger.info("DEBUG: Prop Resource: [" + strFullProperty + "]");
                    if (strNamespace != null) {
                        theProperty = new PropertyImpl(strNamespace, strLocalName);
                    }
                    else { // ...on no prefix or missing namespace, treat as Full...
                        theProperty = new PropertyImpl(strFullProperty);
                    }
                    listPropsForStmts.add( new PropertyObjectList(theProperty, listObjects) );
                }
            }
            catch (IRIParsingException | IllegalArgumentException ex) {
                logger.error( "ERROR: Bad Property IRI: " + strProperty, ex);
            }
        }

        //
        // Process statements...
        //
        org.apache.jena.graph.Node nodeGraph = NodeUtils.asNode( Util.getGraphIRIString( this.baseIRI.toString() ) );
        for (RDFNode theSource : this.listNodes) {
            for ( PropertyObjectList polPropItem : listPropsForStmts )
            {
                theProperty = polPropItem.getProperty();
                listObjects = polPropItem.getObjects();
                for (RDFNode theObject : listObjects) {
                    //this.theModel.add(
                    //    (Resource) theSource,
                    //    (org.apache.jena.rdf.model.Property) theProperty,
                    //    theObject
                    //);
                    this.theDSGraph.add(
                        nodeGraph,
                        theSource.asNode(),
                        theProperty.asNode(),
                        theObject.asNode()
                    );
                }
            }
        }
    }

    /*
     *  Method createObjects() for Resource Node types on OpenRefine Rows
     *
     *  Return: List<RDFNode>
     *    Returns the Resources as generic Values since these are "object" elements in
     *    ( source, predicate, object ) triples and need to be compatible with literals.
     */
    @Override
    protected List<RDFNode> createObjects(ResourceNode nodeProperty)
            throws RuntimeException {
        if (Util.isDebugMode()) ResourceNode.logger.info("DEBUG: createObjects...");

        this.setObjectParameters(nodeProperty);

        // TODO: Create process for Sub-Records

        this.listNodes = null;

        //
        // Record Mode...
        //
        if ( nodeProperty.theRec.isRecordMode() ) { // ...property is Record based,
            // ...set to Row Mode and process on current row as set by rowNext()...
            this.theRec.setMode(nodeProperty, true);
        }

        //
        // Row Mode...
        //
        else {
            // ...process on current row as set by rowNext()...
            this.theRec.setMode(nodeProperty);
        }

        this.createStatementsWorker();
        this.theRec.clear();

        // Return the collected resources from the statement processing as Objects
        // to the given Property...
        return this.listNodes;
    }

    abstract protected void writeNode(JsonGenerator writer, boolean isRoot)
            throws JsonGenerationException, IOException;

    public void write(JsonGenerator writer, boolean isRoot)
            throws JsonGenerationException, IOException {
        writer.writeStartObject();

        // Write node...
        this.writeNode(writer, isRoot);

        // Write Type Mappings...
        if (this.listTypes != null) {
            writer.writeArrayFieldStart(Util.gstrTypeMappings);
            for ( RDFType typeObj : this.listTypes ) {
                if (typeObj != null) {
                    typeObj.write(writer);
                }
            }
            writer.writeEndArray();
        }

        // Write Property Mappings...
        if (this.listProperties != null) {
            writer.writeArrayFieldStart(Util.gstrPropertyMappings);
            for (Property propObj : this.listProperties) {
                if (propObj != null) {
                    propObj.write(writer);
                }
            }
            writer.writeEndArray();
        }

        writer.writeEndObject();
    }
}
