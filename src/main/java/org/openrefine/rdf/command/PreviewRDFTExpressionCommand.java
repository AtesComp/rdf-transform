/*
 *  Class PreviewRDFTExpressionCommand
 *
 *  Processes an RDF Transform Expression for preview from the OpenRefine
 *  Data.
 *
 *  Copyright 2024 Keven L. Ates
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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

public class PreviewRDFTExpressionCommand extends PreviewExpressionCommand {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:PrevRDFTExpCmd");
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
                PreviewRDFTExpressionCommand.respondJSON(response, crErr);
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
                PreviewRDFTExpressionCommand.respondJSON(response, crErr);
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
            boolean bGood = false;
            if (bIsIRI) {
                bGood = this.respondIRIPreview();
            }
            else {
                bGood = this.respondLiteralPreview();
            }

            String strCode = "error";
            if (bGood) {
                strCode = "ok";
            }
            this.theWriter.writeStringField("code", strCode);
            this.theWriter.writeEndObject();

            //
            // Clean up...
            //
            this.theWriter.flush();
            this.theWriter.close();
        }
        catch (Exception ex) {
            PreviewRDFTExpressionCommand.respondException(response, ex);
        }
    }

    private boolean respondIRIPreview() throws IOException {
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
                    continue; // ...process the next row...
                }
                // Process empties...
                if ( ! ExpressionUtils.isNonBlankData(results) ) {
                    this.theWriter.writeNull();
                    continue; // ...process the next row...
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
                        // Remove all whitespace...
                        // NOTE: The expectation for this stripping is that the expression result will
                        //       be used for an IRI, so whitespace is NOT ALLOWED!
                        strResult = Util.toSpaceStrippedString( Array.get(results, iResult) );
                        if (strResult == null || strResult.isEmpty()) {
                            continue; // ...skip empties
                        }
                        // Prepend the prefix...
                        strResult = this.strPrefix + strResult;
                        if ( Util.isDebugMode() ) PreviewRDFTExpressionCommand.logger.info("DEBUG: Resource (" + iResult + "): [" + strResult + "]");
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
                    // Remove all whitespace...
                    // NOTE: The expectation for this stripping is that the expression result will
                    //       be used for an IRI, so whitespace is NOT ALLOWED!
                    strResult = Util.toSpaceStrippedString(results);
                    if ( strResult == null || strResult.isEmpty() ) {
                        this.theWriter.writeNull();
                        continue; // ...process the next row...
                    }
                    // Prepend the prefix...
                    strResult = this.strPrefix + strResult;
                    if ( Util.isDebugMode() ) PreviewRDFTExpressionCommand.logger.info("DEBUG: Resource: [" + strResult + "]");
                    this.theWriter.writeString(strResult);
                    astrAbsolutes[iRow] = Util.resolveIRI( this.baseIRI, strResult ); // ...absolute IRI
                }
            }
            this.theWriter.writeEndArray();

            //
            // Write Index for Row / Record...
            //
            if ( Util.isDebugMode() ) PreviewRDFTExpressionCommand.logger.info("DEBUG: Writing indicies on IRIs: " + iRows);
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
            String strMessageEx = PreviewRDFTExpressionCommand.strOtherError;
            // If exception on ParsingExpection types...
            if (ex.getClass() == Util.IRIParsingException.class ||
                ex.getClass() == ParsingException.class) {
                strTypeEx = "parser";
                if (ex.getClass() == ParsingException.class) {
                    strMessageEx = PreviewRDFTExpressionCommand.strParsingError;
                }
            }
            this.theWriter.writeStringField("type", strTypeEx);
            this.theWriter.writeStringField("message", strMessageEx);
            if ( Util.isVerbose() ) {
                PreviewRDFTExpressionCommand.logger.warn(strMessageEx, ex);
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
                    continue; // ...process the next row...
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
                PreviewRDFTExpressionCommand.logger.warn("WARNING: Writing absolute IRIs", ex);
                if ( Util.isVerbose(2) || Util.isDebugMode() ) ex.printStackTrace();
            }
        }
        return bGood;
    }

    private boolean respondLiteralPreview() throws IOException {
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
                    continue; // ...process the next row...
                }
                // Process empties...
                if ( ! ExpressionUtils.isNonBlankData(results) ) {
                    this.theWriter.writeNull();
                    continue; // ...process the next row...
                }
                // Process arrays...
                if ( results.getClass().isArray() ) {
                    int iResultCount = Array.getLength(results);
                    strbuffTemp.setLength(0);
                    strbuffTemp.append("[");
                    for (int iResult = 0; iResult < iResultCount; iResult++) {
                        strResult = null;
                        Object obj = Array.get(results, iResult);
                        if (obj != null) {
                            strResult = obj.toString();
                        }
                        if (strResult == null || strResult.isEmpty()) {
                            continue; // ...skip empties
                        }
                        if ( Util.isDebugMode() ) PreviewRDFTExpressionCommand.logger.info("DEBUG: Literal (" + iResult + "): [" + strResult + "]");
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
                    strResult = null;
                    if (results != null) {
                        strResult = results.toString();
                    }
                    if ( strResult == null || strResult.isEmpty() ) {
                        this.theWriter.writeNull();
                        continue; // ...process the next row...
                    }
                    if ( Util.isDebugMode() ) PreviewRDFTExpressionCommand.logger.info("DEBUG: Literal: [" + strResult + "]");
                    this.theWriter.writeString(strResult);
                }
            }
            this.theWriter.writeEndArray();

            //
            // Write Index for Row / Record...
            //
            if ( Util.isDebugMode() ) PreviewRDFTExpressionCommand.logger.info("DEBUG: Writing indicies on literals: " + iRows);
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
            String strMessageEx = PreviewRDFTExpressionCommand.strOtherError;
            // If exception on ParsingExpection types...
            if (ex.getClass() == ParsingException.class) {
                strTypeEx = "parser";
                strMessageEx = PreviewRDFTExpressionCommand.strParsingError;
            }
            this.theWriter.writeStringField("type", strTypeEx);
            this.theWriter.writeStringField("message", strMessageEx);
            if ( Util.isVerbose() ) {
                PreviewRDFTExpressionCommand.logger.warn(strMessageEx, ex);
                if ( Util.isVerbose(2) || Util.isDebugMode() ) ex.printStackTrace();
            }
            bGood = false; // ...no good anymore
        }
        return bGood;
    }
}
