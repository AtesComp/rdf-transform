package com.google.refine.rdf.model;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.refine.model.Project;
import com.google.refine.rdf.RDFTransform;
import com.google.refine.rdf.model.utils.RecordModel;
import com.google.refine.rdf.model.vocab.Vocabulary;

import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;

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
                            RDFTransform.Reconstructor theReconstructor,
                            JsonNode jnodeSubject, final ParsedIRI baseIRI, Map<String, Vocabulary> thePrefixes) {
        Objects.requireNonNull(theReconstructor);

        return Node.reconstructNode(jnodeSubject, baseIRI, thePrefixes);
    }

    static public Node reconstructNode(
                            Property.PropertyReconstructor thePropReconstructor,
                            JsonNode jnodeSubject, final ParsedIRI baseIRI, Map<String, Vocabulary> thePrefixes) {
        Objects.requireNonNull(thePropReconstructor);

        return Node.reconstructNode(jnodeSubject, baseIRI, thePrefixes);
    }

    static private Node reconstructNode(JsonNode jnodeSubject, final ParsedIRI baseIRI, Map<String, Vocabulary> thePrefixes) {
        Node nodeElement = null;
        if (jnodeSubject == null) {
            Node.logger.warn("WARNING: Missing Subject for Node");
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
        // TODO: Not currently supported by itself.
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
            }
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
                JsonNode jnodeType = jnodeValueType.get(Util.gstrType);
                String strTypeTemp = jnodeType.asText();
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
        // Process Subject into a Node...
        //
        ResourceNode rnodeResource = null;
        LiteralNode lnodeLiteral = null;

        // Resource types...
        // TODO: Adjust all resource node classes to take prefix reference
        if (bResource) {
            if ( strType.equals(Util.gstrIRI) ) {
                if ( bValueNode ) {
                    rnodeResource = new CellResourceNode(strValue, strPrefix, strExpCode, bIsIndex, eNodeType);
                }
                else if ( bConstNode ) {
                    rnodeResource = new ConstantResourceNode(strValue, strPrefix);
                }
                else if ( eNodeType == Util.NodeType.EXPRESSION ) {
                    // TODO: Currently unsupported
                }
            }
            else if ( strType.equals(Util.gstrBNode) ) {
                rnodeResource = new CellBlankNode(strValue, strExpCode, bIsIndex, eNodeType);
            }
            else if ( strType.equals(Util.gstrValueBNode) ) {
                rnodeResource = new ConstantBlankNode(strValue);
            }

            if ( rnodeResource != null ) {
                //
                // Construct Types from "typeMappings"...
                //
                RDFType.reconstructTypes(Node.theNodeReconstructor, rnodeResource, jnodeSubject, baseIRI, thePrefixes);

                //
                // Construct Properties from "propertyMappings"...
                //
                Property.reconstructProperties(Node.theNodeReconstructor, rnodeResource, jnodeSubject, baseIRI, thePrefixes);
            }

            nodeElement = rnodeResource;
        }

        // Literal types...
        else if (bLiteral) {
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
                    if (jnodeDatatypePrefix != null) {
                        strDatatypePrefix = jnodeDatatypePrefix.asText();
                    }
                    // ...get IRI value, always a constant...
                    JsonNode jnodeValueSource = jnodeDatatype.get(Util.gstrValueSource);
                    if (jnodeValueSource != null) {
                        JsonNode jnodeDatatypeConstant = jnodeValueSource.get(Util.gstrConstant);
                        if (jnodeDatatypeConstant != null) {
                            strDatatypeValue = jnodeDatatypeConstant.asText();

                            // Validate the full IRI (Namespace + Datatype)...
                            ParsedIRI iriNamespace = baseIRI; // ...default
                            if ( thePrefixes.containsKey(strDatatypePrefix) ) {
                                String strDatatypeNamespace = thePrefixes.get(strDatatypePrefix).getNamespace();
                                if (strDatatypeNamespace != null) {
                                    try {
                                        iriNamespace = new ParsedIRI( strDatatypeNamespace );
                                    }
                                    catch (Exception ex) {
                                        Node.logger.error("ERROR: Bad Namespace in Prefixes: " + strDatatypeNamespace, ex);
                                        // ...given Namespace doesn't parse, so use baseIRI...
                                    }
                                }
                            }
                            strDataType = Util.getDataType(iriNamespace, strDatatypeValue);
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
                // TODO: Currently unsupported - what is an expression? Value or Constant?
            }

            nodeElement = lnodeLiteral;
        }

        return nodeElement;
    }

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
    protected String strExpression;

    @JsonIgnore
    protected boolean bIsIndex = false;

    @JsonIgnore
    protected List<Value> listValues = null;

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
        String strExpanded = strObjectIRI;
        if (Util.isDebugMode()) Node.logger.info("DEBUG: expandPrefixedIRI: string = " + strObjectIRI);
        if ( ! strObjectIRI.contains("://") ) {
            if (Util.isDebugMode()) Node.logger.info("DEBUG: expandPrefixedIRI: checking prefix...");
            int iIndex = strObjectIRI.indexOf(':'); // ...get index of first ':'...
            if (Util.isDebugMode()) Node.logger.info("DEBUG: expandPrefixedIRI: index = " + iIndex);
            if (iIndex >= 0) { // ...a ':' exists...
                String strPrefix = strObjectIRI.substring(0, iIndex); // ...including blank ("") prefix...
                if (Util.isDebugMode()) Node.logger.info("DEBUG: expandPrefixedIRI: strPrefix = " + strPrefix);
                if (Util.isDebugMode()) Node.logger.info("DEBUG: expandPrefixedIRI: connection = " +
                                                    ( this.theConnection == null ? "null" : "connected" ) );
                String strNamespace = this.theConnection.getNamespace(strPrefix);
                if (Util.isDebugMode()) Node.logger.info("DEBUG: expandPrefixedIRI: strNamespace = " + strNamespace);
                if (strNamespace != null) {
                     // Get the string just beyond the first ':'...
                    strExpanded = strNamespace + strObjectIRI.substring(iIndex+1);
                }
            }
        }
        return strExpanded;
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
        this.theFactory = nodeProperty.theFactory;
        this.theConnection = nodeProperty.theConnection;
        this.theProject = nodeProperty.theProject;
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
