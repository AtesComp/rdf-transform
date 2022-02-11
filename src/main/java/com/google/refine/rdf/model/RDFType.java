package com.google.refine.rdf.model;

import java.io.IOException;
import java.util.Objects;

import com.google.refine.rdf.model.vocab.Vocabulary;
import com.google.refine.rdf.model.vocab.VocabularyList;

import org.eclipse.rdf4j.common.net.ParsedIRI;

//import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
//import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonIgnoreType
public class RDFType {
	static private final Logger logger = LoggerFactory.getLogger("RDFT:RDFType");

    /*
     * Method reconstructTypes()
     *
     *      Helper function for RDFTransform.reconstruct()
     *
     *      Reconstruct the Node Types.  Nodes are generic descriptors for a related
     *      transformation.  They are transformed into RDF Resource and Literal nodes to construct
     *      (Subject, Property, Object) tuples for an RDF graph.  This method constructs the Types
     *      for type statements (Subject, a, TypeObject).
     * 
     */
    static public void reconstructTypes(
                            Node.NodeReconstructor theNodeReconstructor,
                            ResourceNode rnodeParent, JsonNode jnodeParent, final ParsedIRI baseIRI,
                            VocabularyList theNamespaces) {
        Objects.requireNonNull(theNodeReconstructor);

        RDFType.reconstructTypes(rnodeParent, jnodeParent, baseIRI, theNamespaces);
    }

    static private void reconstructTypes(
                            ResourceNode rnodeParent, JsonNode jnodeParent, final ParsedIRI baseIRI,
                            VocabularyList theNamespaces) {
        if ( ! jnodeParent.has(Util.gstrTypeMappings) ) {
            return;
        }

        JsonNode jnodeTypeMappings = jnodeParent.get(Util.gstrTypeMappings);
        if ( ! jnodeTypeMappings.isArray() ) {
            return;
        }

        for (JsonNode jnodeType : jnodeTypeMappings) {
            //
            // Get Type's Namespace Prefix...
            //
            String strPrefix = null;
            if ( jnodeType.has(Util.gstrPrefix) ) {
                strPrefix = jnodeType.get(Util.gstrPrefix).asText();
            }

            //
            // Get Type's Value Source...
            //
            //      Based on Source, get source information
            //
            String strSource = "";
            String strValue = null;
            if ( jnodeType.has(Util.gstrValueSource) ) {
                JsonNode jnodeValueSrc = jnodeType.get(Util.gstrValueSource);
                if ( jnodeValueSrc.has(Util.gstrSource) ) {
                    strSource = jnodeValueSrc.get(Util.gstrSource).asText();
                }
                if ( strSource.equals(Util.gstrConstant) && jnodeValueSrc.has(Util.gstrConstant)) {
                    strValue = jnodeValueSrc.get(Util.gstrConstant).asText();
                }
            }
            if (strValue == null) {
                RDFType.logger.error("ERROR: Bad Property: Source: " + strSource);
                continue;
            }

            rnodeParent.addType( new RDFType(strPrefix, strValue) );
        }
    }

    private String strLocalPart;
	private String strPrefix;

    public RDFType(String strPrefix, String strLocalPart) {
        this.strPrefix  = Util.toSpaceStrippedString(strPrefix);
        this.strLocalPart = Util.toSpaceStrippedString(strLocalPart);
    }

    public String getPrefix() {
    	return this.strPrefix;
    }

    public String getLocalPart() {
		return this.strLocalPart;
	}

    public String getPrefixedIRI() {
        if (this.strPrefix != null) {
		    return this.strPrefix + ":" + this.strLocalPart;
        }
        return this.strLocalPart;
	}

    public String getFullIRI(VocabularyList theNamespaces) {
        if (this.strPrefix != null) {
            Vocabulary vocab = theNamespaces.findByPrefix(this.strPrefix);
            if (vocab != null) {
                return vocab.getNamespace() + this.strLocalPart;
            }
            return this.strPrefix + ":" + this.strLocalPart;
        }
        return this.strLocalPart;
	}

    public void write(JsonGenerator writer)
            throws JsonGenerationException, IOException {
        writer.writeStartObject();

        if (this.strPrefix != null) {
            writer.writeStringField(Util.gstrPrefix, this.strPrefix);
        }

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

        writer.writeEndObject();
    }
}