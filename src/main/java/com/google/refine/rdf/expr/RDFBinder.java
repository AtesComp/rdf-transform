package com.google.refine.rdf.expr;

import java.io.IOException;
import java.util.Properties;

import com.google.refine.rdf.app.ApplicationContext;
import com.google.refine.rdf.vocab.VocabularyIndexException;
import com.google.refine.rdf.RDFTransform;
import com.google.refine.rdf.Util;

import com.google.refine.expr.Binder;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RDFBinder implements Binder {
	private final static Logger logger = LoggerFactory.getLogger("RDFT:RDFBinder");

	private ApplicationContext context;

	public RDFBinder(ApplicationContext context) {
		super();
		this.context = context;
	}

    @Override
    public void initializeBindings(Properties bindings, Project theProject) {
		if ( Util.isVerbose(3) ) logger.info("Bind baseIRI...");
		String strBindError = "Unable to bind baseIRI.";
        try {
			bindings.put( "baseIRI", RDFTransform.getRDFTransform(context, theProject).getBaseIRIAsString() );
		}
		catch (VocabularyIndexException ex) {
			logger.error(strBindError, ex);
			if ( Util.isVerbose() ) ex.printStackTrace();
		}
		catch (IOException ex) {
			logger.error(strBindError, ex);
			if ( Util.isVerbose() ) ex.printStackTrace();
		}
    }

	@Override
	public void bind(Properties bindings, Row row, int rowIndex, String columnName, Cell cell) {
		// nothing to do
	}
}
