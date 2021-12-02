package com.google.refine.rdf;

import com.google.refine.rdf.app.ApplicationContext;
import com.google.refine.rdf.expr.util.RDFExpressionUtil;
import com.google.refine.rdf.vocab.VocabularyIndexException;

import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.model.Project;
import com.google.refine.preference.PreferenceStore;
import com.google.refine.ProjectManager;

import org.eclipse.rdf4j.common.net.ParsedIRI;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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

	//
	// Preference Setting Defaults (see setPreferencesByPreferenceStore)...
	//
	private static Map<String, Object> Preferences =
		new HashMap<String, Object>() {{
			put("Verbosity", 0);
			put("ExportLimit", 10737418);
			put("DebugMode", false);
	
			put("SampleLimit", 10);
		}};

//
// PCRE IRI Resolution -----
//
//   See https://stackoverflow.com/questions/161738/what-is-the-best-regular-expression-to-check-if-a-string-is-a-valid-url

	public static class IRIParsingException extends Exception { 
		public IRIParsingException(String strErrorMessage) {
			super(strErrorMessage);
		}
	}

	public static String resolveIRI(ParsedIRI baseIRI, String strIRI) throws IRIParsingException {
		String strError = "ERROR: resolveIRI(): ";
		String strErrMsg = null;
		String strAbsoluteIRI = null;
		String strDEBUG = "DEBUG resolveIRI: ";

		// No IRI is not a problem (there is just nothing to resolve)...
		if (strIRI == null || strIRI.length() == 0) {
			if ( Util.isDebugMode() )
				logger.info(strDEBUG + "No IRI");
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
		if ( Util.isDebugMode() ) {
			String strDebugOut;
			if (strAbsoluteIRI == null)
				strDebugOut = strDEBUG + "NULL " + strErrMsg;
			else
				strDebugOut = strDEBUG + strAbsoluteIRI;
			logger.info(strDebugOut);
		}

		if (strAbsoluteIRI == null && strErrMsg != null) {
			logger.error(strError + "Malformed IRI [" + strIRI + "]");
			logger.error(strError + strErrMsg);
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
			// strRetVal is still null...
			// ...continue...
		}
		return strRetVal;
	}

	public static RDFTransform getRDFTransform(ApplicationContext theContext, Project theProject)
			throws VocabularyIndexException, IOException {
		synchronized (theProject) {
			RDFTransform theTransform = (RDFTransform) theProject.overlayModels.get(RDFTransform.EXTENSION);
			if (theTransform == null) {
				theTransform = new RDFTransform(theContext, theProject);

				theProject.overlayModels.put(RDFTransform.EXTENSION, theTransform);
				theProject.getMetadata().updateModified();
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

	public static Object evaluateExpression(Project theProject, String strExp, String strColumnName, int iRowIndex)
			throws ParsingException {
		// Create a bindings property just for this expression...
		Properties bindings = ExpressionUtils.createBindings(theProject);

		// Create an evaluator just for this expression...
		Evaluable eval = MetaParser.parse(strExp);

		// Find the cell index for the column under evaluation...
		int iCellIndex =
			( strColumnName == null || strColumnName.length() == 0 ) ?
				-1 : theProject.columnModel.getColumnByName(strColumnName).getCellIndex();

		// Evaluate the expression on the cell and return results...
		return RDFExpressionUtil.evaluate(eval, bindings, theProject.rows.get(iRowIndex), iRowIndex, strColumnName, iCellIndex);
	}

	public static boolean isVerbose() {
		return ( Util.isVerbose(1) );
	}
	public static boolean isVerbose(int iVerbose) {
		return ( (int) Util.Preferences.get("Verbosity") >= iVerbose );
	}

	public static int getExportLimit() {
		return (int) Util.Preferences.get("ExportLimit");
	}

	public static boolean isDebugMode() {
		return (boolean) Util.Preferences.get("DebugMode");
	}

	//
	// Sample Limit:
	//
	// The limit on the number of sample rows or records processed.
    // NOTE: When set to 0, there is no limit.
	//
	public static int getSampleLimit() {
		return (int) Util.Preferences.get("SampleLimit");
	}

	public static void setSampleLimit(int iSampleLimit) {
		if (iSampleLimit >= 0) {
			Util.Preferences.put("SampleLimit", iSampleLimit);
		}
	}
	// ...end Sample Limit

	public static void setPreferencesByPreferenceStore() {
		PreferenceStore prefStore = ProjectManager.singleton.getPreferenceStore();
		if (prefStore != null) {
			//
			// Set Verbosity for logging...
			//
			// * 0 (or missing preference) == no verbosity and unknown, uncaught errors (stack traces, of course)
			// * 1 == basic functional information and all unknown, caught errors
			// * 2 == additional info and warnings on well-known issues: functional exits, permissibly missing data, etc
			// * 3 == detailed info on functional minutiae and warnings on missing, but desired, data
			// * 4 == controlled error catching stack traces, RDF preview statements, and other highly anal minutiae
			//
			Object prefVerbosity = prefStore.get("RDFTransform/verbose");
			if (prefVerbosity == null) {
				prefVerbosity = prefStore.get("verbose"); // General OpenRefine Verbosity
			}
			if (prefVerbosity != null) {
				try {
					Util.Preferences.put("Verbosity", Integer.parseInt( prefVerbosity.toString() ) );
				}
				catch (Exception ex) {
					// No problem: take default and continue...
				}
			}
			//
			// Set Export Statement Limit...
			//
			// The Export Statement Limit (iExportStatementLimit) is used to manage the
			// statement writing process for an RDF export to a disk file.
			//
			// Ideally, the limit on the number of statements held in memory for buffered
			// writing would be based on the input data size and available memory.  Generally,
			// the processing buffer would be approximately 1/10 to 1/1000 of the data size
			// depending on available memory.  A reasonable limit might be 1 GiB:
			//      1 GiB = 1024 * 1024 * 1024 bytes = 1073741824 bytes
			// Since we only have the number of statements currently in the connection,
			// we estimate the size of a statement to 100 bytes.  The average is probably
			// smaller.  Then, the estimated number of statements for a flush is set to:
			//      1073741824 bytes / 100 bytes per statements ~= 10737418 statements
			// This is the default limit and is overridden by the user preference.
			//
			Object prefExportStatementLimit = prefStore.get("RDFTransform/exportLimit");
			if (prefExportStatementLimit != null) {
				try {
					Util.Preferences.put("ExportLimit", Integer.parseInt( prefExportStatementLimit.toString() ) );
				}
				catch (Exception ex) {
					// No problem: take default and continue...
				}
			}

			//
			// Set Debug Mode...
			//
			// The Debug Mode (iDebug) is used to manage the output of specifically marked "debug" messages.
			//
			Object prefDebugMode = prefStore.get("RDFTransform/debug"); // RDFTransform Debug Mode
			if (prefDebugMode == null) {
				prefDebugMode = prefStore.get("debug"); // General OpenRefine Debug
			}
			if (prefDebugMode != null) {
				try {
					Util.Preferences.put("DebugMode", Boolean.parseBoolean( prefDebugMode.toString() ) );
				}
				catch (Exception ex) {
					// No problem: take default and continue...
				}
			}
		}
	}

	public static String toSpaceStrippedString(Object obj) {
		return obj.toString().replaceAll("\uC2A0", " ").replaceAll("\\h", " ").strip();
	}
}
