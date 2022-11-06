/*
 *  Class CellResourceNode
 *
 *  A Resource Node Cell class for managing cell based Resource nodes in an RDF
 *  Transform.
 *
 *  Copyright 2022 Keven L. Ates
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

package org.openrefine.rdf.model;

import java.util.ArrayList;
import java.io.IOException;

import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.ParsingException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerationException;

import org.apache.jena.rdf.model.RDFNode;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CellResourceNode extends ResourceNode implements CellNode {
    static private final Logger logger = LoggerFactory.getLogger("RDFT:CellResNode");

    static private final String strNODETYPE = "cell-as-resource";

    private final String strColumnName;
    private final String strPrefix;

    @JsonCreator
    public CellResourceNode(String strColumnName, String strPrefix, String strExp, boolean bIsIndex, Util.NodeType eNodeType)
    {
        this.strColumnName = strColumnName;
        this.strPrefix     = Util.toSpaceStrippedString(strPrefix);
        this.strExpression = ( strExp == null ? Util.gstrCodeValue : strExp );
        this.bIsIndex = bIsIndex;
        this.eNodeType = eNodeType;
    }

    static String getNODETYPE() {
        return CellResourceNode.strNODETYPE;
    }

    @Override
    public String getNodeName() {
        String strSubType = Util.toNodeTypeString(eNodeType);
        return "Cell IRI: <" + this.strPrefix + ":[" +
            ( this.bIsIndex ? "Index#" : this.strColumnName ) +
            "] (" + strSubType +
            ") on [" + this.strExpression + "]>";
    }

    @Override
    public String getNodeType() {
        return CellResourceNode.strNODETYPE;
    }

    @JsonProperty("columnName")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getColumnName() {
        return this.strColumnName;
    }

    @JsonProperty("prefix")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getPrefix() {
        return this.strPrefix;
    }

    @JsonProperty("expression")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getExpression() {
        return this.strExpression;
    }

    @Override
    protected void createRowResources() {
        if (Util.isDebugMode()) CellResourceNode.logger.info("DEBUG: createRowResources...");

        this.listNodes = null;
        Object results = null;
        try {
            results =
                Util.evaluateExpression( this.theProject, this.strExpression, this.strColumnName, this.theRec.row() );
        }
        catch (ParsingException ex) {
            // An cell might result in a ParsingException when evaluating an IRI expression.
            // Eat the exception...
            return;
        }

        // Results cannot be classed...
        if ( results == null || ExpressionUtils.isError(results) || ! ExpressionUtils.isNonBlankData(results) ) {
            return;
        }

        this.listNodes = new ArrayList<RDFNode>();

        // Results are an array...
        if ( results.getClass().isArray() ) {
            if (Util.isDebugMode()) CellResourceNode.logger.info("DEBUG: Result is Array...");
            this.processResultsAsArray(this.strPrefix, results);
        }
        // Results are singular...
        else {
            if (Util.isDebugMode()) CellResourceNode.logger.info("DEBUG: Result is Singular...");
            if ( results.toString().isEmpty() ) {
                return;
            }
            var bDone = false;
            if (this.strPrefix == null) {
                bDone = this.processResultsAsSingle(results);
            }
            if ( ! bDone ) {
                this.normalizeResource(this.strPrefix, results);
            }
        }

        if ( this.listNodes.isEmpty() ) {
            this.listNodes = null;
        }
    }

    @Override
    protected void writeNode(JsonGenerator writer, boolean isRoot)
            throws JsonGenerationException, IOException {
        // Prefix
        //      null means Raw (Full) IRI
        //      "" means baseIRI + Local Part
        if (this.strPrefix != null) {
            writer.writeStringField(Util.gstrPrefix, this.strPrefix);
        }

        // Source
        writer.writeObjectFieldStart(Util.gstrValueSource);
        String strType = Util.toNodeSourceString(this.eNodeType);
        writer.writeStringField(Util.gstrSource, strType);
        if ( ! ( this.bIsIndex || this.strColumnName == null ) ) {
            writer.writeStringField(Util.gstrColumnName, this.strColumnName);
        }
        writer.writeEndObject();

        // Expression
        if ( ! ( this.strExpression == null || this.strExpression.equals(Util.gstrCodeValue) ) ) {
            writer.writeObjectFieldStart(Util.gstrExpression);
            writer.writeStringField(Util.gstrLanguage, Util.gstrGREL);
            writer.writeStringField(Util.gstrCode, this.strExpression);
            writer.writeEndObject();
        }

        // Value Type
        if (! isRoot) {
            writer.writeObjectFieldStart(Util.gstrValueType);
            writer.writeStringField(Util.gstrType, Util.gstrIRI);
            writer.writeEndObject();
        }
    }
}
