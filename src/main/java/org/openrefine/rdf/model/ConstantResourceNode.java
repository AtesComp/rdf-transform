/*
 *  Class ConstantResourceNode
 *
 *  A Resource Node Constant class for managing constant based Resource nodes
 *  in an RDF Transform.
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

import java.io.IOException;
import java.util.ArrayList;

import org.apache.jena.rdf.model.RDFNode;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConstantResourceNode extends ResourceNode implements ConstantNode {
    static private final Logger logger = LoggerFactory.getLogger("RDFT:ConstResNode");

    static private final String strNODETYPE = "resource";

    private final String strConstant;
    private final String strPrefix;

    @JsonCreator
    public ConstantResourceNode(String strConstant, String strPrefix) {
        // A Constant Resource Node is a singular IRI...
        this.strConstant = Util.toSpaceStrippedString(strConstant);
        this.strPrefix = Util.toSpaceStrippedString(strPrefix);
        this.eNodeType = Util.NodeType.CONSTANT;
    }

    static String getNODETYPE() {
        return ConstantResourceNode.strNODETYPE;
    }

    @Override
    public String getNodeName() {
        return "Constant IRI: <" + this.strPrefix + ":[" + this.strConstant +  "]>";
    }

    @Override
    public String getNodeType() {
        return ConstantResourceNode.strNODETYPE;
    }

    public String getConstant() {
        return this.strConstant;
    }

    public String getPrefix() {
        return this.strPrefix;
    }

    @Override
    protected void createResources() {
        // For a Constant Resource Node, we only need one constant resource per record,
        // so process as a row...
        this.createRowResources();
    }

    @Override
    protected void createRecordResources() {
        // NOT USED!
        this.listNodes = null;
    }

    @Override
    protected void createRowResources() {
        this.listNodes = null;

        // If there is no value to work with...
        if ( this.strConstant == null || this.strConstant.isEmpty() ) {
            return;
        }

        this.listNodes = new ArrayList<RDFNode>();
        var bDone = false;
        if (this.strPrefix == null) {
            bDone = this.processResultsAsSingle(this.strConstant);
        }
        if ( ! bDone ) {
            this.normalizeResource(this.strPrefix, this.strConstant);
        }

        if ( this.listNodes.isEmpty() ) {
            this.listNodes = null;
        }
    }

    /*
     *  Method normalizeResourceAsString() for Resource Node to IRI
     */
    public String normalizeResourceAsString() {
        String strIRI = "";
        if ( this.strConstant == null || this.strConstant.isEmpty() ) {
            return strIRI;
        }
        strIRI = this.strConstant; // ...Full IRI
        if ( this.strPrefix != null ) {
            strIRI = this.strPrefix + ":" + this.strConstant; // ...CIRIE
        }
        if ( Util.isDebugMode() ) {
            String strDebug = "DEBUG: normalizeResource: Given: ";
            if (this.strPrefix == null) {
                strDebug += "IRI: " + strIRI;
            }
            else {
                strDebug += "Prefix: " + this.strPrefix + " LocalPart: " + this.strConstant;
            }
            ConstantResourceNode.logger.info(strDebug);
        }

        try {
            Util.resolveIRI(this.baseIRI, strIRI);
        }
        catch (Exception ex) {
            // An IRIParsingException from Util.resolveIRI() means a bad IRI.
            // An IllegalArgumentException from theFactory.createIRI() means a bad IRI.
            // In either case, record error and eat the exception...
            ConstantResourceNode.logger.error("ERROR: Bad IRI: " + strIRI, ex);
        }
        return strIRI;
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
        writer.writeStringField(Util.gstrSource, Util.gstrConstant);
        writer.writeStringField(Util.gstrConstant, this.strConstant);
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
