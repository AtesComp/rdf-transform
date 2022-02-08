package com.google.refine.rdf.model.exporter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Properties;

import com.google.refine.rdf.ApplicationContext;
import com.google.refine.rdf.RDFTransform;
import com.google.refine.rdf.model.operation.ExportRDFRecordVisitor;
import com.google.refine.rdf.model.operation.ExportRDFRowVisitor;
import com.google.refine.rdf.model.operation.RDFVisitor;
import com.google.refine.browsing.Engine;
import com.google.refine.exporters.StreamExporter;
import com.google.refine.exporters.WriterExporter;
import com.google.refine.model.Project;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class RDFExporter implements WriterExporter, StreamExporter {
    //private final static Logger logger = LoggerFactory.getLogger("RDFT:RDFExporter");

    private RDFFormat format;
    private ApplicationContext context;

	public RDFExporter(RDFFormat format) {
        this.format = format;
    }

	public ApplicationContext getApplicationContext() {
		return context;
	}

    public void export(Project theProject, Properties options, Engine theEngine,
                        OutputStream outputStream)
            throws IOException {
	    this.export(theProject, options, theEngine, Rio.createWriter(this.format, outputStream));
    }

	public void export(Project theProject, Properties options, Engine theEngine,
					    Writer theWriter)
             throws IOException {
        this.export(theProject, options, theEngine, Rio.createWriter(this.format, theWriter));
	}

	private void export(Project theProject, Properties options, Engine theEngine,
					    RDFWriter theWriter)
            throws IOException {
    	RDFTransform theTransform;
    	theTransform = RDFTransform.getRDFTransform(theProject);
        try {
			theWriter.startRDF();

            // Process all records/rows of data for statements...
	        RDFVisitor theVisitor = null;
            if ( theProject.recordModel.hasRecords() ) {
                theVisitor = new ExportRDFRecordVisitor(theTransform, theWriter);
            }
            else {
                theVisitor = new ExportRDFRowVisitor(theTransform, theWriter);
            }
            theVisitor.buildModel(theProject, theEngine);

			theWriter.endRDF();
		}
		catch (RDFHandlerException ex) {
			throw new RuntimeException(ex);
		}
	}

    public String getContentType() {
        if (this.format != null) {
            return this.format.getDefaultMIMEType();
        }
		else { // ...export as Turtle...
            return RDFFormat.TURTLE.getDefaultMIMEType();
        }
    }
}
