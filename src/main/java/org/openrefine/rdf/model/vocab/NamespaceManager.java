/*
 *  Class NamespaceManager
 *
 *  A Namespace Manager class used to manage RDF Transform namespaces.
 *
 *  Copyright 2022 Keven L. Ates
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.openrefine.rdf.model.Util;

import com.fasterxml.jackson.annotation.JsonIgnoreType;

@JsonIgnoreType
public class NamespaceManager {
    private Map<String, String> prefixMap = new HashMap<String, String>();

    public NamespaceManager(InputStream inStream) throws IOException {
        BufferedReader buffReader = new BufferedReader( new InputStreamReader(inStream) );
        String strLine, strPrefix = null, strNamespace = null;

        //  Read Prefix file lines...
        //      There should be 2 entries per line:
        //          Prefix, Namespace
        //      Each entry should be separated by whitespace.
        String[] astrTokens;
        while ( (strLine = buffReader.readLine() ) != null) {
            // Parse entries...
            astrTokens = Util.replaceAllWhitespace(strLine).split("\\s+");

            // Are there enough entries?
            if (astrTokens.length < 2)
                continue;

            // Organize entries...
            strPrefix    = astrTokens[0];
            strNamespace = astrTokens[1];

            // Store prefix map...
            prefixMap.put(strPrefix, strNamespace);
        }
    }

    public String getNamespace(String strPrefix) {
        return prefixMap.get(strPrefix);
    }
}
