/*
 *  Class SearchResultItem
 *
 *  The Search Result Item class used to manage vocabulary searches in the
 *  Lucene Indexer.
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

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import org.openrefine.rdf.model.Util;

public class SearchResultItem {
    private String strIRI = null;
    private String strLabel = null;
    private String strDesc = null;
    private String strPrefix = null;
    private String strNamespace = null;
    private String strLocalPart = null;

    public SearchResultItem(
                String strIRI, String strLabel, String strDesc,
                String strPrefix, String strNamespace, String strLocalPart) {
        this.strIRI       = strIRI;
        this.strLabel     = strLabel;
        this.strDesc      = strDesc;
        this.strPrefix    = strPrefix;
        this.strNamespace = strNamespace;
        this.strLocalPart = strLocalPart;
    }

    public boolean isSameIRI(SearchResultItem item) {
        return this.strIRI.equals( item.getIRI() );
    }

    public String getIRI() {
        return this.strIRI;
    }

    public String getLabel() {
        return this.strLabel;
    }

    public String getDescription() {
        return this.strDesc;
    }

    public String getPrefix() {
        return this.strPrefix;
    }

    public String getNamespace() {
        return this.strNamespace;
    }

    public String getLocalPart() {
        return this.strLocalPart;
    }

    public void writeAsSearchResult(JsonGenerator theWriter)
            throws IOException {
        theWriter.writeStartObject();
        if (this.strIRI != null) {
            theWriter.writeStringField(Util.gstrIRI, this.strIRI);
        }
        if (this.strLabel != null) {
            theWriter.writeStringField(Util.gstrLabel, this.strLabel);
        }
        if (this.strDesc != null) {
            theWriter.writeStringField(Util.gstrDesc, this.strDesc);
        }
        if (this.strPrefix != null) {
            theWriter.writeStringField(Util.gstrPrefix, this.strPrefix);
        }
        if (this.strNamespace != null) {
            theWriter.writeStringField(Util.gstrNamespace, this.strNamespace);
        }
        if (this.strLocalPart != null) {
            theWriter.writeStringField(Util.gstrLocalPart, this.strLocalPart);
        }
        // The long "description" contains everything:
        //      the full IRI,
        //      the Label,
        //      the stored Description,
        //      the Prefix,
        //      the Namespace, and
        //      the Local Part
        // for searchTerm display purposes.
        String strDescription =
            ((this.strIRI != null) ?
                ("<em>IRI</em>: " + this.strIRI) : "") +
            ((this.strLabel != null) ?
                ("<br/>" + "<em>Label</em>: " + this.strLabel) : "") +
            ((this.strDesc != null) ?
                ("<br/>" + "<em>Description</em>: " + this.strDesc) : "") +
            ((this.strPrefix != null) ?
                ("<br/>" + "<em>Prefix</em>: " + this.strPrefix) : "") +
            ((this.strNamespace != null) ?
                ("<br/>" + "<em>Namespace</em>: " + this.strNamespace) : "") +
            ((this.strLocalPart != null) ?
                ("<br/>" + "<em>LocalPart</em>: " + this.strLocalPart) : "");
        theWriter.writeStringField(Util.gstrDescription, strDescription);
        theWriter.writeEndObject();
    }

}
