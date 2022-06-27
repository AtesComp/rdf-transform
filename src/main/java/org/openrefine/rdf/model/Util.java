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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

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

    static public final String WHITESPACE = "\uC2A0\\p{C}\\p{Z}";
    //static public final String WHITESPACE = "\\p{Cc}\\p{Co}\\p{Cn}\\p{Z}";

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
    // Preference Setting Defaults...
    //      See setPreferencesByPreferenceStore() for preferences settable by OpenRefine.
    //
    static private Map<String, Object> Preferences =
        new HashMap<String, Object>() {{
            // Settable by OpenRefine Preferences...
            put("iVerbosity", 0);
            put("iExportLimit", 10737418);
            put("bPreviewStream", false);
            put("bDebugMode", false);
            put("bDebugJSON", false);
            // Settable only in RDF Transform UI...
            put("iSampleLimit", 20);
        }};

//
// PCRE IRI Resolution -----
//
//   See https://stackoverflow.com/questions/161738/what-is-the-best-regular-expression-to-check-if-a-string-is-a-valid-url

    static public class IRIParsingException extends Exception {
        public IRIParsingException(String strErrorMessage) {
            super(strErrorMessage);
        }
    }

    static public String resolveIRI(IRI baseIRI, String strIRI) throws IRIParsingException {
        String strResolveIRI = "resolveIRI: ";
        String strError = "ERROR: " + strResolveIRI;
        String strErrMsg = null;
        String strAbsoluteIRI = null;
        String strDEBUG = "DEBUG: " + strResolveIRI;

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
            Util.logger.error(strError + "Malformed IRI [" + strIRI + "] : " + strErrMsg);
            throw new Util.IRIParsingException(strResolveIRI + strErrMsg);
        }

        return strAbsoluteIRI;
    }

    static public int findLocalPartIndex(String strIRI) {
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

    static public int findPrefixIndex(String strIRI) {
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
        if (strIRI == null) {
            return iIndex;
        }

        iIndex = strIRI.indexOf(":");
        // If we have a base IRI reference (0) or no prefix (-1)...
        if (iIndex < 1) {
            return iIndex;
        }

        // We have a possible prefix but not a base IRI reference,
        // since iIndex > 0...
        // NOTE: The ':' could also be in the path

        // Is there is a possible path...
        //    iIndex + 1 = the length of strQuery to the ':' inclusive
        //    Is there anything after...
        if (strIRI.length() > iIndex + 1) {
            IRI tempIRI = Util.buildIRI(strIRI, true);
            if (tempIRI == null) { // ...a BAD IRI?...
                return -2;
            }
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
        // Otherwise, we have a string like "ccc:", so treat it as a possible prefix...
        // If the string contains no whitespace...
        else if ( strIRI.length() == Util.removeAllWhitespace(strIRI).length() ) {
            return iIndex; // ...accept it
        }
        // Otherwise, not a valid IRI string, so don't accept...
        return -3;
    }

    static public  boolean isPrefixedIRI(String strIRI) {
        // Check for normal prefixed IRIs (...ccc:ccc...) and
        //      base prefix IRIs...
        return ( Util.findPrefixIndex(strIRI) >= 0 );
    }

    static public String getDataType(IRI baseIRI, String strDatatypePrefix, String strDataTypeValue) {
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

    static public IRI buildIRI(String strIRI) {
        return Util.buildIRI(strIRI, false);
    }

    static public IRI buildIRI(String strIRI, boolean bTest) {
        String strHeader = (bTest ? "TEST: " : "ERROR: ") + "buildIRI(): ";

        if (strIRI == null) {
            if ( Util.isVerbose() || Util.isDebugMode()) Util.logger.error(strHeader + "Null IRI");
            return null;
        }

        IRI iriNew = null;
        try {
            iriNew = Util.iriFactory.construct(strIRI);
        }
        catch (Exception ex) {
            if ( Util.isVerbose() || Util.isDebugMode()) Util.logger.error(strHeader + "Malformed IRI <" + strIRI + ">", ex);
        }

        return iriNew;
    }

    static public Object evaluateExpression(Project theProject, String strExpression, String strColumnName, int iRowIndex)
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

    static public boolean isVerbose() {
        return ( Util.isVerbose(1) );
    }

    static public boolean isVerbose(int iVerbose) {
        return ( (int) Util.Preferences.get("iVerbosity") >= iVerbose );
    }

    static public int getVerbose() {
        return (int) Util.Preferences.get("iVerbosity");
    }

    static public int getExportLimit() {
        return (int) Util.Preferences.get("iExportLimit");
    }

    //
    // Preview Stream:
    //
    // The preview preference: Pretty or Stream.
    // NOTE: When set to false, use Pretty.
    //
    static public void setPreviewStream(boolean bPreviewStream) {
        Util.Preferences.put("bPreviewStream", bPreviewStream);
    }

    static public boolean isPreviewStream() {
        return (boolean) Util.Preferences.get("bPreviewStream");
    }
    // ...end Preview Stream

    static public boolean isDebugMode() {
        return (boolean) Util.Preferences.get("bDebugMode");
    }

    static public boolean isDebugJSON() {
        return (boolean) Util.Preferences.get("bDebugJSON");
    }

    //
    // Sample Limit:
    //
    // The limit on the number of sample rows or records processed.
    // NOTE: When set to 0, there is no limit.
    //
    static public void setSampleLimit(int iSampleLimit) {
        if (iSampleLimit >= 0) {
            Util.Preferences.put("iSampleLimit", iSampleLimit);
        }
    }

    static public int getSampleLimit() {
        int iSampleLimit = 10; // ...default, see Util's Preferences variable
        Integer icSampleLimit = (Integer) Util.Preferences.get("iSampleLimit");
        if (icSampleLimit != null && icSampleLimit >= 0) {
            iSampleLimit = icSampleLimit;
        }
        return iSampleLimit;

    }
    // ...end Sample Limit

    static public String preferencesToString() {
        //
        // Output RDFTranform Preferences...
        //
        String strPrefs = "Preferences: { ";
        String strComma = ", ";
        boolean bNotFirstEntry = false;
        Set<Map.Entry<String, Object>> entrySet = Preferences.entrySet();
        for (Map.Entry<String, Object> entry : entrySet) {
            if (bNotFirstEntry) {
                strPrefs += strComma;
            }
            strPrefs += entry.getKey() + ":" + entry.getValue().toString();
            bNotFirstEntry = true;
        }
        strPrefs +=   " }";
        return strPrefs;
    }

    static public void setPreferencesByPreferenceStore() {
        Util.logger.info("Getting Preferences from Preference Store...");

        PreferenceStore prefStore = ProjectManager.singleton.getPreferenceStore();
        if (prefStore == null) {
            return;
        }

        Object obj = null;

        //
        // Set Verbosity for logging...
        //
        // * 0 (or missing preference) == no verbosity and unknown, uncaught errors (stack traces, of course)
        // * 1 == basic functional information and all unknown, caught errors
        // * 2 == additional info and warnings on well-known issues: functional exits, permissibly missing data, etc
        // * 3 == detailed info on functional minutiae and warnings on missing, but desired, data
        // * 4 == controlled error catching stack traces, RDF preview statements, and other highly anal minutiae
        //
        obj = prefStore.get("RDFTransform.verbose");
        if (obj == null) {
            obj = prefStore.get("verbose"); // General OpenRefine Verbosity
        }
        if (obj != null) {
            try {
                Util.Preferences.put("iVerbosity", Integer.parseInt( obj.toString() ) );
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
        obj = prefStore.get("RDFTransform.exportLimit");
        if (obj != null) {
            try {
                Util.Preferences.put("iExportLimit", Integer.parseInt( obj.toString() ) );
            }
            catch (Exception ex) {
                // No problem: take default and continue...
            }
        }

        //
        // Set Preview Stream Mode...
        //
        // The Preview Stream Mode (bPreviewStream) is used to manage the preview for pretty vs stream output.
        //
        obj = prefStore.get("RDFTransform.previewStream"); // RDFTransform Preview Stream Mode
        if (obj != null) {
            try {
                Util.Preferences.put("bPreviewStream", Boolean.parseBoolean( obj.toString() ) );
            }
            catch (Exception ex) {
                // No problem: take default and continue...
            }
        }

        //
        // Set Debug Mode...
        //
        // The Debug Mode (bDebug) is used to manage the output of specifically marked "debug" messages.
        //
        obj = prefStore.get("RDFTransform.debug"); // RDFTransform Debug Mode
        if (obj == null) {
            obj = prefStore.get("debug"); // General OpenRefine Debug
        }
        if (obj != null) {
            try {
                Util.Preferences.put("bDebugMode", Boolean.parseBoolean( obj.toString() ) );
            }
            catch (Exception ex) {
                // No problem: take default and continue...
            }
        }

        //
        // Set Debug JSON...
        //
        // The Debug JSON (bDebugJSON) is used to manage the output of specifically marked "debug JSON" messages.
        //
        obj = prefStore.get("RDFTransform.debugJSON"); // RDFTransform Debug JSON Mode
        if (obj != null) {
            try {
                Util.Preferences.put("bDebugJSON", Boolean.parseBoolean( obj.toString() ) );
            }
            catch (Exception ex) {
                // No problem: take default and continue...
            }
        }
    }

    static public String replaceAll(String strUTF16, String strRegEx, String strReplace) {
        //String strUTF8 = new String( strUTF16.getBytes(StandardCharsets.UTF_16), StandardCharsets.UTF_8);
        //strUTF8 = strUTF8.replaceAll(strRegEx, strReplace);
        //strUTF16 = new String( strUTF8.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_16);
        strUTF16 = strUTF16.replaceAll(strRegEx, strReplace);
        return strUTF16;
    }

    static public String replaceFirst(String strUTF16, String strRegEx, String strReplace) {
        //String strUTF8 = new String( strUTF16.getBytes(StandardCharsets.UTF_16), StandardCharsets.UTF_8);
        //strUTF8 = strUTF8.replaceFirst(strRegEx, strReplace);
        //strUTF16 = new String( strUTF8.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_16);
        strUTF16 = strUTF16.replaceFirst(strRegEx, strReplace);
        return strUTF16;
    }

    static public String removeAllWhitespace(String strUTF16) {
        return Util.replaceAll(strUTF16, "[" + Util.WHITESPACE + "]+", "");
    }

    static public String replaceAllWhitespace(String strUTF16) {
        return Util.replaceAll(strUTF16, "[" + Util.WHITESPACE + "]", " ");
    }

    static public String toSpaceStrippedString(Object obj) {
        if (obj == null) {
            return null;
        }
        return Util.removeAllWhitespace( obj.toString() ).strip();
    }

    static public String toNodeTypeString(NodeType eNodeType) {
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

    static public String toNodeSourceString(NodeType eNodeType) {
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
