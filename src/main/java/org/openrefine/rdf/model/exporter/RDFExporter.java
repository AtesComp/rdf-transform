/*
 *  Class RDFExporter
 * 
 *  The RDF Exporter base class used by other RDF exporters.
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

package org.openrefine.rdf.model.exporter;

import com.google.refine.exporters.Exporter;

import org.openrefine.rdf.model.Util;

import org.apache.jena.riot.RDFFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RDFExporter implements Exporter  {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:RDFExporter");

    protected RDFFormat format;
    protected String strName;

    public RDFExporter(RDFFormat format, String strName) {
        this.format = format;
        this.strName = strName;
        if ( Util.isDebugMode() ) RDFExporter.logger.info("DEBUG: Preparing exporter " + strName + "...");
    }

    public String getContentType() {
        if (this.format != null) {
            return this.format.getLang().getContentType().getContentTypeStr();
        }
        else { // ...export as Turtle...
            return RDFFormat.TURTLE_PRETTY.getLang().getContentType().getContentTypeStr();
        }
    }
}
