package com.google.refine.rdf.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.refine.rdf.model.vocab.VocabularyList;

import org.eclipse.rdf4j.common.net.ParsedIRI;

//import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
//import com.fasterxml.jackson.annotation.JsonInclude;
//import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Class Link: A Predicate-Object collection
//		The Predicate is stored as:
//			Absolute IRI
//			Prefixed IRI
//		The Object is stored as:
//			a data strucure defining the object node:
//				a Column Value, natural or calculated, resource or literal
//				a Constant Value, natural or calculated, resource or literal
//
//		NOTE: Blank node resources have distinct processing requirements
//				not found here.
//
@JsonIgnoreType
public class Property {
	static private final Logger logger = LoggerFactory.getLogger("RDFT:Property");

    static public final class PropertyReconstructor {
        private PropertyReconstructor() {};
    }

    // Reconstruction Validator
    static private final PropertyReconstructor thePropReconstructor = new PropertyReconstructor();

    /*
     * Method reconstructProperties()
     *
     *      Helper function for RDFTransform.reconstruct()
     *
     *      Reconstruct the Node Properties.  Nodes are generic descriptors for a related
     *      transformation.  They are transformed into RDF Resource and Literal nodes to construct
     *      (Subject, Property, Object) tuples for an RDF graph.  This method constructs the Properties
     *      for regular statements (Subject, Property, Object).
     */
    static public void reconstructProperties(
							Node.NodeReconstructor theNodeReconstructor,
							ResourceNode rnodeParent, JsonNode jnodeParent, final ParsedIRI baseIRI,
                            VocabularyList theNamespaces) {
        Objects.requireNonNull(theNodeReconstructor);

		Property.reconstructProperties(rnodeParent, jnodeParent, baseIRI, theNamespaces);
	}

    static private void reconstructProperties(
							ResourceNode rnodeParent, JsonNode jnodeParent, final ParsedIRI baseIRI,
                            VocabularyList theNamespaces) {
		if ( ! jnodeParent.has(Util.gstrPropertyMappings) ) {
            return;
        }

        JsonNode jnodePropertyMappings = jnodeParent.get(Util.gstrPropertyMappings);
        if ( ! jnodePropertyMappings.isArray() ) {
            return;
        }

        for (JsonNode jnodeProperty : jnodePropertyMappings) {
            //
            // Get Property's Namespace Prefix...
            //
            String strPrefix = null;
            if ( jnodeProperty.has(Util.gstrPrefix) ) {
                strPrefix = jnodeProperty.get(Util.gstrPrefix).asText();
            }

            //
            // Get Property's Value Source...
            //
            //      Based on Source, get source information
            //
            //  TODO NOTE: All property sources are currently "constant"
			//		Future support for other sources
            //
            String strSource = "";
            JsonNode jnodeValueSrc = null;
            if ( jnodeProperty.has(Util.gstrValueSource) ) {
                jnodeValueSrc = jnodeProperty.get(Util.gstrValueSource);
                if ( jnodeValueSrc.has(Util.gstrSource) ) {
                    strSource = jnodeValueSrc.get(Util.gstrSource).asText();
                }
            }

            boolean bIsIndex = false;
            Util.NodeType eNodeType = null;
            boolean bValueNode = false;
            boolean bConstNode = false;
            String strValue = null;
            //boolean bIsExpression = false;

            //  TODO NOTE: All property sources are currently "constant"
			//		Future support for other sources
            //
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

            if (strValue == null) {
                Property.logger.error("ERROR: Bad Property: Source: " + strSource);
                continue;
            }

            //
            // Get Property's Expression...
            //
            String strExpLang = Util.gstrGREL; // ...default
            String strExpCode = null;          // ...default, Node instances report "value" when null
            if ( jnodeProperty.has(Util.gstrExpression) ) {
                JsonNode jnodeExp = jnodeProperty.get(Util.gstrExpression);
                if ( jnodeExp.has(Util.gstrLanguage) ) {
                    strExpLang = jnodeExp.get(Util.gstrLanguage).asText();
                }
                if ( jnodeExp.has(Util.gstrCode) ) {
                    strExpCode = jnodeExp.get(Util.gstrCode).asText().strip();
                }
            }

            //
            // Process Property into a Node...
            //
            //  TODO NOTE: At this time, we are doing nothing more than checking that a node can be created
            //      from the property information.  We should store it and use it like root nodes to manage
            //      IRI declarations, expressions, etc (no literals)
            //
            ResourceNode rnodeResource = null;
            if ( bValueNode ) {
                rnodeResource = new CellResourceNode(strValue, strPrefix, strExpCode, bIsIndex, eNodeType);
            }
            else if ( bConstNode ) {
                rnodeResource = new ConstantResourceNode(strValue, strPrefix);
            }
            //else if ( bIsExpression ) {
            //    // TODO: Currently unsupported
            //}
            if (rnodeResource == null) {
                Property.logger.error("ERROR: Bad Property: Prefix: [" + strPrefix + "]  Src: [" + strSource + "]  Val: [" + strValue + "]  Exp: [" + strExpCode + "]");
                continue;
            }

            //
            // Get Property's Object Mappings...
            //
            List<Node> theObjectNodes = new ArrayList<Node>();
            if ( jnodeProperty.has(Util.gstrObjectMappings) ) {
                JsonNode jnodeObjectMappings = jnodeProperty.get(Util.gstrObjectMappings);
                if ( jnodeObjectMappings.isArray() ) {
                    Node nodeObject = null;
                    for (JsonNode jnodeObject : jnodeObjectMappings) {
                        nodeObject = Node.reconstructNode(thePropReconstructor, jnodeObject, baseIRI, theNamespaces);
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
                    new Property(strPrefix, strValue, nodeObj)
                );
            }
        }
    }

	// The Property: A Prefix for the LocalPart
	//		(to create a CIRIE: Condensed IRI Expression)
    private final String strPrefix;

	// The Property: The Local Part of the IRI (or a Full IRI when Prefix is null)
    private final String strLocalPart;

	// The Target: A source's target "node" connected via this Property
    // TODO: Future multiple targets need adjustment on listNodeObject throughout this class
    private final Node nodeObject;

    public Property(String strPrefix, String strLocalPart, Node nodeObject) {
        this.strPrefix  = Util.toSpaceStrippedString(strPrefix);
        this.strLocalPart = Util.toSpaceStrippedString(strLocalPart);
        this.nodeObject = nodeObject;
    }

	public String getPrefix() {
		return this.strPrefix;
	}

	public String getPathProperty() {
		return this.strLocalPart;
	}

    public String getPrefixedProperty() {
        if (this.strPrefix != null) {
		    return this.strPrefix + ":" + this.strLocalPart.replaceAll("\\/", "/").replaceAll("/", "\\/");
        }
        return this.strLocalPart;
	}

	public Node getObject() {
		return this.nodeObject;
	}

	public void write(JsonGenerator writer)
			throws JsonGenerationException, IOException {
		writer.writeStartObject();

        if (this.strPrefix != null) {
		    writer.writeStringField(Util.gstrPrefix, this.strPrefix);
        }

        // TODO: Modify for Future Non-Constant Value Source...
		writer.writeObjectFieldStart(Util.gstrValueSource);
        writer.writeStringField(Util.gstrSource, Util.gstrConstant);
        writer.writeStringField(Util.gstrConstant, this.strLocalPart);
		writer.writeEndObject();

		// TODO: Future Expression Store...
		//writer.writeFieldName(Util.gstrExpression);
        //writer.writeStartObject();
        //writer.writeStringField(Util.gstrLanguage, Util.gstrGREL);
        //writer.writeStringField(Util.gstrCode, this.strExpCode);
		//writer.writeEndObject();

		if (this.nodeObject != null) {
			writer.writeArrayFieldStart(Util.gstrObjectMappings);
			this.nodeObject.write(writer, false);
			writer.writeEndArray();
		}

		writer.writeEndObject();
	}
}
