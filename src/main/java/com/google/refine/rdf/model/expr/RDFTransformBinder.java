package com.google.refine.rdf.model.expr;

import java.io.IOException;
import java.util.Properties;

import com.google.refine.rdf.model.Util;
import com.google.refine.rdf.ApplicationContext;
import com.google.refine.rdf.RDFTransform;
import com.google.refine.expr.Binder;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Class RDFBinder
 * 
 *   This class is registered by the "controller.js" in this extension.
 *   The purpose of registering this "binder" is to push an instance of this class onto the
 *   HashSet managed by the ExpressionUtils class.
 * 
 *   This "binder" is used by the ExpressionUtils createBindings() method to create and add
 *   generic "bindings" properties.  It calls this "binder"'s initializeBindings() method to
 *   add a "baseIRI" binding to the "bindings" properties.
 * 
 *   The ExpressionUtils bind() method is used to bind a specific row (Row), row index (int),
 *   column name (String), and cell (Cell) to the "bindings".  It calls this "binder"'s bind()
 *   method to perform any additional work concerning the added "baseIRI" binding.
 */
public class RDFTransformBinder implements Binder {
	private final static Logger logger = LoggerFactory.getLogger("RDFT:RDFBinder");

	private ApplicationContext theContext;
	private Project theProject;
	private String strLastBoundBaseIRI;
	private final String strBindError = "Unable to bind baseIRI.";

	public RDFTransformBinder(ApplicationContext context) {
		super();
		this.theContext = context;
		this.strLastBoundBaseIRI = null;
	}

    @Override
    public void initializeBindings(Properties bindings, Project project) {
		this.theProject = project;
		if ( Util.isVerbose(3) ) RDFTransformBinder.logger.info("Bind baseIRI...");
        try {
			this.strLastBoundBaseIRI =
				RDFTransform.getRDFTransform(this.theContext, this.theProject).getBaseIRIAsString();
		}
		catch (IOException ex) {
			RDFTransformBinder.logger.error(this.strBindError, ex);
			if ( Util.isVerbose() || Util.isDebugMode() ) ex.printStackTrace();
			return;
		}
		bindings.put("baseIRI", this.strLastBoundBaseIRI);
    }

	@Override
	public void bind(Properties bindings, Row row, int rowIndex, String columnName, Cell cell) {
		//
		// Update the baseIRI
		//
		// The baseIRI is already added by the initializeBindings() above.
		// The put() call replaces it.
	
		// Get the current baseIRI...
		String strCurrentBaseIRI = null;
        try {
			strCurrentBaseIRI =
				RDFTransform.getRDFTransform(this.theContext, this.theProject).getBaseIRIAsString();
		}
		catch (IOException ex) {
			RDFTransformBinder.logger.error(strBindError, ex);
			if ( Util.isVerbose() || Util.isDebugMode() ) ex.printStackTrace();
			return;
		}
		// If the current baseIRI is new...
		if ( ! this.strLastBoundBaseIRI.equals(strCurrentBaseIRI) ) {
			// Replace the bound baseIRI...
			bindings.put("baseIRI", strCurrentBaseIRI);
			strLastBoundBaseIRI = strCurrentBaseIRI;
		}
	}
}
