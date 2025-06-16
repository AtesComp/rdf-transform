/*
 *  Class RDFExporter
 *
 *  The RDF Exporter base class used by other RDF exporters.
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

package org.openrefine.rdf.model.exporter;

import com.google.refine.exporters.Exporter;

import org.openrefine.rdf.model.Util;

import org.apache.jena.riot.RDFFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RDFExporter implements Exporter  {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:RDFExporter");

    /**
     * The member, theFormat, is the proposed RDFFormat used for output. It can be null.
     * Use method getFormat() to either get the given RDFFormat or, if null, rectify to a default format.
     */
    private RDFFormat theFormat;
    protected String theExportLang;

    public RDFExporter(RDFFormat format, String strLang) {
        this.theFormat = format;
        this.theExportLang = strLang;
        if ( Util.isDebugMode() ) RDFExporter.logger.info("DEBUG: Preparing exporter " + strLang + "...");
    }

    public RDFFormat getFormat() {
        if (this.theFormat != null) {
            return this.theFormat;
        }
        else { // ...export as TriG...
            return RDFFormat.TRIG;
        }
    }

    public String getContentType() {
        if (this.theFormat != null) {
            return this.theFormat.getLang().getContentType().getContentTypeStr();
        }
        else { // ...export as TriG...
            return RDFFormat.TRIG.getLang().getContentType().getContentTypeStr();
        }
    }
}
