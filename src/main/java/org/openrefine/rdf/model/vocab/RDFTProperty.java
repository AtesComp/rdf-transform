/*
 *  Class RDFTProperty
 *
 *  The RDF Transform Property class used to manage RDF properties in
 *  vocabularies.
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

public class RDFTProperty extends RDFTNode{

    public RDFTProperty(String[] astrLoader) {
        super(astrLoader);
    }

    public RDFTProperty(String strIRI) {
        super(strIRI);
    }

    @Override
    public String getType() {
        //return RDFTProperty.class.getSimpleName();
        return "property";
    }
}
