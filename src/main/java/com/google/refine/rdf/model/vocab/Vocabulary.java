package com.google.refine.rdf.model.vocab;

//import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
//import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.refine.rdf.model.Util;
import com.fasterxml.jackson.core.JsonGenerationException;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonIgnoreType
public class Vocabulary {
    static private final Logger logger = LoggerFactory.getLogger("RDFT:Vocabulary");

    private String strPrefix;       // Short name that represents the Namespace
    private String strNamespace;    // The fully qualified Namespace

    public Vocabulary(String strPrefix, String strNamespace )
    {
        this.strPrefix = strPrefix;
        this.strNamespace = strNamespace;
        if ( Util.isDebugMode() ) Vocabulary.logger.info("DEBUG: Prefix:[{}] Namespace:[{}]", strPrefix, strNamespace);
    }

    public String getPrefix() {
        return this.strPrefix;
    }

    public String getNamespace() {
        return this.strNamespace;
    }

    @Override
    public int hashCode() {
        return this.strPrefix.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if ( object != null && object.getClass().equals( this.getClass() ) ) {
            return this.strPrefix.equals( ( (Vocabulary) object).getPrefix());
        }
        return false;
    }

    public void write(JsonGenerator theWriter)
            throws JsonGenerationException, IOException {
        theWriter.writeStringField(this.strPrefix, this.strNamespace);
    }
}
