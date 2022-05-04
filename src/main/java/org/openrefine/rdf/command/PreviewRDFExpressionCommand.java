package org.openrefine.rdf.command;

import java.io.IOException;
import java.lang.reflect.Array;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openrefine.rdf.model.Util;

import com.google.refine.commands.expr.PreviewExpressionCommand;
import com.google.refine.expr.EvalError;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.ParsingException;
import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;

import org.apache.jena.iri.IRI;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreviewRDFExpressionCommand extends PreviewExpressionCommand {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:PrevRDFValExpCmd");
    private final static String strParsingError = "WARNING: Parsing: No problem. Correct it.";
    private final static String strOtherError = "WARNING: Other: Some other problem occurred while parsing.";

    private Project theProject = null;
    private String strExpression = null;
    private JsonNode theRowIndices = null;
    private String strPrefix = ""; // No Prefix, Base IRI == ":", all others are "ccc:"
    private String strColumnName = null;
    private IRI baseIRI = null;

    private JsonGenerator theWriter = null;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            //
            // Set up response...
            //   ...cause we're hand-jamming JSON responses directly...
            //
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");

            //
            // Process parameters...
            //
            this.theProject = this.getProject(request);

            this.strExpression = request.getParameter("expression");

            String strRowIndices = request.getParameter("rowIndices");
            if (strRowIndices == null) {
                CodeResponse crErr = new CodeResponse("No row / record indices specified", true);
                PreviewRDFExpressionCommand.respondJSON(response, crErr);
                return;
            }
            this.theRowIndices = ParsingUtilities.evaluateJsonStringToArrayNode(strRowIndices);

            String strIsIRI = request.getParameter("isIRI");
            boolean bIsIRI = ( strIsIRI != null && strIsIRI.equals("1") ) ? true : false;

            String strPrefix = request.getParameter("prefix");
            if ( strPrefix != null ) {
                this.strPrefix = strPrefix;
            }

            this.strColumnName = request.getParameter("columnName");

            String strBaseIRI = request.getParameter("baseIRI");
            this.baseIRI = Util.buildIRI(strBaseIRI);
            if (this.baseIRI == null) {
                CodeResponse crErr = new CodeResponse("Invalid Base IRI", true);
                PreviewRDFExpressionCommand.respondJSON(response, crErr);
                return;
            }
            // ...end Parameters

            //
            // Set up the output
            //
            ObjectMapper mapper = new ObjectMapper();
            this.theWriter = mapper.getFactory().createGenerator(response.getWriter());

            //
            // Process the command...
            //
            if (bIsIRI) {
                this.respondIRIPreview();
            }
            else {
                this.respondLiteralPreview();
            }

            //
            // Clean up...
            //
            theWriter.flush();
            theWriter.close();
        }
        catch (Exception ex) {
            PreviewRDFExpressionCommand.respondException(response, ex);
        }
    }

    private void respondIRIPreview() throws IOException {
        int iRows = this.theRowIndices.size();

        this.theWriter.writeStartObject();

        String[] astrAbsolutes = new String[iRows];
        Integer[] aiIndices = new Integer[iRows];
        Boolean bGood = true;
        int iRow = 0;
        boolean bRecordMode = this.theProject.recordModel.hasRecords();
        StringBuffer strbuffTemp = new StringBuffer();
        StringBuffer strbuffTempAbs = new StringBuffer();
        String strResult;
        String strResultAbs;

        //
        // Write the results...
        //
        try {
            this.theWriter.writeArrayFieldStart("results");
            for (iRow = 0; iRow < iRows; iRow++) {
                Object results = null;
                strResult = null;
                astrAbsolutes[iRow] = null;
                strbuffTemp.setLength(0);

                int iRowIndex = this.theRowIndices.get(iRow).asInt();
                if (iRowIndex >= 0 && iRowIndex < this.theProject.rows.size()) {

                    // Store Index for Row / Record...
                    aiIndices[iRow] = iRowIndex;
                    if (bRecordMode)
                        aiIndices[iRow] = this.theProject.recordModel.getRecordOfRow(iRowIndex).recordIndex;

                    // NOTE: Expression evaluation will fail all the time because...typing!
                    //       It's constantly updating the preview as we type, so failure on
                    //       incomplete expressions!
                    results = Util.evaluateExpression(this.theProject, this.strExpression, this.strColumnName, iRowIndex);
                }
                else
                    break;

                // Process errors...
                if ( ExpressionUtils.isError(results) ) {
                    this.theWriter.writeStartObject();
                    this.theWriter.writeStringField("message", ((EvalError) results).message);
                    this.theWriter.writeEndObject();
                    continue; // ...keep processing the array...
                }
                // Process empties...
                if ( ! ExpressionUtils.isNonBlankData(results) ) {
                    this.theWriter.writeNull();
                    continue; // ...keep processing the array...
                }
                // Process arrays...
                if ( results.getClass().isArray() ) {
                    // NOTE: We'll prepare processing for absolute IRI value as well.
                    int iResultCount = Array.getLength(results);
                    strbuffTemp.setLength(0);
                    strbuffTemp.append("[");
                    strbuffTempAbs.setLength(0); // ...absolute IRI
                    strbuffTempAbs.append("["); // ...absolute IRI
                    for (int iResult = 0; iResult < iResultCount; iResult++) {
                        // Convert all non-breaking spaces to whitespace and strip string ends and
                        // prepend the prefix...
                        // NOTE: The expectation for this stripping is that the expression result will
                        //       be used for an IRI, so whitespace and non-breaking space is NOT ALLOWED!
                        strResult = this.strPrefix + Util.toSpaceStrippedString( Array.get(results, iResult) );
                        if ( Util.isDebugMode() ) PreviewRDFExpressionCommand.logger.info("DEBUG: Resource (" + iResult + "): [" + strResult + "]");
                        if (strResult == null || strResult.isEmpty()) { // ...skip empties
                            continue;
                        }
                        strbuffTemp.append(strResult);
                        strResultAbs = Util.resolveIRI(this.baseIRI, strResult); // ...absolute IRI
                        strbuffTempAbs.append(strResultAbs); // ...absolute IRI
                        if (iResult < iResultCount - 1) {
                            strbuffTemp.append(",");
                            strbuffTempAbs.append(","); // ...absolute IRI
                        }
                    }
                    strbuffTemp.append("]");
                    strResult = strbuffTemp.toString();
                    this.theWriter.writeString(strResult);

                    strbuffTempAbs.append("]"); // ...absolute IRI
                    astrAbsolutes[iRow] = strbuffTempAbs.toString(); // ...absolute IRI
                }
                // Process anything but an array as a string...
                else {
                    // Convert all non-breaking spaces to whitespace and strip string ends and
                    // prepend the prefix...
                    // NOTE: The expectation for this stripping is that the expression result will
                    //       be used for an IRI, so whitespace and non-breaking space is NOT ALLOWED!
                    strResult = this.strPrefix + Util.toSpaceStrippedString(results);
                    if ( Util.isDebugMode() ) PreviewRDFExpressionCommand.logger.info("DEBUG: Resource: [" + strResult + "]");
                    if ( strResult == null || strResult.isEmpty() ) {
                        this.theWriter.writeNull();
                        continue;
                    }
                    this.theWriter.writeString(strResult);
                    astrAbsolutes[iRow] = Util.resolveIRI( this.baseIRI, strResult ); // ...absolute IRI
                }
            }
            this.theWriter.writeEndArray();

            //
            // Write Index for Row / Record...
            //
            if ( Util.isDebugMode() ) PreviewRDFExpressionCommand.logger.info("DEBUG: Writing indicies on IRIs: " + iRows);
            this.theWriter.writeArrayFieldStart("indicies");
            for (iRow = 0; iRow < iRows; iRow++) {
                this.theWriter.writeString( aiIndices[iRow].toString() );
            }
            this.theWriter.writeEndArray();
        }
        catch (Exception ex) {
            this.theWriter.writeEndArray();
            // Parsing errors will always occur, so move on...

            String strTypeEx = "other";
            String strMessageEx = PreviewRDFExpressionCommand.strOtherError;
            // If exception on ParsingExpection types...
            if (ex.getClass() == Util.IRIParsingException.class ||
                ex.getClass() == ParsingException.class) {
                strTypeEx = "parser";
                if (ex.getClass() == ParsingException.class) {
                    strMessageEx = PreviewRDFExpressionCommand.strParsingError;
                }
            }
            this.theWriter.writeStringField("type", strTypeEx);
            this.theWriter.writeStringField("message", strMessageEx);
            if ( Util.isVerbose() ) {
                PreviewRDFExpressionCommand.logger.warn(strMessageEx, ex);
                if ( Util.isVerbose(2) || Util.isDebugMode() ) ex.printStackTrace();
            }
            bGood = false; // ...no good anymore
            iRows = iRow; // ...row error occurred--make it max
        }

        //
        // Write the absolutes...
        //
        try {
            this.theWriter.writeArrayFieldStart("absolutes");
            for (iRow = 0; iRow < iRows; iRow++) { // NOTE: iRows can be truncated from above errors.
                String strAbsolute = astrAbsolutes[iRow];
                if (strAbsolute == null) {
                    this.theWriter.writeNull();
                    continue;
                }
                this.theWriter.writeString(strAbsolute);
            }
            this.theWriter.writeEndArray();
        }
        catch (Exception ex) {
            this.theWriter.writeEndArray();
            if (bGood) { // ...some other error has NOT already happened...
                this.theWriter.writeStringField("type", "absolute");
                this.theWriter.writeStringField("message", ex.getMessage());
                bGood = false; // ...no good anymore
            }
            if ( Util.isVerbose() ) {
                PreviewRDFExpressionCommand.logger.warn("WARNING: Writing absolute IRIs", ex);
                if ( Util.isVerbose(2) || Util.isDebugMode() ) ex.printStackTrace();
            }
        }

        String strCode = "error";
        if (bGood) {
            strCode = "ok";
        }
        this.theWriter.writeStringField("code", strCode);
        this.theWriter.writeEndObject();
    }

    private void respondLiteralPreview() throws IOException {
        int iRows = this.theRowIndices.size();

        this.theWriter.writeStartObject();

        Integer[] aiIndices = new Integer[iRows];
        int iRow = 0;
        boolean bRecordMode = this.theProject.recordModel.hasRecords();
        StringBuffer strbuffTemp = new StringBuffer();
        String strResult;
        boolean bGood = true;

        //
        // Write the results...
        //
        try {
            this.theWriter.writeArrayFieldStart("results");
            for (iRow = 0; iRow < iRows; iRow++) {
                Object results = null;
                strbuffTemp.setLength(0);

                int iRowIndex = this.theRowIndices.get(iRow).asInt();
                if (iRowIndex >= 0 && iRowIndex < this.theProject.rows.size()) {

                    // Store Index for Row / Record...
                    aiIndices[iRow] = iRowIndex;
                    if (bRecordMode)
                        aiIndices[iRow] = this.theProject.recordModel.getRecordOfRow(iRowIndex).recordIndex;

                    // NOTE: Expression evaluation will fail all the time because...typing!
                    //       It's constantly updating the preview as we type, so failure on
                    //       incomplete expressions!
                    results = Util.evaluateExpression(this.theProject, this.strExpression, this.strColumnName, iRowIndex);
                }
                else
                    break;

                // Process errors...
                if ( ExpressionUtils.isError(results) ) {
                    this.theWriter.writeStartObject();
                    this.theWriter.writeStringField("message", ((EvalError) results).message);
                    this.theWriter.writeEndObject();
                    continue; // ...keep processing the array...
                }
                // Process empties...
                if ( ! ExpressionUtils.isNonBlankData(results) ) {
                    this.theWriter.writeNull();
                    continue; // ...keep processing the array...
                }
                // Process arrays...
                if ( results.getClass().isArray() ) {
                    int iResultCount = Array.getLength(results);
                    strbuffTemp.setLength(0);
                    strbuffTemp.append("[");
                    for (int iResult = 0; iResult < iResultCount; iResult++) {
                        // Convert all non-breaking spaces to whitespace and strip string ends...
                        // NOTE: The expectation for this stripping is that the expression result will
                        //       be used for an IRI, so whitespace and non-breaking space is NOT ALLOWED!
                        strResult = Util.toSpaceStrippedString( Array.get(results, iResult) );
                        if ( Util.isDebugMode() ) PreviewRDFExpressionCommand.logger.info("DEBUG: Literal (" + iResult + "): [" + strResult + "]");
                        if (strResult == null || strResult.isEmpty()) { // ...skip empties
                            continue;
                        }
                        strbuffTemp.append(strResult);
                        if (iResult < iResultCount - 1) {
                            strbuffTemp.append(",");
                        }
                    }
                    strbuffTemp.append("]");
                    strResult = strbuffTemp.toString();
                    this.theWriter.writeString(strResult);
                }
                // Process anything but an array as a string...
                else {
                    // Convert all non-breaking spaces to whitespace and strip string ends...
                    // NOTE: Not as strong an argument as for respondIRIPreview()
                    strResult = Util.toSpaceStrippedString(results);
                    if ( Util.isDebugMode() ) PreviewRDFExpressionCommand.logger.info("DEBUG: Literal: [" + strResult + "]");
                    if ( strResult == null || strResult.isEmpty() ) {
                        this.theWriter.writeNull();
                        continue;
                    }
                    this.theWriter.writeString(strResult);
                }
            }
            this.theWriter.writeEndArray();

            //
            // Write Index for Row / Record...
            //
            if ( Util.isDebugMode() ) PreviewRDFExpressionCommand.logger.info("DEBUG: Writing indicies on literals: " + iRows);
            this.theWriter.writeArrayFieldStart("indicies");
            for (iRow = 0; iRow < iRows; iRow++) {
                this.theWriter.writeString( aiIndices[iRow].toString() );
            }
            this.theWriter.writeEndArray();
        }
        catch (Exception ex) {
            this.theWriter.writeEndArray();
            // Parsing errors will always occur, so move on...

            String strTypeEx = "other";
            String strMessageEx = PreviewRDFExpressionCommand.strOtherError;
            // If exception on ParsingExpection types...
            if (ex.getClass() == ParsingException.class) {
                strTypeEx = "parser";
                strMessageEx = PreviewRDFExpressionCommand.strParsingError;
            }
            this.theWriter.writeStringField("type", strTypeEx);
            this.theWriter.writeStringField("message", strMessageEx);
            if ( Util.isVerbose() ) {
                PreviewRDFExpressionCommand.logger.warn(strMessageEx, ex);
                if ( Util.isVerbose(2) || Util.isDebugMode() ) ex.printStackTrace();
            }
            bGood = false; // ...no good anymore
        }

        String strCode = "error";
        if (bGood) {
            strCode = "ok";
        }
        this.theWriter.writeStringField("code", strCode);
        this.theWriter.writeEndObject();
    }
}
