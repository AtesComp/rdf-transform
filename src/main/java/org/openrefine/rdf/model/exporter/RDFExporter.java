package org.openrefine.rdf.model.exporter;

import org.openrefine.rdf.model.Util;

import org.apache.jena.riot.RDFFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RDFExporter {
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
            return RDFFormat.TURTLE.getLang().getContentType().getContentTypeStr();
        }
    }
}
