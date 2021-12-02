package com.google.refine.rdf.exporter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Properties;

import com.google.refine.rdf.RDFTransform;
import com.google.refine.rdf.app.ApplicationContext;
import com.google.refine.rdf.operation.ExportRDFRecordVisitor;
import com.google.refine.rdf.operation.ExportRDFRowVisitor;
import com.google.refine.rdf.operation.RDFVisitor;
import com.google.refine.rdf.vocab.Vocabulary;
import com.google.refine.rdf.vocab.VocabularyIndexException;

import com.google.refine.browsing.Engine;
import com.google.refine.exporters.WriterExporter;
import com.google.refine.model.Project;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class RDFExporter implements WriterExporter {
    //private final static Logger logger = LoggerFactory.getLogger("RDFT:RDFExporter");

    private RDFFormat format;
    private ApplicationContext context;

	public RDFExporter(ApplicationContext context, RDFFormat format) {
        this.format = format;
        this.context = context;
    }

	public ApplicationContext getApplicationContext() {
		return context;
	}

    public void export(Project theProject, Properties options, Engine theEngine,
                        OutputStream outputStream)
            throws IOException {
	    export(theProject, options, theEngine, Rio.createWriter(this.format, outputStream));
    }

	public void export(Project theProject, Properties options, Engine theEngine,
					    Writer theWriter)
             throws IOException {
		export(theProject, options, theEngine, Rio.createWriter(this.format, theWriter));
	}

	private void export(Project theProject, Properties options, Engine theEngine,
					    RDFWriter theWriter)
            throws IOException {
    	RDFTransform theTransform;
    	try {
    		theTransform = RDFTransform.getRDFTransform(context, theProject);
    	}
		catch (VocabularyIndexException ex) {
    		throw new IOException("Unable to retrieve RDF Transform", ex);
    	}
        try {
            // Start writing...
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

    public boolean takeWriter() {
        return true;
    }
}
