package com.google.refine.rdf.exporter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Properties;

import com.google.refine.rdf.RDFTransform;
import com.google.refine.rdf.app.ApplicationContext;
import com.google.refine.rdf.operation.ExportRDFRowVisitor;
import com.google.refine.rdf.operation.RDFRowVisitor;
import com.google.refine.rdf.Util;
import com.google.refine.rdf.vocab.Vocabulary;
import com.google.refine.rdf.vocab.VocabularyIndexException;

import com.google.refine.browsing.Engine;
import com.google.refine.exporters.WriterExporter;
import com.google.refine.model.Project;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class RDFExporter implements WriterExporter {
    //private final static Logger logger = LoggerFactory.getLogger("RDFT:RDFExporter");

    private RDFFormat format;
    private ApplicationContext applicationContext;

	public RDFExporter(ApplicationContext context, RDFFormat format){
        this.format = format;
        this.applicationContext = context;
    }

	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}

    public void export(Project project, Properties options, Engine engine,
            OutputStream outputStream) throws IOException {
	    export(project, options, engine, Rio.createWriter(this.format, outputStream));
    }

	public void export(Project project, Properties options, Engine engine,
					   Writer writer) throws IOException {
		export(project, options, engine, Rio.createWriter(this.format, writer));
	}

	private void export(Project project, Properties options, Engine engine,
						RDFWriter writer) throws IOException {
    	RDFTransform transformer;
    	try {
    		transformer = RDFTransform.getRDFTransform(applicationContext, project);
    	}
		catch (VocabularyIndexException ex) {
    		throw new IOException("Unable to retrieve RDF Transform", ex);
    	}
        try {
			writer.startRDF();

			for (Vocabulary v : transformer.getPrefixesMap().values() ) {
				writer.handleNamespace( v.getPrefix(), v.getNamespace() );
			}
			exportModel(project, engine, transformer, writer);

			writer.endRDF();
		}
		catch (RDFHandlerException ex) {
			throw new RuntimeException(ex);
		}
	}

	public Repository exportModel(final Project project, Engine engine, RDFTransform transformer, RDFWriter writer)
			throws IOException{
				RDFRowVisitor visitor = new ExportRDFRowVisitor(transformer, writer);
		return Util.buildModel(project, engine, visitor);
    }

    public String getContentType() {
        if (this.format.equals(RDFFormat.RDFXML)) {
            return "application/rdf+xml";
        }
		else if (this.format.equals(RDFFormat.NTRIPLES)) {
            return "application/n-triples";
        }
        else if (this.format.equals(RDFFormat.TURTLE)) {
            return "text/turtle";
        }
        else if (this.format.equals(RDFFormat.TURTLESTAR)) {
            return "text/x-turtlestar";
        }
        else if (this.format.equals(RDFFormat.N3)) {
            return "text/n3";
        }
        else if (this.format.equals(RDFFormat.TRIX)) {
            return "application/trix";
        }
        else if (this.format.equals(RDFFormat.TRIG)) {
            return "application/trig";
        }
        else if (this.format.equals(RDFFormat.TRIGSTAR)) {
            return "application/x-trigstar";
        }
        else if (this.format.equals(RDFFormat.BINARY)) {
            return "application/x-binary-rdf";
        }
        else if (this.format.equals(RDFFormat.NQUADS)) {
            return "application/n-quads";
        }
        else if (this.format.equals(RDFFormat.JSONLD)) {
            return "application/ld+json";
        }
        else if (this.format.equals(RDFFormat.NDJSONLD)) {
            return "application/x-ld+ndjson";
        }
        else if (this.format.equals(RDFFormat.RDFJSON)) {
            return "application/rdf+json";
        }
        else if (this.format.equals(RDFFormat.RDFA)) {
            return "application/xhtml+xml";
        }
        else if (this.format.equals(RDFFormat.HDT)) {
            return "application/vnd.hdt";
        }
		else { // ...export as Turtle when unknown...
            return "text/turtle";
        }
    }

    public boolean takeWriter() {
        return true;
    }
}
