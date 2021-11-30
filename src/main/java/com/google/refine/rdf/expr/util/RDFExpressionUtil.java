package com.google.refine.rdf.expr.util;

import java.util.Properties;

//import com.google.refine.expr.EvalError;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.model.Cell;
import com.google.refine.model.Row;

public class RDFExpressionUtil {

	public static Object evaluate(Evaluable eval, Properties bindings,
									Row theRow, int iRowIndex,
									String strColumnName, int iCellIndex) {
		Cell cell = null;
		// If there is a valid cell index for a column...
		if (iCellIndex >= 0) {
			cell = theRow.getCell(iCellIndex);
		}
		// Otherwise, create a pseudo-cell for the "row index column"...
		else {
         	cell = new Cell(iRowIndex, null);
        }

		// Bind the cell for expression evaluation...
        ExpressionUtils.bind(bindings, theRow, iRowIndex, strColumnName, cell);

		// Evaluate the expression on the cell for results...
		return eval.evaluate(bindings);
	}
}
