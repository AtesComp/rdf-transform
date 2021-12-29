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
 *   The ExpressionUtils bind() method is used to bind a specific Row, row index, row Cells,
 *   column name, Cell, and cell value to the "bindings".  It calls this "binder"'s bind()
 *   method to perform any additional work concerning the added "baseIRI" binding.
 */
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
		catch (IOException ex) {
			logger.error(strBindError, ex);
			if ( Util.isVerbose() ) ex.printStackTrace();
		}
    }

	@Override
	public void bind(Properties bindings, Row row, int rowIndex, String columnName, Cell cell) {
		// The baseIRI is already added by the initializeBindings() above.
		// ...nothing more to do...
	}
}
