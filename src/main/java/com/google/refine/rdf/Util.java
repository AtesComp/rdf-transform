package com.google.refine.rdf;

import com.google.refine.rdf.app.ApplicationContext;
import com.google.refine.rdf.expr.util.RDFExpressionUtil;
import com.google.refine.rdf.operation.RDFRecordVisitor;
import com.google.refine.rdf.operation.RDFRowVisitor;
import com.google.refine.rdf.vocab.VocabularyIndexException;

import com.google.refine.browsing.FilteredRecords;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.Engine;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.preference.PreferenceStore;
import com.google.refine.ProjectManager;

import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.repository.Repository;
//import java.net.URISyntaxException;
//import java.net.URLEncoder;

import java.io.IOException;
//import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.List;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * CLASS Util
 * 
 * 	The Util class is a convenience class holding common values and functions used by others.
 */
public class Util {

	private final static String XSD_DUR_IRI      = "http://www.w3.org/2001/XMLSchema#duration";
	private final static String XSD_DT_IRI       = "http://www.w3.org/2001/XMLSchema#dateTime";
	private final static String XSD_TIME_IRI     = "http://www.w3.org/2001/XMLSchema#time";
	private final static String XSD_DATE_IRI     = "http://www.w3.org/2001/XMLSchema#date";
	private final static String XSD_GYM_IRI      = "http://www.w3.org/2001/XMLSchema#gYearMonth";
	private final static String XSD_GYEAR_IRI    = "http://www.w3.org/2001/XMLSchema#gYear";
	private final static String XSD_GMD_IRI      = "http://www.w3.org/2001/XMLSchema#gMonthday";
	private final static String XSD_GDAY_IRI     = "http://www.w3.org/2001/XMLSchema#gDay";
	private final static String XSD_GMONTH_IRI   = "http://www.w3.org/2001/XMLSchema#gMonth";
	private final static String XSD_BOOL_IRI     = "http://www.w3.org/2001/XMLSchema#boolean";
	private final static String XSD_B64BIN_IRI   = "http://www.w3.org/2001/XMLSchema#base64Binary";
	private final static String XSD_XBIN_IRI     = "http://www.w3.org/2001/XMLSchema#hexBinary";
	private final static String XSD_FLOAT_IRI    = "http://www.w3.org/2001/XMLSchema#float";
	private final static String XSD_DOUBLE_IRI   = "http://www.w3.org/2001/XMLSchema#double";
	private final static String XSD_ANY_IRI      = "http://www.w3.org/2001/XMLSchema#anyURI";
	private final static String XSD_QNAME_IRI    = "http://www.w3.org/2001/XMLSchema#QName";
	private final static String XSD_NOTATION_IRI = "http://www.w3.org/2001/XMLSchema#NOTATION";

	private final static String XSD_STR_IRI      = "http://www.w3.org/2001/XMLSchema#string";
	private final static String XSD_NSTR_IRI     = "http://www.w3.org/2001/XMLSchema#normalizedString";
	private final static String XSD_TOKEN_IRI    = "http://www.w3.org/2001/XMLSchema#token";
	private final static String XSD_LANG_IRI     = "http://www.w3.org/2001/XMLSchema#language";
	private final static String XSD_NAME_IRI     = "http://www.w3.org/2001/XMLSchema#Name";
	private final static String XSD_NCNAME_IRI   = "http://www.w3.org/2001/XMLSchema#NCName";
	private final static String XSD_ID_IRI       = "http://www.w3.org/2001/XMLSchema#ID";
	private final static String XSD_IDREF_IRI    = "http://www.w3.org/2001/XMLSchema#IDREF";
	private final static String XSD_IDREFS_IRI   = "http://www.w3.org/2001/XMLSchema#IDREFS";
	private final static String XSD_ENITY_IRI    = "http://www.w3.org/2001/XMLSchema#ENTITY";
	private final static String XSD_ENITIES_IRI  = "http://www.w3.org/2001/XMLSchema#ENTITIES";
	private final static String XSD_NMTOKEN_IRI  = "http://www.w3.org/2001/XMLSchema#NMTOKEN";
	private final static String XSD_NMTOKENS_IRI = "http://www.w3.org/2001/XMLSchema#NMTOKENS";

	private final static String XSD_DEC_IRI      = "http://www.w3.org/2001/XMLSchema#decimal";
	private final static String XSD_INTEGER_IRI  = "http://www.w3.org/2001/XMLSchema#integer";
	private final static String XSD_NPINT_IRI    = "http://www.w3.org/2001/XMLSchema#nonPositiveInteger";
	private final static String XSD_NINT_IRI     = "http://www.w3.org/2001/XMLSchema#negativeInteger";
	private final static String XSD_LONG_IRI     = "http://www.w3.org/2001/XMLSchema#long";
	private final static String XSD_INT_IRI      = "http://www.w3.org/2001/XMLSchema#int";
	private final static String XSD_SHORT_IRI    = "http://www.w3.org/2001/XMLSchema#short";
	private final static String XSD_BYTE_IRI     = "http://www.w3.org/2001/XMLSchema#byte";
	private final static String XSD_NNINT_IRI    = "http://www.w3.org/2001/XMLSchema#nonNegativeInteger";
	private final static String XSD_ULONG_IRI    = "http://www.w3.org/2001/XMLSchema#unsignedLong";
	private final static String XSD_UINT_IRI     = "http://www.w3.org/2001/XMLSchema#unsignedInt";
	private final static String XSD_USHORT_IRI   = "http://www.w3.org/2001/XMLSchema#unsignedShort";
	private final static String XSD_UBYTE_IRI    = "http://www.w3.org/2001/XMLSchema#unsignedByte";
	private final static String XSD_PINT_IRI     = "http://www.w3.org/2001/XMLSchema#positiveInteger";

	private final static List<String> XML_SCHEMA_DEFS = Arrays.asList(
		XSD_DUR_IRI, XSD_DT_IRI, XSD_TIME_IRI, XSD_DATE_IRI,
		XSD_GYM_IRI, XSD_GYEAR_IRI, XSD_GMD_IRI, XSD_GDAY_IRI, XSD_GMONTH_IRI,
		XSD_BOOL_IRI, XSD_B64BIN_IRI, XSD_XBIN_IRI, XSD_FLOAT_IRI,
		XSD_DOUBLE_IRI, XSD_ANY_IRI, XSD_QNAME_IRI, XSD_NOTATION_IRI,

		XSD_STR_IRI, XSD_NSTR_IRI, XSD_TOKEN_IRI, XSD_LANG_IRI, XSD_NAME_IRI,
		XSD_NCNAME_IRI, XSD_ID_IRI, XSD_IDREF_IRI, XSD_IDREFS_IRI,
		XSD_ENITY_IRI, XSD_ENITIES_IRI, XSD_NMTOKEN_IRI, XSD_NMTOKENS_IRI,

		XSD_DEC_IRI, XSD_INTEGER_IRI, XSD_NPINT_IRI, XSD_NINT_IRI,
		XSD_LONG_IRI, XSD_INT_IRI, XSD_SHORT_IRI, XSD_BYTE_IRI,
		XSD_NNINT_IRI, XSD_ULONG_IRI, XSD_UINT_IRI, XSD_USHORT_IRI, XSD_UBYTE_IRI,
		XSD_PINT_IRI
	);

	private final static Logger logger = LoggerFactory.getLogger("RDFT:Util" );

	private static int iVerbosity = 0;

//
// PCRE IRI Resolution -----
//
//   See https://stackoverflow.com/questions/161738/what-is-the-best-regular-expression-to-check-if-a-string-is-a-valid-url

	public static class IRIParsingException extends Exception { 
		public IRIParsingException(String errorMessage) {
			super(errorMessage);
		}
	}

	public static String resolveIRI(ParsedIRI baseIRI, String strIRI) throws IRIParsingException {
		String strError = "ERROR: resolveIRI(): ";
		String strErrMsg = null;
		String strAbsoluteIRI = null;

		// No IRI is not a problem (there is just nothing to resolve)...
		if (strIRI == null || strIRI.length() == 0) {
			if ( Util.isVerbose(4) )
				logger.info("resolveIRI: No IRI");
			return strAbsoluteIRI;
		}

		// Create Absolute IRI without Base IRI...
		try {
			ParsedIRI absoluteIRI = new ParsedIRI(strIRI);
			if ( absoluteIRI.isAbsolute() ) {
			    strAbsoluteIRI = absoluteIRI.toString();
			}
		}
		catch (Exception ex) {
			strErrMsg = ex.getMessage();
			// ...continue in case we can resolve as a Relative IRI...
		}

		// Not an Absolute IRI?
		if (strAbsoluteIRI == null && baseIRI != null) {
			// Create Absolute IRI from Relative IRI using Base IRI...
			try {
				strAbsoluteIRI = baseIRI.resolve(strIRI);
			}
			catch (Exception ex) {
				strErrMsg = ex.getMessage();
				// ...continue in case it needs a little adjusting...
			}

			// Create Absolute IRI from adjusted Relative IRI using Base IRI...
			if (strAbsoluteIRI == null)
			{
				try {
					if ( ! strIRI.startsWith("/") ) {
						strAbsoluteIRI = baseIRI.resolve("/" + strIRI);
					}
				}
				catch (Exception ex) {
					strErrMsg = ex.getMessage();
				}
			}
		}

		// DEBUG: Check IRI...
		if ( Util.isVerbose(4) ) {
			String strDEBUG = "DEBUG resolveIRI: ";
			if (strAbsoluteIRI == null)
				strDEBUG += "NULL";
			else
				strDEBUG += "[" + strAbsoluteIRI + "]";
			logger.info(strDEBUG);
		}

		if (strAbsoluteIRI == null && strErrMsg != null) {
			if ( Util.isVerbose() ) {
				logger.error(strError + "Malformed IRI [" + strIRI + "]");
				logger.error(strError + strErrMsg);
			}
			throw new Util.IRIParsingException(strError + strErrMsg);
		}

		return strAbsoluteIRI;
	}

	public static String getDataType(ParsedIRI baseIRI, String strDataType) {
		if (strDataType == null) {
			return strDataType;
		}
		if ( Util.XML_SCHEMA_DEFS.contains(strDataType) ) {
			return strDataType;
		}

		String strRetVal = null;
		try {
			strRetVal = resolveIRI(baseIRI, strDataType);
		}
		catch (IRIParsingException ex) {
			// ...continue...
		}
		return strRetVal;
	}

	public static RDFTransform getRDFTransform(ApplicationContext context, Project project)
			throws VocabularyIndexException, IOException {
		synchronized (project) {
			RDFTransform theTransform = (RDFTransform) project.overlayModels.get(RDFTransform.EXTENSION);
			if (theTransform == null) {
				theTransform = new RDFTransform(context, project);

				project.overlayModels.put(RDFTransform.EXTENSION, theTransform);
				project.getMetadata().updateModified();
			}
            return theTransform;
		}
	}

	public static ParsedIRI buildIRI(String strIRI) {
		if (strIRI == null) {
			if ( Util.isVerbose() )
				logger.error("ERROR: buildIRI(): Null IRI");
			return null;
		}

		ParsedIRI baseIRI = null;
		try {
			baseIRI = new ParsedIRI(strIRI);
		}
		catch (Exception ex) {
			if ( Util.isVerbose() )
				logger.error("ERROR: buildIRI(): Malformed IRI <" + strIRI + ">", ex);
			baseIRI = null;
		}

		return baseIRI;
	}

	public static Object evaluateExpression(Project project, String strExp, String strColumnName, Row theRow, int iRowIndex)
			throws ParsingException {
		// Create a bindings property just for this expression...
		Properties bindings = ExpressionUtils.createBindings(project);

		// Create an evaluator just for this expression...
		Evaluable eval = MetaParser.parse(strExp);

		// Find the cell index for the column under evaluation...
		int iCellIndex =
			( strColumnName == null || strColumnName.length() == 0 ) ?
				-1 : project.columnModel.getColumnByName(strColumnName).getCellIndex();

		// Evaluate the expression on the cell and return results...
		return RDFExpressionUtil.evaluate(eval, bindings, theRow, iRowIndex, strColumnName, iCellIndex);
	}

    public static Repository buildModel(Project project, Engine engine, RDFRowVisitor visitor) {
        FilteredRows filteredRows = engine.getAllFilteredRows();
		if ( Util.isVerbose(4) )
			logger.info("buildModel: visit matching filtered rows");
        filteredRows.accept(project, visitor);
        return visitor.getModel();
    }

    public static Repository buildModel(Project project, Engine engine, RDFRecordVisitor visitor) {
        FilteredRecords filteredRecords = engine.getFilteredRecords();
		if ( Util.isVerbose(4) )
			logger.info("buildModel: visit matching filtered records");
        filteredRecords.accept(project, visitor);
        return visitor.getModel();
    }

	public static boolean isVerbose() {
		return (Util.isVerbose(1));
	}
	public static boolean isVerbose(int iVerbose) {
		return (Util.iVerbosity >= iVerbose);
	}

	public static void setVerbosityByPreferenceStore() {
		// Setup for debug logging...
		PreferenceStore prefStore = ProjectManager.singleton.getPreferenceStore();
		if (prefStore != null) {
			var prefVerbosity = prefStore.get("RDFTransform/verbose"); // RDFTransform Verbosity
			if (prefVerbosity == null) {
				prefVerbosity = prefStore.get("verbose"); // General Verbosity
			}
			if (prefVerbosity != null) {
				Util.iVerbosity = Integer.parseInt( prefVerbosity.toString() );
			}
		}
	}

	public static String toSpaceStrippedString(Object obj) {
		return obj.toString().replaceAll("\uC2A0", " ").replaceAll("\\h", " ").strip();
	}
}
