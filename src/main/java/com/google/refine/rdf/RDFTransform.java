package com.google.refine.rdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.refine.rdf.model.CellBlankNode;
import com.google.refine.rdf.model.CellLiteralNode;
import com.google.refine.rdf.model.CellResourceNode;
import com.google.refine.rdf.model.ConstantBlankNode;
import com.google.refine.rdf.model.ConstantLiteralNode;
import com.google.refine.rdf.model.ConstantResourceNode;
import com.google.refine.rdf.model.Node;
import com.google.refine.rdf.model.ResourceNode;
import com.google.refine.rdf.model.LiteralNode;
import com.google.refine.rdf.model.Property;
import com.google.refine.rdf.model.RDFType;
import com.google.refine.rdf.model.Util;
import com.google.refine.rdf.model.vocab.PrefixExistException;
import com.google.refine.rdf.model.vocab.Vocabulary;
import com.google.refine.model.OverlayModel;
import com.google.refine.model.Project;

import org.eclipse.rdf4j.common.net.ParsedIRI;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

enum NodeType {
    ROW,
    RECORD,
    COLUMN,
    CONSTANT,
    EXPRESSION
}

public class RDFTransform implements OverlayModel {
    /****************************************************************************************************
     ****************************************************************************************************
     *
     *  Class Variables
     * 
     ****************************************************************************************************
     ****************************************************************************************************/

    static public final String EXTENSION = "RDFTransform";
    static public final String VERSION_MAJOR = "2";
    static public final String VERSION_MINOR = "0";
    static public final String VERSION_MICRO = "0";
    static public final String VERSION =
        RDFTransform.VERSION_MAJOR + "." +
        RDFTransform.VERSION_MINOR + "." +
        RDFTransform.VERSION_MICRO;
    // This Server-side KEY matches Client-side RDFTransform.KEY
    static public final String KEY = "rdf_transform";

    // RDF Transform JSON
    // --------------------------------------------------------------------------------
    static public final String gstrExtension = "extension";
    static public final String gstrVersion = "version";
    static public final String gstrBaseIRI = "baseIRI";
    static public final String gstrNamespaces = "namespaces";
    static public final String gstrSubjectMappings = "subjectMappings";
    static public final String gstrTypeMappings = "typeMappings";
    static public final String gstrPropertyMappings = "propertyMappings";
    static public final String gstrObjectMappings = "objectMappings";
    static public final String gstrPrefix = "prefix";
    static public final String gstrValueType = "valueType";
    static public final String gstrType = "type";
    static public final String gstrIRI = "iri";                          // type
    static public final String gstrLiteral = "literal";                  // type
    static public final String gstrDatatypeLiteral = "datatype_literal"; // type
    static public final String gstrDatatype = "datatype";                // key
    static public final String gstrLanguageLiteral = "language_literal"; // type
    static public final String gstrBNode = "bnode";                      // type
    static public final String gstrValueBNode = "value_bnode";           // type
    static public final String gstrValueSource = "valueSource";
    static public final String gstrSource = "source";
    static public final String gstrConstant = "constant";     // source & key
    static public final String gstrColumn = "column";         // source
    static public final String gstrColumnName = "columnName"; // key
    static public final String gstrRowIndex = "row_index";    // source, no key
    static public final String gstrRecordID = "record_id";    // source, no key
    static public final String gstrExpression = "expression"; // also source
    static public final String gstrLanguage = "language";     // also type key
    static public final String gstrCode = "code";

	static private final Logger logger = LoggerFactory.getLogger("RDFT:RDFTransform");

    /****************************************************************************************************
     ****************************************************************************************************
     *
     *  Class Methods
     * 
     ****************************************************************************************************
     ****************************************************************************************************/

    static public RDFTransform getRDFTransform(ApplicationContext theContext, Project theProject)
			throws IOException {
		synchronized (theProject) {
			RDFTransform theTransform = (RDFTransform) theProject.overlayModels.get(RDFTransform.EXTENSION);
			if (theTransform == null) {
				theTransform = new RDFTransform(theContext, theProject);

				theProject.overlayModels.put(RDFTransform.EXTENSION, theTransform);
				theProject.getMetadata().updateModified();
			}
            return theTransform;
		}
	}

    static public RDFTransform load(Project theProject, JsonNode jnodeElement) {
        RDFTransform theTransform = RDFTransform.reconstruct(jnodeElement);
        return theTransform;
    }

    /*
     * Method reconstruct()
     *
     *      Reconstruct an RDF Transform, it's Subject, Property, and Type Mappings.
     * 
     *      Transform Nodes are use to construct (Subject, Property, Object) tuples for a
     *      data transform to RDF.
     *      An RDFTransform contains the following:
     *        Base IRI - a default namespace descriptor
     *        Namespaces - a mapping of short prefixes to namespace descriptors
     *        Subject Mappings - a list of Transform Nodes that serve as Subjects and contain
     *            Property Mappings and Type Mappings
     *        
     *      Transform Nodes are column or constant descriptors that represent Subjects,
     *          Properties, and Objects.
     *      Subjects and Objects are column or constant descriptors that represent graph nodes.
     *      Properties are column or constant descriptors that connect Subjects to
     *          Objects, i.e. graph edges, and provide context.
     *      Types use the "rdf:type" property to describe Objects that define a Subject's
     *          type.
     * 
     *      Nodes and Edges:
     *      Generally, there are two Transform Node categories - Resources and Literals.
     *          Subjects and Properties are always Resources.
     *          Properties are linking (edge) connections between Subjects and
     *              Objects.
     *          Objects can be either Resources or Literals.
     *      Then, Resources are the nodes and edges for an RDF graph and may
     *          be Subjects, Properties, and Objects.
     *      Then, Literals are "leaf (terminal)" nodes in an RDF graph and are
     *          always Objects.
     *      A given Transform Node can server as all three simultaniously, but generally not
     *          in the same tuple.
     *      Then, a tuple (Subject, Property, Object) represents and node-edge-node
     *          subgraph that connects the Subject node to the Object node through the
     *          Property edge (a directed edge).
     *      Resources and Literals can be subcategorized as Constants and Columns.
     *      Constants are constant Resources or Literals that can be defined and used anywhere
     *          in the entire resulting RDF graph.
     *      Columns are column defined Resources or Literals that are based on data rows or
     *          records that intersect with a specific column name to harvest data cell to
     *          include in the transform.  Column names are generally used to specify Property
     *          Resources while the cell data are used to specify Subject Resources and Object
     *          Resources or Literals.
     *      Additionally, Blank Nodes are special unnamed, i.e. blank, Resources used as
     *          connective tissue to generally group several related Objects for a Subject.
     *          Each Blank node is local graph unique and can be system defined or value based.
     *          Value based Blank Nodes can be Constant or Column defined.  Blank node have a
     *          specialized, graph local, naming scheme of the form "_:somename" where
     *          "somename" may be user specified as a placeholder for queries or inserts and
     *          system specified for query results.
     * 
     *      Subject Mappings:
     *      A Subject Mapping is a composite of a Subject, a Property Mapping, and a Type
     *          Mapping.
     *      The Subjects of the Subject Mapping list are the root nodes of the RDF Transform.
     * 
     *      Property Mapping:
     *      A Property Mapping is a composite of a single Property and an Object Mapping.
     *      A Property Mapping list is held by a Subject to describe its tuples.
     * 
     *      Type Mappings:
     *      A Type Mapping is a list of Resource Objects that define the RDF types assigned to
     *          a Subject.  They are like Property Mappings, but the Property is always
     *          "rdf:type" and the Objects are always Resources.  Therefore, a Type Mapping only
     *          holds Objects that provide type information for the Subject.
     *      A Type Mapping is held by a Subject to describe its type tuples.
     * 
     *      Object Mappings:
     *      An Object Mapping is a composite of a single Property and an Object Mapping.
     *      Objects in Properties may also serve as Subjects for other tuples.  Therefore,
     *      Objects may iterate on the above Subject Mappings.
     */
    static public RDFTransform reconstruct(JsonNode jnodeRoot) {
        if ( Util.isVerbose(2) ) logger.info("Reconstructing overlay...");

        if (jnodeRoot == null) {
            if ( Util.isVerbose(3) ) logger.info("  Nothing to reconstruct!");
            return null;
        }

        RDFTransform theTransform = new RDFTransform();

        //
        // JSON Header...
        //
        String strExtension = null;
        JsonNode jnodeExtension = jnodeRoot.get(RDFTransform.gstrExtension);
        if (jnodeExtension != null) {
            strExtension = jnodeExtension.asText();
            if (strExtension == null) {
                strExtension = "";
            }
        }
        String strVersion = null;
        JsonNode jnodeVersion = jnodeRoot.get(RDFTransform.gstrVersion);
        if (jnodeVersion != null) {
            strVersion = jnodeVersion.asText();
            if (strVersion == null) {
                strVersion = "";
            }
        }
        if ( Util.isVerbose(3) ) logger.info("  Found Extension: [" + strExtension + "]  Version: [" + strVersion + "]");

        //
        // Construct Base IRI from "baseIRI"...
        //
        JsonNode jnodeBaseIRI = null;
        if ( jnodeRoot.has(RDFTransform.gstrBaseIRI) ) {
            jnodeBaseIRI = jnodeRoot.get(RDFTransform.gstrBaseIRI);
            theTransform.setBaseIRI(jnodeBaseIRI);
        }
        else {
            if ( Util.isVerbose(2) ) logger.warn("  No Base IRI!");
        }

        //
        // Construct prefixesMap from "namespaces"...
        //
        //  NOTE: The map "thePrefixes" is just a convenience map to store and look up vocabularies
        //      based on the prefix as a key.
        Map<String, Vocabulary> thePrefixes = new HashMap<String, Vocabulary>();
        JsonNode jnodePrefixes = null;
        if ( jnodeRoot.has(RDFTransform.gstrNamespaces) ) {
            jnodePrefixes = jnodeRoot.get(RDFTransform.gstrNamespaces);
            if ( jnodePrefixes.isObject() ) {
                Iterator<Entry<String, JsonNode>> iterNodes = jnodePrefixes.fields();
                Map.Entry<String, JsonNode> mePrefix;
                while ( iterNodes.hasNext() ) {
                    mePrefix = (Map.Entry<String, JsonNode>) iterNodes.next();
                    String strPrefix = mePrefix.getKey();
                    String strIRI    = mePrefix.getValue().asText();
                    thePrefixes.put( strPrefix, new Vocabulary(strPrefix, strIRI) );
                }
                theTransform.setPrefixesMap(thePrefixes);
            }
        }
        else {
            if ( Util.isVerbose(2) ) logger.warn("  No Namespaces!");
        }

        //
        // Construct listRootNodes from "subjectMappings"...
        //
        JsonNode jnodeSubjectMappings = null;
        if ( jnodeRoot.has(RDFTransform.gstrSubjectMappings) ) {
            jnodeSubjectMappings = jnodeRoot.get(RDFTransform.gstrSubjectMappings);
            List<ResourceNode> theRootNodes = new ArrayList<ResourceNode>();
            if ( jnodeSubjectMappings.isArray() ) {
                for (JsonNode jnodeSubject : jnodeSubjectMappings) {
                    Node nodeRoot =
                        RDFTransform.reconstructNode( jnodeSubject, theTransform.getBaseIRI(), thePrefixes );
                    if (nodeRoot != null && nodeRoot instanceof ResourceNode) {
                        theRootNodes.add( (ResourceNode) nodeRoot );
                    }
                    // Otherwise, non-resource nodes (literals, generic nodes) cannot be root nodes.
                    // So, skip them.  They should never have been in the root node list anyway.
                }
            }
            theTransform.setRoots(theRootNodes);
        }
        else {
            if ( Util.isVerbose(2) ) logger.warn("  No Subjects!");
        }

        if ( Util.isVerbose(2) ) logger.info("...reconstructed overlay");
        return theTransform;
    }

    /*
     * Method reconstructNode()
     *
     *      Helper function for reconstruct()
     *
     *      Reconstruct any ol' Subject and their Properties.  Nodes are generic descriptors for a related
     *      transformation.  They are transformed into RDF Resource and Literal nodes to construct
     *      (Subject, Property, Object) tuples for an RDF graph.
     */
    static private Node reconstructNode(JsonNode jnodeSubject, final ParsedIRI baseIRI, Map<String, Vocabulary> thePrefixes) {
        Node nodeElement = null;
        if (jnodeSubject == null) {
            return nodeElement;
        }

        //
        // Get Subject's Prefix...
        //
        String strPrefix = null;
        if ( jnodeSubject.has(RDFTransform.gstrPrefix) ) {
            strPrefix = jnodeSubject.get(RDFTransform.gstrPrefix).asText();
        }

        //
        // Get Subject's Value Source...
        //
        //      Based on Source, get source information
        //
        String strSource = "";
        JsonNode jnodeValueSrc = null;
        if ( jnodeSubject.has(RDFTransform.gstrValueSource) ) {
            jnodeValueSrc = jnodeSubject.get(RDFTransform.gstrValueSource);
            if ( jnodeValueSrc.has(RDFTransform.gstrSource) ) {
                strSource = jnodeValueSrc.get(RDFTransform.gstrSource).asText();
            }
        }

        boolean bIsIndex = false;
        NodeType eNodeType = null;
        boolean bValueNode = false;
        boolean bConstNode = false;
        String strValue = null; // ..as Column Name or Constant
        boolean bIsExpression = false;

        if ( strSource.equals(RDFTransform.gstrRowIndex) ) {
            // A Row Index based node...
            bIsIndex = true;
            eNodeType = NodeType.ROW;
            bValueNode = true;
        }
        else if ( strSource.equals(RDFTransform.gstrRecordID) ) {
            // A Record Index (group of rows) based node...
            bIsIndex = true;
            eNodeType = NodeType.RECORD;
            bValueNode = true;
        }
        else if ( strSource.equals(RDFTransform.gstrColumn) && jnodeValueSrc.has(RDFTransform.gstrColumnName) ) {
            // A Column Name based node...
            strValue = jnodeValueSrc.get(RDFTransform.gstrColumnName).asText();
            eNodeType = NodeType.COLUMN;
            bValueNode = true;
        }
        else if ( strSource.equals(RDFTransform.gstrConstant) && jnodeValueSrc.has(RDFTransform.gstrConstant)) {
            // A Constant based node...
            strValue = jnodeValueSrc.get(RDFTransform.gstrConstant).asText();
            eNodeType = NodeType.CONSTANT;
            bConstNode = true;
        }
        else if ( strSource.equals(RDFTransform.gstrExpression) ) {
            // An Expression based node...
            eNodeType = NodeType.EXPRESSION;
            bIsExpression = true;
            // TODO: Not currently supported by itself.
            //      Expressions may be embedded in the others.
            //      Is it a Value or Constant node?
            //bValueNode = true;
            //bConstNode = true;
        }

        //
        // Get Subject's Expression...
        //
        String strExpLang = "grel"; // ...default
        String strExpCode = null;   // ...default, Node instances report "value" when null
        if ( jnodeSubject.has(RDFTransform.gstrExpression) ) {
            JsonNode jnodeExp = jnodeSubject.get(RDFTransform.gstrExpression);
            if ( jnodeExp.has(RDFTransform.gstrLanguage) ) {
                strExpLang = jnodeExp.get(RDFTransform.gstrLanguage).asText();
            }
            if ( jnodeExp.has(RDFTransform.gstrCode) ) {
                strExpCode = jnodeExp.get(RDFTransform.gstrCode).asText().strip();
            }
        }

        //
        // Get Subject's Value Type...
        //
        //      Based on Type, get type information
        //
        String strType = RDFTransform.gstrIRI;  // ...default for Root nodes
        JsonNode jnodeValueType = jnodeSubject; // ...for Root nodes
        boolean bResource = false;
        boolean bLiteral = false;
        if ( jnodeSubject.has(RDFTransform.gstrValueType) ) { // ...for Object nodes
            jnodeValueType = jnodeSubject.get(RDFTransform.gstrValueType);
            if ( jnodeValueType.has(RDFTransform.gstrType) ) {
                JsonNode jnodeType = jnodeValueType.get(RDFTransform.gstrType);
                String strTypeTemp = jnodeType.asText();
                if ( ! ( strTypeTemp == null || strTypeTemp.isEmpty() ) ) {
                    strType = strTypeTemp;
                }
            }
        }
        if ( strType.equals(RDFTransform.gstrIRI) ||
             strType.equals(RDFTransform.gstrBNode) ||
             strType.equals(RDFTransform.gstrValueBNode) ) {
            bResource = true;
        }
        if ( strType.equals(RDFTransform.gstrLiteral) ||
             strType.equals(RDFTransform.gstrDatatypeLiteral) ||
             strType.equals(RDFTransform.gstrLanguageLiteral) ) {
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
            if ( strType.equals(RDFTransform.gstrIRI) ) {
                if ( bValueNode ) {
                    rnodeResource = new CellResourceNode(strValue, strPrefix, strExpCode, bIsIndex);
                }
                else if ( bConstNode ) {
                    rnodeResource = new ConstantResourceNode(strValue, strPrefix);
                }
                else if ( eNodeType == NodeType.EXPRESSION ) {
                    // TODO: Currently unsupported
                }
            }
            else if ( strType.equals(RDFTransform.gstrBNode) ) {
                rnodeResource = new CellBlankNode(strValue, strExpCode, bIsIndex);
            }
            else if ( strType.equals(RDFTransform.gstrValueBNode) ) {
                rnodeResource = new ConstantBlankNode(strValue);
            }

            if ( rnodeResource != null ) {
                //
                // Construct Types from "typeMappings"...
                //
                RDFTransform.reconstructTypes(rnodeResource, jnodeSubject, thePrefixes);

                //
                // Construct Properties from "propertyMappings"...
                //
                RDFTransform.reconstructProperties(rnodeResource, jnodeSubject, baseIRI, thePrefixes);
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

            if ( strType.equals(RDFTransform.gstrLiteral) ) {
                // Nothing to do...
            }
            else if ( strType.equals(RDFTransform.gstrDatatypeLiteral) ) {
                // A Datatype Literal based node...
                JsonNode jnodeDatatype = jnodeValueType.get(RDFTransform.gstrDatatype);
                if (jnodeDatatype != null) {
                    // ...get prefix, if exists...
                    JsonNode jnodeDatatypePrefix = jnodeDatatype.get(RDFTransform.gstrPrefix);
                    if (jnodeDatatypePrefix != null) {
                        strDatatypePrefix = jnodeDatatypePrefix.asText();
                    }
                    // ...get IRI value, always a constant...
                    JsonNode jnodeValueSource = jnodeDatatype.get(RDFTransform.gstrValueSource);
                    if (jnodeValueSource != null) {
                        JsonNode jnodeDatatypeConstant = jnodeValueSource.get(RDFTransform.gstrConstant);
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
                                        logger.error("ERROR: Bad Namespace in Prefixes: " + strDatatypeNamespace, ex);
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
            else if ( strType.equals(RDFTransform.gstrLanguageLiteral) ) {
                strLanguageCode = jnodeValueType.get(RDFTransform.gstrLanguage).asText();
            }

            if ( bValueNode ) {
                lnodeLiteral = new CellLiteralNode(strValue, strExpCode, bIsIndex, nodeDatatype, strLanguageCode);
            }
            else if ( bConstNode ) {
                lnodeLiteral = new ConstantLiteralNode(strValue, nodeDatatype, strLanguageCode);
            }
            else if ( eNodeType == NodeType.EXPRESSION ) {
                // TODO: Currently unsupported - what is an expression? Value or Constant?
            }

            nodeElement = lnodeLiteral;
        }

        return nodeElement;
    }

    /*
     * Method reconstructProperties()
     *
     *      Helper function for reconstruct()
     *
     *      Reconstruct the Node Properties.  Nodes are generic descriptors for a related
     *      transformation.  They are transformed into RDF Resource and Literal nodes to construct
     *      (Subject, Property, Object) tuples for an RDF graph.  This method constructs the Properties
     *      for regular statements (Subject, Property, Object).
     */
    static private void reconstructProperties(ResourceNode rnodeParent, JsonNode jnodeParent, final ParsedIRI baseIRI, Map<String, Vocabulary> thePrefixes) {
        if ( ! jnodeParent.has(RDFTransform.gstrPropertyMappings) ) {
            return;
        }

        JsonNode jnodePropertyMappings = jnodeParent.get(RDFTransform.gstrPropertyMappings);
        if ( jnodePropertyMappings.isArray() ) {
            return;
        }

        for (JsonNode jnodeProperty : jnodePropertyMappings) {
            //
            // Get Property's Namespace Prefix...
            //
            String strPrefix = null;
            if ( jnodeProperty.has(RDFTransform.gstrPrefix) ) {
                strPrefix = jnodeProperty.get(RDFTransform.gstrPrefix).asText();
            }

            //
            // Get Property's Value Source...
            //
            //      Based on Source, get source information
            //
            //  NOTE: All property sources are currently "constant"
            //
            String strSource = "";
            JsonNode jnodeValueSrc = null;
            if ( jnodeProperty.has(RDFTransform.gstrValueSource) ) {
                jnodeValueSrc = jnodeProperty.get(RDFTransform.gstrValueSource);
                if ( jnodeValueSrc.has(RDFTransform.gstrSource) ) {
                    strSource = jnodeValueSrc.get(RDFTransform.gstrSource).asText();
                }
            }

            //boolean bIsIndex = false;
            NodeType eNodeType = null;
            boolean bValueNode = false;
            boolean bConstNode = false;
            String strValue = null;
            boolean bIsExpression = false;

            //if ( strSource.equals(RDFTransform.gstrRowIndex) ) {
            //    // A Row Index based node...
            //    bIsIndex = true;
            //    eNodeType = NodeType.ROW;
            //    bValueNode = true;
            //}
            //else if ( strSource.equals(RDFTransform.gstrRecordID) ) {
            //    // A Record Index (group of rows) based node...
            //    bIsIndex = true;
            //    eNodeType = NodeType.RECORD;
            //    bValueNode = true;
            //}
            if ( strSource.equals(RDFTransform.gstrColumn) && jnodeValueSrc.has(RDFTransform.gstrColumnName) ) {
                // A Column Name based node...
                strValue = jnodeValueSrc.get(RDFTransform.gstrColumnName).asText();
                eNodeType = NodeType.COLUMN;
                bValueNode = true;
            }
            else if ( strSource.equals(RDFTransform.gstrConstant) && jnodeValueSrc.has(RDFTransform.gstrConstant)) {
                // A Constant based node...
                strValue = jnodeValueSrc.get(RDFTransform.gstrConstant).asText();
                eNodeType = NodeType.CONSTANT;
                bConstNode = true;
            }
            else if ( strSource.equals(RDFTransform.gstrExpression) ) {
                // An Expression based node...
                eNodeType = NodeType.EXPRESSION;
                bIsExpression = true;
                // TODO: Not currently supported by itself.
                //      Expressions may be embedded in the others.
                //      Is it a Value or Constant node?
                //bValueNode = true;
                //bConstNode = true;
            }
            if (strValue == null) {
                logger.error("ERROR: Bad Property: Source: " + strSource);
                continue;
            }

            //
            // Get Property's Expression...
            //
            String strExpLang = "grel"; // ...default
            String strExpCode = null;   // ...default, Node instances report "value" when null
            if ( jnodeProperty.has(RDFTransform.gstrExpression) ) {
                JsonNode jnodeExp = jnodeProperty.get(RDFTransform.gstrExpression);
                if ( jnodeExp.has(RDFTransform.gstrLanguage) ) {
                    strExpLang = jnodeExp.get(RDFTransform.gstrLanguage).asText();
                }
                if ( jnodeExp.has(RDFTransform.gstrCode) ) {
                    strExpCode = jnodeExp.get(RDFTransform.gstrCode).asText().strip();
                }
            }

            //
            // Process Property into a Node...
            //
            //  TODO NOTE: At this time, we are doing nothing more than checking that a node can be created
            //      from the property information.
            //
            ResourceNode rnodeResource = null;
            if ( bValueNode ) {
                rnodeResource = new CellResourceNode(strValue, strPrefix, strExpCode, false);
            }
            else if ( bConstNode ) {
                rnodeResource = new ConstantResourceNode(strValue, strPrefix);
            }
            else if ( eNodeType == NodeType.EXPRESSION ) {
                // TODO: Currently unsupported
            }
            if (rnodeResource == null) {
                logger.error("ERROR: Bad Property: IRI: " + strValue);
                continue;
            }

            // TODO: Change following for Property storage to prefix, IRI, & nodeObject
            String strIRI = strValue;
            if (bValueNode) {
                // TODO: Unsupported. Need to evaluate expression on column to get IRI...or later
                //strIRI = strValue;
            }
            else if (bConstNode) {
                strIRI = strValue;
            }
            String strCIRIE = strIRI;
            if (strPrefix != null) {
                strCIRIE = strPrefix + ":" + strValue;
                Vocabulary vocab = thePrefixes.get(strPrefix);
                if (vocab != null) {
                    strIRI = vocab.getNamespace() + strValue;
                }
            }

            //
            // Get Property's Object Mappings...
            //
            List<Node> theObjectNodes = new ArrayList<Node>();
            if ( jnodeProperty.has(RDFTransform.gstrObjectMappings) ) {
                JsonNode jnodeObjectMappings = jnodeProperty.get(RDFTransform.gstrObjectMappings);
                if ( jnodeObjectMappings.isArray() ) {
                    Node nodeObject = null;
                    for (JsonNode jnodeObject : jnodeObjectMappings) {
                        nodeObject = RDFTransform.reconstructNode( jnodeObject, baseIRI, thePrefixes );
                        if (nodeObject != null) {
                            theObjectNodes.add(nodeObject);
                        }
                    }
                }
            }
            if ( theObjectNodes.isEmpty() ) {
                theObjectNodes.add(null);
            }

            for ( Node nodeObj : theObjectNodes ) {
                rnodeParent.addProperty(
                    new Property(strIRI, strCIRIE, nodeObj)
                );
            }
        }
    }

    /*
     * Method reconstructTypes()
     *
     *      Helper function for reconstruct()
     *
     *      Reconstruct the Node Types.  Nodes are generic descriptors for a related
     *      transformation.  They are transformed into RDF Resource and Literal nodes to construct
     *      (Subject, Property, Object) tuples for an RDF graph.  This method constructs the Types
     *      for type statements (Subject, a, TypeObject).
     * 
     */
    static private void reconstructTypes(ResourceNode rnodeParent, JsonNode jnodeParent, Map<String, Vocabulary> thePrefixes) {
    	if ( ! jnodeParent.has(RDFTransform.gstrTypeMappings) ) {
            return;
        }

        JsonNode jnodeTypeMappings = jnodeParent.get(RDFTransform.gstrTypeMappings);
        if ( ! jnodeTypeMappings.isArray() ) {
            return;
        }

        for (JsonNode jnodeType : jnodeTypeMappings) {
            //
            // Get Type's Namespace Prefix...
            //
            String strPrefix = null;
            if ( jnodeType.has(RDFTransform.gstrPrefix) ) {
                strPrefix = jnodeType.get(RDFTransform.gstrPrefix).asText();
            }

            //
            // Get Type's Value Source...
            //
            //      Based on Source, get source information
            //
            String strSource = "";
            String strValue = null;
            if ( jnodeType.has(RDFTransform.gstrValueSource) ) {
                JsonNode jnodeValueSrc = jnodeType.get(RDFTransform.gstrValueSource);
                if ( jnodeValueSrc.has(RDFTransform.gstrSource) ) {
                    strSource = jnodeValueSrc.get(RDFTransform.gstrSource).asText();
                }
                if ( strSource.equals(RDFTransform.gstrConstant) && jnodeValueSrc.has(RDFTransform.gstrConstant)) {
                    strValue = jnodeValueSrc.get(RDFTransform.gstrConstant).asText();
                }
            }
            if (strValue == null) {
                logger.error("ERROR: Bad Property: Source: " + strSource);
                continue;
            }

            // TODO: Change following for RDFType storage to prefix & IRI
            String strIRI = strValue;
            String strCIRIE = strIRI;
            if (strPrefix != null) {
                strCIRIE = strPrefix + ":" + strValue;
                Vocabulary vocab = thePrefixes.get(strPrefix);
                if (vocab != null) {
                    strIRI = vocab.getNamespace() + strValue;
                }
                
            }
            rnodeParent.addType( new RDFType(strIRI, strCIRIE) );
        }
    }

    /****************************************************************************************************
     ****************************************************************************************************
     *
     *  Instance Variables
     * 
     ****************************************************************************************************
     ****************************************************************************************************/

    /*
     * Base IRI for Document
     *
     *  A base IRI is the default namespace used for resources lacking a formal namespace.
     *
     *    Given a base IRI:
     *      http://example.com/
     *    and a raw column name used for a property:
     *      first name
     *    the resulting IRI for the property would be:
     *      http://example.com/first_name
     */
    @JsonIgnore
    private ParsedIRI theBaseIRI;

    /*
     * Prefix Mapping to Namespace
     * 
     *   A defined Prefix mapping consists of a:
     *       1) Prefix - a short name for a Namespace
     *       2) Namespace - a full IRI
     *
     *   For this mapping:
     *       Keys:   the Namespace Prefix
     *       Values: the full IRI Namespace
     * 
     *   Example:
     *       Prefix  Namespace
     *       (Key)   (Value)
     *       ------  ----------------------------------------
     *       foaf    http://xmlns.com/foaf/0.1/
     *
     *   Prefixes are use to construct Condensed IRI Expressions (CIRIE).
     *   For an IRI:
     *       http://xmlns.com/foaf/0.1/knows
     *   a CIRIE using the above prefix is:
     *       foaf:knows
     */ 
    @JsonIgnore
    private Map<String, Vocabulary> thePrefixes;

    /*
     * Root Nodes for Document
     *
     *  A root node is any subject element  designated in the transform.
     */
    //@JsonProperty("rootNodes")
    @JsonIgnore
    private List<ResourceNode> theRootNodes;

    /****************************************************************************************************
     ****************************************************************************************************
     *
     *  Instance Methods
     * 
     ****************************************************************************************************
     ****************************************************************************************************/

    /*
        Constructors
    */
    @JsonCreator
    public RDFTransform() {
        if ( Util.isVerbose(2) ) logger.info("Created empty overlay");
    }

    public RDFTransform(ApplicationContext theContext, Project theProject)
            throws IOException {
        if ( Util.isVerbose(2) ) logger.info("Creating base overlay for project from context...");

        this.theBaseIRI = Util.buildIRI( theContext.getDefaultBaseIRI() );
       	this.thePrefixes = this.clonePrefixes( theContext.getPredefinedVocabularyManager().getPredefinedVocabulariesMap() );
       	// Copy the index of predefined vocabularies...
       	//   Each project will have its own copy of these predefined vocabs to enable, delete, update...
       	theContext.getVocabularySearcher().addPredefinedVocabulariesToProject(theProject.id);
        this.theRootNodes = new ArrayList<ResourceNode>();

        if ( Util.isVerbose(2) ) logger.info("Created overlay");
    }

    /*
        Methods
    */
    private Map<String, Vocabulary> clonePrefixes(Map<String, Vocabulary> mapPrefixes) {
    	Map<String, Vocabulary> mapPrefixesCopy = new HashMap<String, Vocabulary>();

        for ( Entry<String, Vocabulary> entryPrefix : mapPrefixes.entrySet() ) {
    		mapPrefixesCopy.put(
                entryPrefix.getKey(),
                new Vocabulary(
                    entryPrefix.getValue().getPrefix(),
                    entryPrefix.getValue().getNamespace() )
            );
    	}

    	return mapPrefixesCopy;
    }

    @JsonIgnore // ...see getBaseIRIAsString()
    public ParsedIRI getBaseIRI() {
        return this.theBaseIRI;
    }

    @JsonProperty(RDFTransform.gstrBaseIRI)
    public String getBaseIRIAsString() {
        return this.theBaseIRI.toString();
    }

    @JsonIgnore // ...see setBaseIRI(JsonNode)
    public void setBaseIRI(ParsedIRI iriBase)  {
        if ( Util.isVerbose(2) ) logger.info("Setting Base IRI...");
        this.theBaseIRI = iriBase;
    }

    @JsonProperty(RDFTransform.gstrBaseIRI)
    public void setBaseIRI(JsonNode jnodeBaseIRI)  {
        if ( Util.isVerbose(2) ) logger.info("Setting Base IRI from JSON Node...");

        // If it is not a direct string representation, then these keys are stored:
        // { "scheme"   : null or scheme in "scheme:" from beginning of string,
        //   "userInfo" : null or user:password in "://user:password@",
        //   "host"     : null or host in "@host:",
        //   "port"     : -1 or a port as integer (0 - 65535) in ":port/",
        //   "path"     : null or path in "path?" including starting /,
        //   "query"    : null or query in "?query#",
        //   "fragment" : null or fragment in "#fragment" to end of string,
        //   "absolute" : true if there is enough required for a "complete" IRI,
        //   "opaque"   : false if there is no "//", just :string
        // }

        ParsedIRI iriBase = null;
        if ( jnodeBaseIRI != null && ! jnodeBaseIRI.isNull() ) {
            String strBaseIRI;
            if ( jnodeBaseIRI.isValueNode() ) {
                // Get the Base IRI string...
                strBaseIRI = jnodeBaseIRI.asText();
                iriBase = Util.buildIRI( strBaseIRI );
            }
            else {
                // Get the Base IRI components...
                String  strScheme   = jnodeBaseIRI.get("scheme"  ).asText();
                String  strUserInfo = jnodeBaseIRI.get("userInfo").asText();
                String  strHost     = jnodeBaseIRI.get("host"    ).asText();
                int     iPort       = jnodeBaseIRI.get("port"    ).asInt(-1);
                String  strPath     = jnodeBaseIRI.get("path"    ).asText();
                String  strQuery    = jnodeBaseIRI.get("query"   ).asText();
                String  strFragment = jnodeBaseIRI.get("fragment").asText();
                boolean bAbsolute   = jnodeBaseIRI.get("absolute").asBoolean();
                boolean bOpaque     = jnodeBaseIRI.get("opaque"  ).asBoolean();

                // Construct the Base IRI string from the components...
                strBaseIRI = "";
                if ( strScheme != null && ! strScheme.isEmpty() )
                    strBaseIRI += strScheme + ":";
                if ( strHost != null && ! strHost.isEmpty() ) {
                    strBaseIRI += "//";
                    if ( strUserInfo != null && ! strUserInfo.isEmpty() )
                        strBaseIRI += strUserInfo + "@";
                    strBaseIRI += strHost;
                    if ( iPort > -1 )
                        strBaseIRI += ":" + Integer.toString(iPort);
                }
                if ( strPath != null && ! strPath.isEmpty() )
                    strBaseIRI += strPath;
                if ( strQuery != null && ! strQuery.isEmpty() )
                    strBaseIRI += "?" + strQuery;
                if ( strFragment != null && ! strFragment.isEmpty() )
                    strBaseIRI += "#" + strFragment;

                // Realize the Base IRI...
                iriBase = Util.buildIRI( strBaseIRI );

                // Sanity check the Base IRI against remaining components...
                if (iriBase != null) {
                    if (iriBase.isAbsolute() != bAbsolute) {
                        if ( Util.isVerbose() ) logger.warn("Mismatch reconstructing Base IRI: Absolute Value");
                    }
                    if (iriBase.isOpaque() != bOpaque) {
                        if ( Util.isVerbose() ) logger.warn("Mismatch reconstructing Base IRI: Opaque Value");
                    }
                }
            }
        }
        if (iriBase != null) {
            this.theBaseIRI = iriBase;
        }
    }

    @JsonIgnore
    public Map<String, Vocabulary> getPrefixesMap() {
		return this.thePrefixes;
	}

    @JsonIgnore
    public void setPrefixesMap(Map<String, Vocabulary> mapPrefixes) {
        this.thePrefixes = mapPrefixes;
	}

    public void addPrefix(String strName, String strIRI)
            throws PrefixExistException {
        if (this.thePrefixes == null) {
            this.thePrefixes = new HashMap<String, Vocabulary>();
        }
        synchronized(this.thePrefixes) {
    		if ( this.thePrefixes.containsKey(strName) ) {
    		    throw new PrefixExistException(strName + " already defined");
    		}
    		this.thePrefixes.put( strName, new Vocabulary(strName, strIRI) );
    	}
    }

    public void removePrefix(String strName) {
        synchronized(this.thePrefixes) {
            this.thePrefixes.remove(strName);
        }
    }

    @JsonProperty(RDFTransform.gstrNamespaces)
    public Collection<Vocabulary> getPrefixes() {
        if ( Util.isVerbose(2) ) logger.info("Getting prefixes...");
        if (thePrefixes != null) {
    	    return this.thePrefixes.values();
        }
        return null;
    }

    @JsonProperty(RDFTransform.gstrNamespaces)
    public void setPrefixes(Vocabulary[] aVocabularies) {
        if ( Util.isVerbose(2) ) logger.info("Setting prefixes...");
        if (this.thePrefixes == null) {
            this.thePrefixes = new HashMap<String, Vocabulary>();
        }
        synchronized(this.thePrefixes) {
            this.thePrefixes.clear();
            for (Vocabulary vocab : aVocabularies) {
                this.thePrefixes.put( vocab.getPrefix(), vocab );
            }
        }
    }

    @JsonProperty("subjectMappings")
	public List<ResourceNode> getRoots() {
        if ( Util.isVerbose(2) ) logger.info("Getting root nodes: size = " + this.theRootNodes.size());
        return this.theRootNodes;
    }

    @JsonProperty("subjectMappings")
	public void setRoots(List<ResourceNode> listRootNodes) {
        if ( Util.isVerbose(2) ) logger.info("Setting root nodes...");
        this.theRootNodes = listRootNodes;
    }

    @Override
    public void onBeforeSave(Project theProject) {
        if ( Util.isVerbose(2) ) logger.info("Saving...");
    }

    @Override
    public void onAfterSave(Project theProject) {
        if ( Util.isVerbose(2) ) logger.info("...saved.");
    }

    @Override
    public void dispose(Project theProject) {
	   /*try {
			ApplicationContext.instance().getVocabularySearcher().deleteProjectVocabularies(String.valueOf(theProject.id));
		}
        catch (ParseException ex) {
			logger.error("ERROR: Unable to delete index for project " + theProject.id, ex);
		}
        catch (IOException ex) {
			logger.error("ERROR: Unable to delete index for project " + theProject.id, ex);
		}*/
        if ( Util.isVerbose(2) ) logger.info("Disposed overlay");
    }

    /*
     * Method: write(JsonGenerator theWriter)
     * 
     * This write() method produces the JSON object containing the entire RDF Transform template.
     * The template's format is:
     * 
     * The Base IRI
     * The Namespaces ( "namespaces" ) containing each namespace keyed to its prefix
     * The Subject Mappings array of Subjects ( "subjectMappings" ) consisting of
     *      the Subject's Prefix ( "prefix" )
     *      the Subject's Source ( "valueSource" )
     *      the Subject's Expression ( "expression" )
     *      the Subject's Type Mappings ( "typeMappings" ) array of Types [via rdf:type ("a") property, an IRI] consisting of
     *          the Type's Prefix (prefix)
     *          the Type's Source (valueSource)
     *          the Type's Expression (expression)
     *      the Subject's Property Mappings ( "propertyMappings" ) array of Properties consisting of
     *          the Property's Prefix (prefix)
     *          the Property's Source (valueSource)
     *          the Property's Expression (expression)
     *          the Property's Objects (values) array of Objects [resource IRI or literal Data] consisting of
     *              the Object's Prefix (prefix) [for resources]
     *              the Object's Source (valueSource)
     *              the Object's Type (valueType) [Datatype for literals]
     *              the Object's Expression (expression)
     * 
     * "baseIRI" : an IRI
     * "namespaces" : {
     *      a prefix : a namespace IRI,
     *      ...
     * }
     * "subjectMappings" : [
     *      {   "prefix" : a prefix,
     *          "valueSource" : { },
     *          "expression": { }
     *          "typeMappings" : [ { }, ... ],
     *          "propertyMappings" : [ { }, ... ]
     *      },
     *      ...
     * ]
     * 
     * "typeMappings" : [
     *      {   "prefix" : a prefix,
     *          "valueSource" : { },
     *          "expression": { }
     *      },
     *      ...
     * ]
     * "propertyMappings" : [
     *      {   "prefix" : a prefix,
     *          "valueSource" : { },
     *          "expression": { },
     *          "objectMappings": [ { }, ... ]
     *      },
     *      ...
     * ]
     * "objectMappings": [
     *      {   "prefix" : a prefix,
     *          "valueSource" : { },
     *          "valueType" : { },
     *          "expression": { }
     *      },
     *      ...
     * ]
     * 
     * "prefix" : a prefix from "namespaces"
     * 
     * "valueSource" : {
     *      "source": a source ["row_index", "record_id", "constant", "column", "expression"],
     *    for "constant":
     *      "constant": a constant value string
     *    for "column":
     *      "columnName": a column name from the data store
     * }
     * 
     * "valueType" : {
     *      "type": a type ["iri", "literal", "language_literal", "datatype_literal", "value_bnode", "bnode"],
     *    for "iri":
     *      "typeMappings": []
     *      "propertyMappings": [],
     *    for "language_literal":
     *      "language": a language code ("en", ...)
     *    for "datatype_literal":
     *      "datatype": {
     *          "prefix" : a prefix,
     *          "valueSource": { }
     *          "expression": { },
     *      }
     * }
     *      
     * "expression": {
     *      "language": a code language such as "grel",
     *      "code": the code language expression
     * }
     */
    public void write(JsonGenerator theWriter)
            throws IOException {
        theWriter.writeStartObject();

        //
        // The Base IRI (baseIRI)...
        //
        theWriter.writeStringField(RDFTransform.gstrBaseIRI, this.theBaseIRI.toString());

        //
        // The Namespaces (namespaces)...
        //
        theWriter.writeFieldName(RDFTransform.gstrNamespaces);
        theWriter.writeStartObject();
        for ( Vocabulary vocab : this.thePrefixes.values() ) {
            vocab.write(theWriter);
        }
        theWriter.writeEndObject();

        //
        // The Subject Mappings (subjectMappings) array...
        //
        theWriter.writeFieldName("subjectMappings");
        theWriter.writeStartArray();
        for (Node nodeRoot : this.theRootNodes) {
            nodeRoot.write(theWriter);
        }
        theWriter.writeEndArray();

        theWriter.writeEndObject();

        theWriter.flush();
    }
}
