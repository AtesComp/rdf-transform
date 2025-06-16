/*
 *  Class Node
 *
 *  The Node base class use by other nodes in an RDF Transform.
 *
 *  Copyright 2025 Keven L. Ates
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

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import com.google.refine.model.Project;

import org.openrefine.rdf.RDFTransform;
import org.openrefine.rdf.model.utils.RecordModel;
import org.openrefine.rdf.model.vocab.VocabularyList;

import org.apache.jena.iri.IRI;
//import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.core.DatasetGraph;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.fasterxml.jackson.databind.JsonNode;
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

    static public final class NodeReconstructor {
        private NodeReconstructor() {};
    }

    // Reconstruction Validator
    static private final NodeReconstructor theNodeReconstructor = new NodeReconstructor();

    /*
     * Method reconstructNode()
     *
     *      Helper function for RDFTransform.reconstruct()
     *
     *      Reconstruct any ol' Subject and their Properties.  Nodes are generic descriptors for a related
     *      transformation.  They are transformed into RDF Resource and Literal nodes to construct
     *      (Subject, Property, Object) tuples for an RDF graph.
     */

    static public Node reconstructNode(
                            RDFTransform.Reconstructor theNodeReconstructor,
                            JsonNode jnodeSubject, final IRI baseIRI, VocabularyList theNamespaces) {
        Objects.requireNonNull(theNodeReconstructor);

        return Node.reconstructNode(jnodeSubject, baseIRI, theNamespaces);
    }

    static public Node reconstructNode(
                            Property.PropertyReconstructor thePropReconstructor,
                            JsonNode jnodeSubject, final IRI baseIRI, VocabularyList theNamespaces) {
        Objects.requireNonNull(thePropReconstructor);

        return Node.reconstructNode(jnodeSubject, baseIRI, theNamespaces);
    }

    static private Node reconstructNode(JsonNode jnodeSubject, final IRI baseIRI, VocabularyList theNamespaces) {
        Node nodeElement = null;
        if (jnodeSubject == null) {
            Node.logger.warn("WARNING: Missing Subject for Node");
            return null;
        }
        if ( jnodeSubject.isNull() ) {
            Node.logger.warn("WARNING: Subject is NULL");
            return null;
        }

        //
        // Get Subject's Prefix...
        //
        String strPrefix = null;
        if ( jnodeSubject.has(Util.gstrPrefix) ) {
            strPrefix = jnodeSubject.get(Util.gstrPrefix).asText();
        }

        //
        // Get Subject's Value Type...
        //
        //      Based on Type, get type information
        //
        String strType = Util.gstrIRI;  // ...default for Root nodes
        JsonNode jnodeValueType = jnodeSubject; // ...for Root nodes
        boolean bResource = false;
        boolean bLiteral = false;
        if ( jnodeSubject.has(Util.gstrValueType) ) { // ...for Object nodes
            jnodeValueType = jnodeSubject.get(Util.gstrValueType);
            if ( jnodeValueType.has(Util.gstrType) ) {
                String strTypeTemp = jnodeValueType.get(Util.gstrType).asText();
                if ( ! ( strTypeTemp == null || strTypeTemp.isEmpty() ) ) {
                    strType = strTypeTemp;
                }
            }
        }
        if ( strType.equals(Util.gstrIRI) ||
             strType.equals(Util.gstrBNode) ||
             strType.equals(Util.gstrValueBNode) ) {
            bResource = true;
        }
        if ( strType.equals(Util.gstrLiteral) ||
             strType.equals(Util.gstrDatatypeLiteral) ||
             strType.equals(Util.gstrLanguageLiteral) ) {
            bLiteral = true;
        }

        //
        // Get Subject's Value Source...
        //
        //      Based on Source, get source information
        //
        String strSource = "";
        JsonNode jnodeValueSrc = null;
        if ( jnodeSubject.has(Util.gstrValueSource) ) {
            jnodeValueSrc = jnodeSubject.get(Util.gstrValueSource);
            if ( jnodeValueSrc.has(Util.gstrSource) ) {
                strSource = jnodeValueSrc.get(Util.gstrSource).asText();
            }
        }

        boolean bIsIndex = false;
        Util.NodeType eNodeType = null;
        boolean bValueNode = false;
        boolean bConstNode = false;
        String strValue = null; // ..as Column Name or Constant
        //boolean bIsExpression = false;

        if ( strSource.equals(Util.gstrRowIndex) ) {
            // A Row Index based node...
            bIsIndex = true;
            eNodeType = Util.NodeType.ROW;
            bValueNode = true;
        }
        else if ( strSource.equals(Util.gstrRecordID) ) {
            // A Record Index (group of rows) based node...
            bIsIndex = true;
            eNodeType = Util.NodeType.RECORD;
            bValueNode = true;
        }
        else if ( strSource.equals(Util.gstrColumn) && jnodeValueSrc.has(Util.gstrColumnName) ) {
            // A Column Name based node...
            strValue = jnodeValueSrc.get(Util.gstrColumnName).asText();
            eNodeType = Util.NodeType.COLUMN;
            bValueNode = true;
        }
        else if ( strSource.equals(Util.gstrConstant) && jnodeValueSrc.has(Util.gstrConstant)) {
            // A Constant based node...
            strValue = jnodeValueSrc.get(Util.gstrConstant).asText();
            eNodeType = Util.NodeType.CONSTANT;
            bConstNode = true;
        }
        // TODO: Expressions currently unsupported independently.
        //      Expressions may be embedded in the others.
        //      Is it a Value or Constant node?
        //else if ( strSource.equals(Util.gstrExpression) ) {
        //    // An Expression based node...
        //    eNodeType = Util.NodeType.EXPRESSION;
        //    bIsExpression = true;
        //    bValueNode = true;
        //    bConstNode = true;
        //}

        if (eNodeType == null) {
            Node.logger.error("ERROR: Bad Node: Source: " + strSource);
            return null;
        }

        //
        // Get Subject's Expression...
        //
        String strExpLang = Util.gstrGREL; // ...default
        String strExpCode = null;          // ...default, Node instances report "value" when null
        if ( jnodeSubject.has(Util.gstrExpression) ) {
            JsonNode jnodeExp = jnodeSubject.get(Util.gstrExpression);
            if ( jnodeExp.has(Util.gstrLanguage) ) {
                strExpLang = jnodeExp.get(Util.gstrLanguage).asText();
            }
            if ( jnodeExp.has(Util.gstrCode) ) {
                strExpCode = jnodeExp.get(Util.gstrCode).asText().strip();
                // If there is an embedded language in the code, it overrides...
                int iColon = strExpCode.indexOf(58);
                if (iColon >= 0) {
                    strExpLang = strExpCode.substring(0, iColon).toLowerCase();
                }
                // Otherwise, embed the language in the code...
                else strExpCode = strExpLang + ":" + strExpCode;
            }
        }

        //
        // Process Subject into a Node...
        //

        // Resource types...
        if (bResource) {
            nodeElement =
                Node.reconstructResourceNode(jnodeSubject, baseIRI, theNamespaces,
                    strType, bValueNode, bConstNode, strValue, strPrefix, strSource, strExpCode,
                    bIsIndex, eNodeType);
        }

        // Literal types...
        else if (bLiteral) {
            nodeElement =
                Node.reconstructLiteralNode(jnodeSubject, baseIRI, theNamespaces,
                    strType, jnodeValueType, bValueNode, bConstNode, strValue, strExpCode,
                    bIsIndex, eNodeType);
        }

        return nodeElement;
    }

    static private ResourceNode reconstructResourceNode(JsonNode jnodeSubject, final IRI baseIRI, VocabularyList theNamespaces,
            String strType, boolean bValueNode, boolean bConstNode, String strValue, String strPrefix, String strSource, String strExpCode,
            boolean bIsIndex, Util.NodeType eNodeType) {

        ResourceNode rnodeResource = null;

        if ( strType.equals(Util.gstrIRI) ) {
            if ( bValueNode ) {
                rnodeResource = new CellResourceNode(strValue, strPrefix, strExpCode, bIsIndex, eNodeType);
            }
            else if ( bConstNode ) {
                rnodeResource = new ConstantResourceNode(strValue, strPrefix);
            }
            else if ( eNodeType == Util.NodeType.EXPRESSION ) {
                // TODO: Expressions currently unsupported independently
            }
        }
        else if ( strType.equals(Util.gstrBNode) ) { // Variable
            rnodeResource = new CellBlankNode(strValue, strExpCode, bIsIndex, eNodeType);
        }
        else if ( strType.equals(Util.gstrValueBNode) ) { // Constant Value
            rnodeResource = new ConstantBlankNode(strValue);
        }

        // Check Resource creation...
        if (rnodeResource == null) {
            Node.logger.error(
                "ERROR: Bad Node: Prefix: [" + strPrefix + "]  Src: [" + strSource + "]  Val: [" + strValue + "]" +
                "  Exp: [" + strExpCode + "]");
        }
        else {
            //
            // Construct Types from "typeMappings"...
            //
            RDFType.reconstructTypes(Node.theNodeReconstructor, rnodeResource, jnodeSubject, baseIRI, theNamespaces);

            //
            // Construct Properties from "propertyMappings"...
            //
            Property.reconstructProperties(Node.theNodeReconstructor, rnodeResource, jnodeSubject, baseIRI, theNamespaces);
        }

        return rnodeResource;
    }

    static private LiteralNode reconstructLiteralNode(JsonNode jnodeSubject, final IRI baseIRI, VocabularyList theNamespaces,
            String strType, JsonNode jnodeValueType, boolean bValueNode, boolean bConstNode, String strValue, String strExpCode,
            boolean bIsIndex, Util.NodeType eNodeType) {

        LiteralNode lnodeLiteral = null;

        String strDatatypePrefix = null;
        String strDatatypeValue = null;
        String strDataType = null;
        ConstantResourceNode nodeDatatype = null;
        String strLanguageCode = null;

        if ( strType.equals(Util.gstrLiteral) ) {
            // Nothing to do...
        }
        else if ( strType.equals(Util.gstrDatatypeLiteral) ) {
            // A Datatype Literal based node...
            JsonNode jnodeDatatype = jnodeValueType.get(Util.gstrDatatype);
            if (jnodeDatatype != null) {
                // ...get prefix, if exists...
                JsonNode jnodeDatatypePrefix = jnodeDatatype.get(Util.gstrPrefix);
                if  ( ! ( jnodeDatatypePrefix == null || jnodeDatatypePrefix.isNull() ) ) {
                    strDatatypePrefix = jnodeDatatypePrefix.asText();
                }
                // ...get IRI value, always a constant...
                JsonNode jnodeValueSource = jnodeDatatype.get(Util.gstrValueSource);
                if ( ! ( jnodeValueSource == null || jnodeValueSource.isNull() ) ) {
                    JsonNode jnodeDatatypeConstant = jnodeValueSource.get(Util.gstrConstant);
                    if ( ! ( jnodeDatatypeConstant == null || jnodeDatatypeConstant.isNull() ) ) {
                        strDatatypeValue = jnodeDatatypeConstant.asText();

                        // Validate the full IRI (Namespace + Datatype)...
                        IRI iriNamespace = baseIRI; // ...default
                        if ( theNamespaces.containsPrefix(strDatatypePrefix) ) {
                            String strDatatypeNamespace = theNamespaces.findByPrefix(strDatatypePrefix).getNamespace();
                            if (strDatatypeNamespace != null) {
                                iriNamespace = Util.buildIRI( strDatatypeNamespace );
                                if (iriNamespace == null) {
                                    Node.logger.error("ERROR: Bad Namespace in Namespaces: " + strDatatypeNamespace);
                                    // ...given Namespace doesn't parse, so use baseIRI...
                                    iriNamespace = baseIRI; // ...default
                                }
                            }
                        }
                        strDataType = Util.getDataType(iriNamespace, strDatatypePrefix, strDatatypeValue);
                        if ( ! ( strDataType == null || strDataType.isEmpty() ) ) {
                            // Set the Datatype to the CIRIE (Prefix + Datatype)...
                            //strDataType = strDatatypePrefix + ":" + strDatatypeValue;
                            nodeDatatype = new ConstantResourceNode(strDatatypeValue, strDatatypePrefix);
                        }
                    }
                }
            }
        }
        else if ( strType.equals(Util.gstrLanguageLiteral) ) {
            strLanguageCode = jnodeValueType.get(Util.gstrLanguage).asText();
        }

        if ( bValueNode ) {
            lnodeLiteral = new CellLiteralNode(strValue, strExpCode, bIsIndex, nodeDatatype, strLanguageCode, eNodeType);
        }
        else if ( bConstNode ) {
            lnodeLiteral = new ConstantLiteralNode(strValue, nodeDatatype, strLanguageCode);
        }
        else if ( eNodeType == Util.NodeType.EXPRESSION ) {
            // TODO: Expressions currently unsupported independently - what is an expression? Value or Constant?
        }

        return lnodeLiteral;
    }

    @JsonIgnore
    protected IRI baseIRI = null;

    @JsonIgnore
    //protected Model theModel = null;
    protected DatasetGraph theDSGraph = null;

    @JsonIgnore
    protected Project theProject = null;

    @JsonIgnore
    public RecordModel theRec = null;

    @JsonIgnore
    protected String strExpression;

    @JsonIgnore
    protected boolean bIsIndex = false;

    @JsonIgnore
    protected List<RDFNode> listNodes = null;

    @JsonIgnore
    protected Util.NodeType eNodeType = null;

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

    @JsonProperty("isIndex")
    public boolean isIndexNode() {
        return this.bIsIndex;
    }

    public Util.NodeType getNodeSubType() {
        return eNodeType;
    }

    protected String expandPrefixedIRI(String strObjectIRI) {
        if (this.theDSGraph == null) {
            return null;
        }
        //return this.theModel.expandPrefix(strObjectIRI);
        return this.theDSGraph.prefixes().expand(strObjectIRI);

        // String strExpanded = strObjectIRI;
        // int iIndex = strObjectIRI.indexOf(':'); // ...get index of first ':'...
        // if (iIndex >= 0) { // ...a ':' exists...
        //     if ( ! strObjectIRI.contains("://") ) { // ...if not already expanded...
        //         String strPrefix = strObjectIRI.substring(0, iIndex); // ...including blank ("") prefix...
        //         String strNamespace = this.theModel.getNsPrefixURI(strPrefix);
        //         if (strNamespace != null) {
        //              // Get the string just beyond the first ':'...
        //             strExpanded = strNamespace + strObjectIRI.substring(iIndex+1);
        //         }
        //     }
        // }
        // return strExpanded;
    }

    /*
     *  Method setObjectParameters()
     *
     *  Copy the parameters from the given Property resource to this Node object.
     *
     */
    @JsonIgnore
    protected void setObjectParameters(ResourceNode nodeProperty) {
        this.baseIRI = nodeProperty.baseIRI;
        //this.theModel = nodeProperty.theModel;
        this.theDSGraph = nodeProperty.theDSGraph;
        this.theProject = nodeProperty.theProject;
    }

    /*
     *  Method createObjects()
     *
     *    Creates the object list for triple statements from this node.
     */
    abstract protected List<RDFNode> createObjects(ResourceNode nodeProperty);

    abstract public void write(JsonGenerator writer, boolean isRoot)
            throws JsonGenerationException, IOException;
}
