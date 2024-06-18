/*
 *  Class Vocabulary
 *
 *  The Vocabulary class used to manage vocabularies for an RDF TRansform.
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

package org.openrefine.rdf.model.vocab;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.core.JsonGenerator;
import org.openrefine.rdf.model.Util;
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
