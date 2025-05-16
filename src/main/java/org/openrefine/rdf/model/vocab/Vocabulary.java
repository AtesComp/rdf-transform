/*
 *  Class Vocabulary
 *
 *  The Vocabulary class used to manage vocabularies for an RDF TRansform.
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

    static public enum LocationType { NONE, URL, FILE, RDF_XML, TTL, N3, NTRIPLE, JSON_LD, NQUADS, RDF_JSON, TRIG, TRIX, RDFTHRIFT };    // The set of Location Types

    private String strPrefix;       // Short name that represents the Namespace
    private String strNamespace;    // The fully qualified Namespace
    private String strLocation;     // The location for loading the vocabulary
    private LocationType loctype;   // The location type for the vocabulary

    static public LocationType fromLocTypeString(String strVal) {
        if ( strVal == LocationType.NONE.toString() )       return LocationType.NONE;
        if ( strVal == LocationType.URL.toString() )        return LocationType.URL;
        if ( strVal == LocationType.FILE.toString() )       return LocationType.FILE;
        if ( strVal == "RDF/XML" ||
             strVal == LocationType.RDF_XML.toString() )    return LocationType.RDF_XML;
        if ( strVal == LocationType.TTL.toString() )        return LocationType.TTL;
        if ( strVal == LocationType.N3.toString() )         return LocationType.N3;
        if ( strVal == LocationType.NTRIPLE.toString() )    return LocationType.NTRIPLE;
        if ( strVal == "JSON-LD" ||
             strVal == LocationType.JSON_LD.toString() )    return LocationType.JSON_LD;
        if ( strVal == LocationType.NQUADS.toString() )     return LocationType.NQUADS;
        if ( strVal == "RDF/JSON" ||
             strVal == LocationType.RDF_JSON.toString() )   return LocationType.RDF_JSON;
        if ( strVal == LocationType.TRIG.toString() )       return LocationType.TRIG;
        if ( strVal == LocationType.TRIX.toString() )       return LocationType.TRIX;
        if ( strVal == LocationType.RDFTHRIFT.toString() )  return LocationType.RDFTHRIFT;
        return null;
    }

    public Vocabulary(String strPrefix, String strNamespace)
    {
        this.strPrefix = strPrefix;
        this.strNamespace = strNamespace;
        this.strLocation = strNamespace;
        this.loctype = LocationType.URL;
        if (strPrefix == "xsd") {
            this.strLocation = "";
            this.loctype = LocationType.NONE;
        }
        if ( Util.isDebugMode() ) {
            Vocabulary.logger.info(
                "DEBUG: Prefix:[{}] Namespace:[{}] Location:[{}] LocType:[{}]",
                strPrefix, strNamespace, strLocation, loctype.toString()
            );
        }
    }

    public Vocabulary(String strPrefix, String strNamespace, String strLocation, LocationType loctype)
    {
        this.strPrefix = strPrefix;
        this.strNamespace = strNamespace;
        this.strLocation = strLocation;
        this.loctype = loctype;
        if ( Util.isDebugMode() ) {
            Vocabulary.logger.info(
                "DEBUG: Prefix:[{}] Namespace:[{}] Location:[{}] LocType:[{}]",
                strPrefix, strNamespace, strLocation, loctype.toString()
            );
        }
    }

    public String getPrefix() {
        return this.strPrefix;
    }

    public String getNamespace() {
        return this.strNamespace;
    }

    public String getLocation() {
        return this.strLocation;
    }

    public LocationType getLocationType() {
        return this.loctype;
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
        //theWriter.writeStringField(this.strPrefix, this.strNamespace);
        theWriter.writeObjectFieldStart(this.strPrefix);
        theWriter.writeStringField("namespace", this.strNamespace);
        theWriter.writeStringField("location", (this.strLocation == null) ? "" : this.strLocation);
        theWriter.writeStringField("loctype",  (this.strLocation == null) ? LocationType.NONE.toString() : this.loctype.toString() );
        theWriter.writeEndObject(); // ...this.strPrefix }
    }
}
