/*
 *  Class ConstantBlankNode
 * 
 *  A Blank Node Constant class for managing constant based blank nodes in an
 *  RDF Transform.
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerationException;

import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.rdf.model.RDFNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConstantBlankNode extends ResourceNode implements ConstantNode {
    static private final Logger logger = LoggerFactory.getLogger("RDFT:ConstBlankNode");

    static private final String strNODETYPE = "blank";
    static private final String strNotLast = "[\\.]+";
    static private final String strNotFirst = "[-\\.\\u00B7\\u0300\\u036F\\u203F\\u2040]+";

    private RDFNode bnode;
    private final String strConstant;

    @JsonCreator
    public ConstantBlankNode(String strConstant) {
        // NOTE: A Constant Blank Node is a singular blank node base on the supplied constant value.
        this.strConstant = strConstant;
        this.eNodeType = Util.NodeType.CONSTANT;
        this.bnode = null; // ...create the One and Only Constant BNode later
    }

    static String getNODETYPE() {
        return ConstantBlankNode.strNODETYPE;
    }

    @Override
    public String getNodeName() {
        return "Constant BNode: <[" + this.strConstant + "]" + this.bnode.asNode().getBlankNodeLabel() + ">";
    }

    @Override
    public String getNodeType() {
        return ConstantBlankNode.strNODETYPE;
    }

    public String getConstant() {
        return this.strConstant;
    }

    @Override
    protected void createResources() {
        // For a Constant Blank Node, we only need one constant blank node resource per record,
        // so process as a row...
        this.createRowResources();
    }

    @Override
    protected void createRecordResources() {
        // NOT USED!  Just in case, process as a row...
        this.createRowResources();
    }

    @Override
    protected void createRowResources() {
        if ( Util.isDebugMode() ) ConstantBlankNode.logger.info("DEBUG: createRowResources...");

        this.listNodes = new ArrayList<RDFNode>();
        this.normalizeBNodeResource();
    }

    private void normalizeBNodeResource() {
        // If the One and Only Constant BNode has NOT been constructed...
        if (this.bnode == null) {
            //
            // Construct the One and Only Constant BNode...once...
            //

            // When there is nothing to evaluate...
            if ( strConstant == null || strConstant.isEmpty() ) {
                // ...produce a generic blank node...
                ConstantBlankNode.logger.warn("WARNING: The ConstantBlankNode constant is empty! Creating generic BNode.");
                this.bnode = new ResourceImpl( new AnonId() );
            }
            else {
                //
                // Validate the supplied constant value as a BNode ID based on Turtle limits...
                //
                String strBNodeValue = Util.toSpaceStrippedString(strConstant);
                String strBNodeValueBegin;
                do {
                    strBNodeValueBegin = strBNodeValue;
                    while ( strBNodeValue.startsWith(ResourceNode.strBNodePrefix) ) {
                        strBNodeValue = strBNodeValue.substring(2);
                    }
                    // Not First...
                    strBNodeValue = strBNodeValue.replaceFirst("^" + ConstantBlankNode.strNotFirst, "");
                    // Not Last...
                    strBNodeValue = strBNodeValue.replaceFirst(ConstantBlankNode.strNotLast + "$", "");
                    // On no change, break...
                    if ( strBNodeValueBegin.equals(strBNodeValue) )
                        break;
                    // Otherwise, something was removed so recheck...
                } while (true);

                // When there is nothing to evaluate...
                if ( strBNodeValue == null || strBNodeValue.isEmpty() ) {
                    ConstantBlankNode.logger.error("ERROR: The ConstantBlankNode constant evaluates to nothing! Creating generic BNode.");
                    this.bnode = new ResourceImpl( new AnonId() );
                }
                else {
                    if ( Util.isDebugMode() ) ConstantBlankNode.logger.info("DEBUG:  Pre-Create: " + strBNodeValue);
                    // NOTE: The prefix "_:" is auto-added by createBNode()
                    this.bnode = new ResourceImpl( new AnonId(strBNodeValue) );
                    if ( Util.isDebugMode() ) ConstantBlankNode.logger.info("DEBUG: Post-Create: " + this.bnode.toString());
                }
            }
        }
        // Otherwise, reuse as needed.

        this.listNodes.add(bnode);
    }

    @Override
    protected void writeNode(JsonGenerator writer, boolean isRoot)
            throws JsonGenerationException, IOException {
        // Prefix
        //  N/A

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

        // Value Type, Constant Value
        if (! isRoot) {
            writer.writeObjectFieldStart(Util.gstrValueType);
            writer.writeStringField(Util.gstrType, Util.gstrValueBNode);
            writer.writeEndObject();
        }
    }
}
