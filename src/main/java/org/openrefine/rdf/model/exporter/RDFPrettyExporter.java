package org.openrefine.rdf.model.exporter;

import java.io.IOException;
import java.io.Writer;
import java.util.Properties;

import org.openrefine.rdf.model.Util;

import com.google.refine.browsing.Engine;
import com.google.refine.exporters.WriterExporter;
import com.google.refine.model.Project;

import org.apache.jena.riot.RDFFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Class RDFPrettyExporter
 *
 *  An exporter used to transform OpenRefine project data to RDF *in the prettiest format possible*
 *  meaning the data is condensed and formatted for readability.  This requires that the entire graph
 *  is accessible by the print process to organize repeated resources and literals and, therefore,
 *  cannot process discreet data chunks--i.e., it does not scale.  Furthermore, the entire graph must
 *  currently fit in the available memory as the graph used to dump the project data is a memory
 *  graph.
 *
 *  Use with relatively small graphs (compared to available memory) as the process could result in a
 *  critical failure.
 */
public class RDFPrettyExporter extends RDFExporter implements WriterExporter {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:RDFPrettyExporter");

    public RDFPrettyExporter(RDFFormat format, String strName) {
        super(format, strName);
    }

    public void export(Project theProject, Properties options, Engine theEngine,
                        Writer theWriter)
             throws IOException
    {
        if ( Util.isDebugMode() ) RDFPrettyExporter.logger.info("DEBUG: Exporting " + this.strName + " via Writer");

        // TODO: Finish coding graph model export.

        this.export(theProject, options, theEngine);
    }

    private void export(Project theProject, Properties options, Engine theEngine)
             throws IOException
    {
        // TODO: Finish coding graph model export.
    }
}
