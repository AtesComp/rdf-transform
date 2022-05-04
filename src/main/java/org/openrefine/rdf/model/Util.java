package org.openrefine.rdf.model;

import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.preference.PreferenceStore;
import com.google.refine.ProjectManager;

import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIFactory;
import org.apache.jena.iri.IRIException;
import org.apache.jena.irix.SetupJenaIRI;

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
 *  The Util class is a convenience class holding common values and functions used by others.
 */
public class Util {
    static public enum NodeType {
        ROW,
        RECORD,
        COLUMN,
        CONSTANT,
        EXPRESSION
    }

    static public final IRIFactory iriFactory = SetupJenaIRI.iriCheckerFactory();

    // RDF Transform JSON Strings
    // --------------------------------------------------------------------------------
    static public final String gstrProject = "project";
    static public final String gstrExtension = "extension";
    static public final String gstrVersion = "version";
    static public final String gstrBaseIRI = "baseIRI";
    static public final String gstrNamespaces = "namespaces";
    static public final String gstrSubjectMappings = "subjectMappings";
    static public final String gstrTypeMappings = "typeMappings";
    static public final String gstrPropertyMappings = "propertyMappings";
    static public final String gstrObjectMappings = "objectMappings";
    static public final String gstrPrefix = "prefix";
    static public final String gstrValueType = "valueType";
    static public final String gstrType = "type";
    static public final String gstrIRI = "iri";                          // type
    static public final String gstrLiteral = "literal";                  // type
    static public final String gstrDatatypeLiteral = "datatype_literal"; // type
    static public final String gstrDatatype = "datatype";                // key
    static public final String gstrLanguageLiteral = "language_literal"; // type
    static public final String gstrBNode = "bnode";                      // type
    static public final String gstrValueBNode = "value_bnode";           // type
    static public final String gstrValueSource = "valueSource";
    static public final String gstrSource = "source";
    static public final String gstrConstant = "constant";     // source & key
    static public final String gstrColumn = "column";         // source
    static public final String gstrColumnName = "columnName"; // key
    static public final String gstrRowIndex = "row_index";    // source, no key
    static public final String gstrRecordID = "record_id";    // source, no key
    static public final String gstrExpression = "expression"; // also source
    static public final String gstrLanguage = "language";     // also type key
    static public final String gstrGREL = "grel";
    static public final String gstrCode = "code";
    static public final String gstrCodeValue = "value";

    static public final String gstrNamespace = "namespace";
    static public final String gstrLabel = "label";
    static public final String gstrDesc = "desc";
    static public final String gstrDescription = "description";
    static public final String gstrLocalPart = "localPart";

    // XML Schema Strings
    // --------------------------------------------------------------------------------
    static private final String XSD_PREFIX        = "xsd:"; // for namespace http://www.w3.org/2001/XMLSchema#

    static private final String XSD_DUR_IRI      = XSD_PREFIX + "duration";
    static private final String XSD_DT_IRI       = XSD_PREFIX + "dateTime";
    static private final String XSD_TIME_IRI     = XSD_PREFIX + "time";
    static private final String XSD_DATE_IRI     = XSD_PREFIX + "date";
    static private final String XSD_GYM_IRI      = XSD_PREFIX + "gYearMonth";
    static private final String XSD_GYEAR_IRI    = XSD_PREFIX + "gYear";
    static private final String XSD_GMD_IRI      = XSD_PREFIX + "gMonthDay";
    static private final String XSD_GDAY_IRI     = XSD_PREFIX + "gDay";
    static private final String XSD_GMONTH_IRI   = XSD_PREFIX + "gMonth";
    static private final String XSD_BOOL_IRI     = XSD_PREFIX + "boolean";
    static private final String XSD_B64BIN_IRI   = XSD_PREFIX + "base64Binary";
    static private final String XSD_XBIN_IRI     = XSD_PREFIX + "hexBinary";
    static private final String XSD_FLOAT_IRI    = XSD_PREFIX + "float";
    static private final String XSD_DOUBLE_IRI   = XSD_PREFIX + "double";
    static private final String XSD_ANY_IRI      = XSD_PREFIX + "anyURI";
    static private final String XSD_QNAME_IRI    = XSD_PREFIX + "QName";
    static private final String XSD_NOTATION_IRI = XSD_PREFIX + "NOTATION";

    static private final String XSD_STR_IRI      = XSD_PREFIX + "string";
    static private final String XSD_NSTR_IRI     = XSD_PREFIX + "normalizedString";
    static private final String XSD_TOKEN_IRI    = XSD_PREFIX + "token";
    static private final String XSD_LANG_IRI     = XSD_PREFIX + "language";
    static private final String XSD_NAME_IRI     = XSD_PREFIX + "Name";
    static private final String XSD_NCNAME_IRI   = XSD_PREFIX + "NCName";
    static private final String XSD_ID_IRI       = XSD_PREFIX + "ID";
    static private final String XSD_IDREF_IRI    = XSD_PREFIX + "IDREF";
    static private final String XSD_IDREFS_IRI   = XSD_PREFIX + "IDREFS";
    static private final String XSD_ENITY_IRI    = XSD_PREFIX + "ENTITY";
    static private final String XSD_ENITIES_IRI  = XSD_PREFIX + "ENTITIES";
    static private final String XSD_NMTOKEN_IRI  = XSD_PREFIX + "NMTOKEN";
    static private final String XSD_NMTOKENS_IRI = XSD_PREFIX + "NMTOKENS";

    static private final String XSD_DEC_IRI      = XSD_PREFIX + "decimal";
    static private final String XSD_INTEGER_IRI  = XSD_PREFIX + "integer";
    static private final String XSD_NPINT_IRI    = XSD_PREFIX + "nonPositiveInteger";
    static private final String XSD_NINT_IRI     = XSD_PREFIX + "negativeInteger";
    static private final String XSD_LONG_IRI     = XSD_PREFIX + "long";
    static private final String XSD_INT_IRI      = XSD_PREFIX + "int";
    static private final String XSD_SHORT_IRI    = XSD_PREFIX + "short";
    static private final String XSD_BYTE_IRI     = XSD_PREFIX + "byte";
    static private final String XSD_NNINT_IRI    = XSD_PREFIX + "nonNegativeInteger";
    static private final String XSD_ULONG_IRI    = XSD_PREFIX + "unsignedLong";
    static private final String XSD_UINT_IRI     = XSD_PREFIX + "unsignedInt";
    static private final String XSD_USHORT_IRI   = XSD_PREFIX + "unsignedShort";
    static private final String XSD_UBYTE_IRI    = XSD_PREFIX + "unsignedByte";
    static private final String XSD_PINT_IRI     = XSD_PREFIX + "positiveInteger";

    static private final List<String> XML_SCHEMA_DEFS = Arrays.asList(
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

    static private final Logger logger = LoggerFactory.getLogger("RDFT:Util" );

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

    public static String resolveIRI(IRI baseIRI, String strIRI) throws IRIParsingException {
        String strError = "ERROR: resolveIRI: ";
        String strErrMsg = null;
        String strAbsoluteIRI = null;
        String strDEBUG = "DEBUG: resolveIRI: ";

        // No IRI is not a problem (there is just nothing to resolve)...
        if (strIRI == null || strIRI.length() == 0) {
            if ( Util.isDebugMode() ) Util.logger.info(strDEBUG + "No IRI");
            return strAbsoluteIRI;
        }

        // Create Absolute IRI without Base IRI...
        try {
            IRI absoluteIRI = Util.iriFactory.construct(strIRI);
            if ( absoluteIRI.isAbsolute() ) {
                strAbsoluteIRI = absoluteIRI.toString();
            }
        }
        catch (IRIException ex) {
            strErrMsg = ex.getMessage();
            // ...continue in case we can resolve as a Relative IRI...
        }

        // Not an Absolute IRI?
        if (strAbsoluteIRI == null && baseIRI != null) {
            // Create Absolute IRI with Relative IRI using Base IRI...
            try {
                strAbsoluteIRI = baseIRI.construct(strIRI).toString();
            }
            catch (IRIException ex) {
                strErrMsg = ex.getMessage();
                // ...continue in case it needs a little adjusting...
            }

            // Create Absolute IRI with adjusted Relative IRI using Base IRI...
            if (strAbsoluteIRI == null)
            {
                try {
                    if ( ! strIRI.startsWith("/") ) {
                        strAbsoluteIRI = baseIRI.construct("/" + strIRI).toString();
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
            Util.logger.info(strDebugOut);
        }

        if (strAbsoluteIRI == null && strErrMsg != null) {
            Util.logger.error(strError + "Malformed IRI [" + strIRI + "]");
            Util.logger.error(strError + strErrMsg);
            throw new Util.IRIParsingException(strError + strErrMsg);
        }

        return strAbsoluteIRI;
    }

    public int findLocalPartIndex(String strIRI) {
        if ( strIRI == null || strIRI.isEmpty() ) {
            return -1;
        }
        // From RDF4J Interface IRI documentation:
        // --------------------------------------------------------------------------------
        // An IRI can be split into a namespace part and a local name part, which are
        // derived from an IRI string by splitting it in two using the following algorithm:
        //
        // * Split after the first occurrence of the '#' character,
        // * If this fails, split after the last occurrence of the '/' character,
        // * If this fails, split after the last occurrence of the ':' character.
        //
        // The last step should never fail as every legal (full) IRI contains at least one
        // ':' character to separate the scheme from the rest of the IRI. The
        // implementation should check this upon object creation.
        int iIndex = strIRI.indexOf('#');
        if (iIndex == -1) {
            iIndex = strIRI.lastIndexOf('/');
            if (iIndex == -1) {
                iIndex = strIRI.lastIndexOf(':');
            }
        }
        if (iIndex != -1) { // ...found a split
            ++iIndex; // ...split after
            // ...because Namespace includes the character...
        }
        return iIndex;
    }

    public int findPrefixIndex(String strIRI) {
        //
        // A Prefixed Qualified Name is by definition an IRI of the form:
        //    prefix:FQDN
        // where the FQDN is just a representation of a host, therefore:
        //    prefix:host
        //
        // IRIs for this context can be represented essentially 2 ways:
        //    1. schema://host/path (an IRI)
        //    2. prefix:path (a condensed IRI expression, CIRIE)
        // NOTE: For 1, the '//' after the schema always indicates an authority component
        //       that contains a host component.  Also, the path component includes the '/'.
        // See:
        //   https://en.wikipedia.org/wiki/Internationalized_Resource_Identifier
        //   https://en.wikipedia.org/wiki/Uniform_Resource_Identifier
        //
        // NOTE: The second representation may contain a host component within the path.
        //       Without the '//', there is no definitive way for the IRI parser to know
        //       what is authority vs path.
        //
        //       Consider:
        //       1. If the path does not begin with a '/', then the content up to the first '/'
        //          can be interpreted as the host component.
        //          This also implies that there was no authority component (no '//').
        //       2. If the path does begin with a '/', it implies there is an authority component
        //          that contains a host component.
        //       Then, IRI parsing will interpret the Prefixed Qualified Name format as
        //         prefix:path
        //
        // We really don't care!  All we need to know is whether the text up to the first ':'
        // is a prefix for a CIRIE...

        int iIndex = -1;
        if (strIRI != null) {
            iIndex = strIRI.indexOf(":");
             // If we have a possible prefix but not a base IRI reference (where iIndex == 0)...
             // NOTE: The ':' could also be in the path
            if (iIndex > 0) {
                // Is there is a possible path...
                //    iIndex + 1 = the length of strQuery to the ':' inclusive
                //    Is there anything after...
                if (strIRI.length() > iIndex + 1) {
                    try {
                        IRI tempIRI = Util.iriFactory.construct(strIRI);
                        // ...it parsed as an IRI...
                        // If a scheme is present, but a host is not present...
                        if (tempIRI.getScheme() != null && tempIRI.getRawHost() == null) {
                            // There is no authority component:
                            //    i.e., there was no "schema://...", just "schema:...", so
                            //    the authority parsing that contains the host parsing was not
                            //    performed.  The rest may parse as a path, query, fragment.
                            // Then, the schema is a prefix and that is enough...
                            return iIndex; // ...accept it
                        }
                    }
                    catch (Exception ex) {
                        // ...continue: strQuery is NOT an IRI...yet...
                    }
                }
                // Otherwise, we have a string like "ccc:", so treat it as a possible prefix...
                else if ( strIRI.matches("\\S+") ) { // ...contains no whitespace...
                        return iIndex; // ...accept it
                }
            }
            // Else, we might have a possible base IRI reference (starts with ':")...
            // ...don't accept...
            /*
            else if (iIndex == 0 && strIRI.length() > 1) {
                // Create Absolute IRI with Relative IRI using Base IRI...
                try {
                    Project theProject = this.getProject(this.theRequest);
                    String strBaseIRI =
                        RDFTransform.getRDFTransform(theProject).getBaseIRI().toString();
                    ParsedIRI tempIRI = new ParsedIRI(strBaseIRI + strIRI.substring(1));
                    // It parses with the Base IRI...
                    bIsPrefixed = true; // ...accept it
                }
                catch (Exception ex) {
                    // ...continue...
                }
            }
            */
        }
        return iIndex;
    }

    public static String getDataType(IRI baseIRI, String strDatatypePrefix, String strDataTypeValue) {
        if (strDataTypeValue == null) {
            return strDataTypeValue;
        }
        String strDataType = strDatatypePrefix + ":" + strDataTypeValue;
        if (strDatatypePrefix == null) {
            strDataType = strDataTypeValue;
        }
        if ( Util.XML_SCHEMA_DEFS.contains(strDataType) ) {
            return strDataType;
        }

        String strResolvedDatatype = null;
        try {
            strResolvedDatatype = Util.resolveIRI(baseIRI, strDataType);
        }
        catch (IRIParsingException ex) {
            // strResolvedDatatype is still null...
            // ...continue...
        }
        return strResolvedDatatype;
    }

    public static IRI buildIRI(String strIRI) {
        if (strIRI == null) {
            if ( Util.isVerbose() || Util.isDebugMode()) Util.logger.error("ERROR: buildIRI(): Null IRI");
            return null;
        }

        IRI iriNew = null;
        try {
            iriNew = Util.iriFactory.construct(strIRI);
        }
        catch (Exception ex) {
            if ( Util.isVerbose() || Util.isDebugMode()) Util.logger.error("ERROR: buildIRI(): Malformed IRI <" + strIRI + ">", ex);
            iriNew = null;
        }

        return iriNew;
    }

    public static Object evaluateExpression(Project theProject, String strExpression, String strColumnName, int iRowIndex)
            throws ParsingException {
        //
        // Evaluate the expression on the cell and return results...
        //   NOTE: Here is where we tie the RDF Transform model to the data.
        //
        if ( Util.isDebugMode() ) {
            Util.logger.info("DEBUG: evaluateExpression: " +
                                "Exp: [" + strExpression + "] " +
                                "Col: [" + strColumnName + "] " +
                                "Row: [" + iRowIndex     + "]");
        }

        //
        if ( strExpression == null ) {
            return null;
        }

        // Select the column reference (er, cell index) by given name...
        int theColumn = -1;
        // If a regular column (not a row/record index column)...
        if ( ! ( strColumnName == null || strColumnName.isEmpty() ) ) {
            try {
                theColumn = theProject.columnModel.getColumnByName(strColumnName).getCellIndex();
            }
            catch (ClassCastException | NullPointerException ex) {
                // Let it be -1...continue...
            }
        }

        // Select the row by given row index...
        Row theRow = null;
        try {
            theRow = theProject.rows.get(iRowIndex);
        }
        catch (IndexOutOfBoundsException ex) {
            // Let it be null...continue...
        }

        // Select the data cell by row and column...
        Cell theCell = null;
        if (theColumn >= 0 && theRow != null) { // ...for a valid column and row...
            theCell = theRow.getCell(theColumn); // ...get the cell
        }
        // Otherwise, create a pseudo-cell...
        else {
            theCell = new Cell(iRowIndex, null);
        }

        // Create a bindings property for this expression...
        Properties bindings = ExpressionUtils.createBindings(theProject);

        // Bind the cell for expression evaluation...
        ExpressionUtils.bind(bindings, theRow, iRowIndex, strColumnName, theCell);

        // Create an evaluator for this expression...
        Evaluable eval = MetaParser.parse(strExpression);

        // Evaluate the expression on the cell for results...
        return eval.evaluate(bindings);
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
            Object prefVerbosity = prefStore.get("RDFTransform.verbose");
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
            Object prefExportStatementLimit = prefStore.get("RDFTransform.exportLimit");
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
            Object prefDebugMode = prefStore.get("RDFTransform.debug"); // RDFTransform Debug Mode
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
        if (obj == null) {
            return null;
        }
        return obj.toString().replaceAll("[\uC2A0\\p{C}\\p{Z}]+", "").strip();
    }

    public static String toNodeTypeString(NodeType eNodeType) {
        if ( eNodeType.equals(NodeType.ROW) ) {
            return "ROW";
        }
        if ( eNodeType.equals(NodeType.RECORD) ) {
            return "RECORD";
        }
        if ( eNodeType.equals(NodeType.COLUMN) ) {
            return "COLUMN";
        }
        if ( eNodeType.equals(NodeType.CONSTANT) ) {
            return "CONSTANT";
        }
        return "EXPRESSION";
    }

    public static String toNodeSourceString(NodeType eNodeType) {
        if ( eNodeType.equals(NodeType.ROW) ) {
            return Util.gstrRowIndex;
        }
        if ( eNodeType.equals(NodeType.RECORD) ) {
            return Util.gstrRecordID;
        }
        if ( eNodeType.equals(NodeType.COLUMN) ) {
            return Util.gstrColumn;
        }
        if ( eNodeType.equals(NodeType.CONSTANT) ) {
            return Util.gstrConstant;
        }
        return Util.gstrExpression;
    }
}
