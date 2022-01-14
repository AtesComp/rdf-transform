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
     *      Helper function for reconstruct()
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
                            VocabularyList thePrefixes) {
        Objects.requireNonNull(theNodeReconstructor);

        RDFType.reconstructTypes(rnodeParent, jnodeParent, baseIRI, thePrefixes);
    }

    static private void reconstructTypes(
                            ResourceNode rnodeParent, JsonNode jnodeParent, final ParsedIRI baseIRI,
                            VocabularyList thePrefixes) {
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

    private String strPathIRI;
	private String strPrefix;

    public RDFType(String strPrefix, String strPathIRI) {
        this.strPrefix  = Util.toSpaceStrippedString(strPrefix);
        this.strPathIRI = Util.toSpaceStrippedString(strPathIRI);
    }

    public String getPrefix() {
    	return this.strPrefix;
    }

    public String getPathIRI() {
		return this.strPathIRI;
	}

    public String getPrefixedIRI() {
        if (this.strPrefix != null) {
		    return this.strPrefix + ":" + this.strPathIRI;
        }
        return this.strPathIRI;
	}

    public String getFullIRI(VocabularyList thePrefixes) {
        if (this.strPrefix != null) {
            Vocabulary vocab = thePrefixes.findByPrefix(this.strPrefix);
            if (vocab != null) {
                return vocab.getNamespace() + this.strPathIRI;
            }
            return this.strPrefix + ":" + this.strPathIRI;
        }
        return this.strPathIRI;
	}

    public void write(JsonGenerator writer)
            throws JsonGenerationException, IOException {
        writer.writeStartObject();

        writer.writeStringField(Util.gstrPrefix, this.strPrefix);

        writer.writeObjectFieldStart(Util.gstrValueSource);
        writer.writeStringField(Util.gstrSource, Util.gstrConstant);
        writer.writeStringField(Util.gstrConstant, this.strPathIRI);
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