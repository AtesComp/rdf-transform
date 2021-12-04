package com.google.refine.rdf.command;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URISyntaxException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.rdf.Util;

import com.google.refine.commands.expr.PreviewExpressionCommand;
import com.google.refine.expr.EvalError;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.ParsingException;
import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;

import org.eclipse.rdf4j.common.net.ParsedIRI;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreviewRDFExpressionCommand extends PreviewExpressionCommand {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:PrevRDFValExpCmd");
    private final static String strParsingError = "WARNING: Parsing: No problem. Correct it.";
    private final static String strOtherError = "WARNING: Other: Some other problem occurred while parsing.";

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");

            Project theProject = getProject(request);

            String strColumnName = request.getParameter("columnName");
            String strIsIRI = request.getParameter("isIRI");
            boolean bIsIRI = ( strIsIRI != null && strIsIRI.equals("1") ) ? true : false;

            String strExpression = request.getParameter("expression");
            String strRowIndices = request.getParameter("rowIndices");
            if (strRowIndices == null) {
                respond(response, "{ \"code\" : \"error\", \"message\" : \"No row/record indices specified\" }");
                return;
            }

            String strBaseIRI = request.getParameter("baseIRI");
            ParsedIRI baseIRI;
            try {
            	baseIRI = new ParsedIRI(strBaseIRI);
            }
            catch (URISyntaxException ex) {
            	respond(response, "{ \"code\" : \"error\", \"message\" : \"Invalid Base IRI\" }");
                return;
            }

            JsonNode jnodeRowIndices = ParsingUtilities.evaluateJsonStringToArrayNode(strRowIndices);

            ObjectMapper mapper = new ObjectMapper();
            JsonGenerator theWriter = mapper.getFactory().createGenerator(response.getWriter());
            if (bIsIRI) {
            	respondIRIPreview(theProject, theWriter, jnodeRowIndices, strExpression, strColumnName, baseIRI);
            }
            else {
            	respondLiteralPreview(theProject, theWriter, jnodeRowIndices, strExpression, strColumnName);
            }
            theWriter.flush();
            theWriter.close();
        }
        catch (Exception ex) {
            respondException(response, ex);
        }
	}

	private void respondIRIPreview(
                    Project theProject, JsonGenerator theWriter, JsonNode jnodeRowIndices, String strExpression,
                    String strColumnName, ParsedIRI baseIRI )
            throws IOException {
		int iRows = jnodeRowIndices.size();

        theWriter.writeStartObject();

        String[] astrAbsolutes = new String[iRows];
        Integer[] aiIndices = new Integer[iRows];
        Boolean bGood = true;
        int iRow = 0;
        boolean bRecordMode = theProject.recordModel.hasRecords();
        StringBuffer strbuffTemp = new StringBuffer();
        StringBuffer strbuffTempAbs = new StringBuffer();
        String strResult;
        String strResultAbs;

        //
        // Write the results...
        //
        try {
            theWriter.writeArrayFieldStart("results");
            for (iRow = 0; iRow < iRows; iRow++) {
                Object result = null;
                strResult = null;
                astrAbsolutes[iRow] = null;
                strbuffTemp.setLength(0);

                int iRowIndex = jnodeRowIndices.get(iRow).asInt();
                if (iRowIndex >= 0 && iRowIndex < theProject.rows.size()) {

                    // Store Index for Row / Record...
                    aiIndices[iRow] = iRowIndex;
                    if (bRecordMode)
                        aiIndices[iRow] = theProject.recordModel.getRecordOfRow(iRowIndex).recordIndex;

                    // NOTE: Expression evaluation will fail all the time because...typing!
                    //       It's constantly updating the preview as we type, so failure on
                    //       incomplete expressions!
                    result = Util.evaluateExpression(theProject, strExpression, strColumnName, iRowIndex);
                }
                else
                    break;

                // Process errors...
                if ( ExpressionUtils.isError(result) ) {
                    theWriter.writeStartObject();
                    theWriter.writeStringField("message", ((EvalError) result).message);
                    theWriter.writeEndObject();
                    continue; // ...keep processing the array...
                }
                // Process empties...
                if (result == null || (result instanceof String && ((String) result).isEmpty())) {
                    theWriter.writeNull();
                    continue; // ...keep processing the array...
                }
                // Process arrays...
                if ( result.getClass().isArray() ) {
                    // NOTE: We'll prepare processing for absolute IRI value as well.
                    int iResultCount = Array.getLength(result);
                    strbuffTemp.setLength(0);
                    strbuffTemp.append("[");
                    strbuffTempAbs.setLength(0); // ...absolute IRI
                    strbuffTempAbs.append("["); // ...absolute IRI
                    for (int iResult = 0; iResult < iResultCount; iResult++) {
                        // Convert all non-breaking spaces to whitespace and strip string ends...
                        // NOTE: The expectation for this stripping is that the expression result will
                        //       be used for an IRI, so whitespace and non-breaking space is NOT ALLOWED!
                        strResult = Util.toSpaceStrippedString( Array.get(result, iResult) );
                        if ( Util.isDebugMode() ) logger.info("DEBUG: Result (" + iResult + "): [" + strResult + "]");
                        if (strResult == null || strResult.isEmpty()) { // ...skip empties
                            continue;
                        }
                        strbuffTemp.append(strResult);
                        strResultAbs = Util.resolveIRI(baseIRI, strResult); // ...absolute IRI
                        strbuffTempAbs.append(strResultAbs); // ...absolute IRI
                        if (iResult < iResultCount - 1) {
                            strbuffTemp.append(",");
                            strbuffTempAbs.append(","); // ...absolute IRI
                        }
                    }
                    strbuffTemp.append("]");
                    strResult = strbuffTemp.toString();
                    theWriter.writeString(strResult);

                    strbuffTempAbs.append("]"); // ...absolute IRI
                    astrAbsolutes[iRow] = strbuffTempAbs.toString(); // ...absolute IRI
                }
                // Process anything but an array as a string...
                else {
                    PreviewExpressionCommand.writeValue(strbuffTemp, result, false);
                    // Convert all non-breaking spaces to whitespace and strip string ends...
                    // NOTE: The expectation for this stripping is that the expression result will
                    //       be used for an IRI, so whitespace and non-breaking space is NOT ALLOWED!
                    strResult = Util.toSpaceStrippedString(strbuffTemp);
                    if ( Util.isDebugMode() ) logger.info("DEBUG: Result: [" + strResult + "]");
                    if ( strResult.isEmpty() ) {
                        theWriter.writeNull();
                        continue;
                    }
                    theWriter.writeString(strResult);
                    astrAbsolutes[iRow] = Util.resolveIRI( baseIRI, strResult ); // ...absolute IRI
                }
            }
            theWriter.writeEndArray();
            
            //
            // Write Index for Row / Record...
            //
            if ( Util.isDebugMode() ) logger.info("DEBUG: Writing indicies on IRIs: " + iRows);
            theWriter.writeArrayFieldStart("indicies");
            for (iRow = 0; iRow < iRows; iRow++) {
                theWriter.writeString( aiIndices[iRow].toString() );
            }
            theWriter.writeEndArray();
        }
        catch (Exception ex) {
        	theWriter.writeEndArray();
            // Parsing errors will always occur, so move on...

            String strTypeEx = "other";
            String strMessageEx = strOtherError;
            // If exception on ParsingExpection types...
            if (ex.getClass() == Util.IRIParsingException.class ||
                ex.getClass() == ParsingException.class) {
                strTypeEx = "parser";
                if (ex.getClass() == ParsingException.class) {
                    strMessageEx = strParsingError;
                }
            }
            theWriter.writeStringField("type", strTypeEx);
            theWriter.writeStringField("message", strMessageEx);
            if ( Util.isVerbose() ) {
                logger.warn(strMessageEx, ex);
                if ( Util.isVerbose(2) ) ex.printStackTrace();
            }
            bGood = false; // ...no good anymore
            iRows = iRow; // ...row error occurred--make it max
        }

        //
        // Write the absolutes...
        //
        try {
            theWriter.writeArrayFieldStart("absolutes");
            for (iRow = 0; iRow < iRows; iRow++) { // NOTE: iRows can be truncated from above errors.
                String strAbsolute = astrAbsolutes[iRow];
                if (strAbsolute == null) {
                    theWriter.writeNull();
                    continue;
                }
                theWriter.writeString(strAbsolute);
            }
            theWriter.writeEndArray();
        }
        catch (Exception ex) {
        	theWriter.writeEndArray();
            if (bGood) { // ...some other error has NOT already happened...
                theWriter.writeStringField("type", "absolute");
                theWriter.writeStringField("message", ex.getMessage());
                bGood = false; // ...no good anymore
            }
            if ( Util.isVerbose() ) {
                logger.warn("WARNING: Writing absolute IRIs", ex);
                if ( Util.isVerbose(2) ) ex.printStackTrace();
            }
        }

        String strCode = "error";
        if (bGood) {
            strCode = "ok";
        }
        theWriter.writeStringField("code", strCode);
        theWriter.writeEndObject();
	}

	private void respondLiteralPreview(
                    Project theProject, JsonGenerator theWriter, JsonNode jnodeRowIndices, String strExpression,
                    String strColumnName)
            throws IOException {
		int iRows = jnodeRowIndices.size();

        theWriter.writeStartObject();

        Integer[] aiIndices = new Integer[iRows];
        int iRow = 0;
        boolean bRecordMode = theProject.recordModel.hasRecords();
        StringBuffer strbuffTemp = new StringBuffer();
        String strResult;
        boolean bGood = true;
        try {
            theWriter.writeArrayFieldStart("results");
            for (iRow = 0; iRow < iRows; iRow++) {
                Object result = null;
                strbuffTemp.setLength(0);

                int iRowIndex = jnodeRowIndices.get(iRow).asInt();
                if (iRowIndex >= 0 && iRowIndex < theProject.rows.size()) {

                    // Store Index for Row / Record...
                    aiIndices[iRow] = iRowIndex;
                    if (bRecordMode)
                        aiIndices[iRow] = theProject.recordModel.getRecordOfRow(iRowIndex).recordIndex;

                    // NOTE: Expression evaluation will fail all the time because...typing!
                    //       It's constantly updating the preview as we type, so failure on
                    //       incomplete expressions!
                    result = Util.evaluateExpression(theProject, strExpression, strColumnName, iRowIndex);
                }
                else
                    break;

                if ( ExpressionUtils.isError(result) ) {
                    theWriter.writeStartObject();
                    theWriter.writeStringField("message", ((EvalError) result).message);
                    theWriter.writeEndObject();
                    continue; // ...keep processing the array...
                }
                if (result == null || (result instanceof String && ((String) result).isEmpty())) {
                    theWriter.writeNull();
                    continue; // ...keep processing the array...
                }
                PreviewExpressionCommand.writeValue(strbuffTemp, result, false);
                // Convert all non-breaking spaces to whitespace and strip string ends...
                // NOTE: Not as strong an argument as for respondIRIPreview()
                strResult = Util.toSpaceStrippedString(strbuffTemp);
                theWriter.writeString(strResult);
            }
            theWriter.writeEndArray();

            //
            // Write Index for Row / Record...
            //
            if ( Util.isDebugMode() ) logger.info("DEBUG: Writing indicies on literals: " + iRows);
            theWriter.writeArrayFieldStart("indicies");
            for (iRow = 0; iRow < iRows; iRow++) {
                theWriter.writeString( aiIndices[iRow].toString() );
            }
            theWriter.writeEndArray();
        }
        catch (Exception ex) {
        	theWriter.writeEndArray();
            // Parsing errors will always occur, so move on...

            String strTypeEx = "other";
            String strMessageEx = strOtherError;
            // If exception on ParsingExpection types...
            if (ex.getClass() == ParsingException.class) {
                strTypeEx = "parser";
                strMessageEx = strParsingError;
            }
            theWriter.writeStringField("type", strTypeEx);
            theWriter.writeStringField("message", strMessageEx);
            if ( Util.isVerbose() ) {
                logger.warn(strMessageEx, ex);
                if ( Util.isVerbose(2) ) ex.printStackTrace();
            }
            bGood = false; // ...no good anymore
        }

        String strCode = "error";
        if (bGood) {
            strCode = "ok";
        }
        theWriter.writeStringField("code", strCode);
        theWriter.writeEndObject();
	}
}
