/*
 *  Class LiteralNode
 *
 *  The Literal Node base class use by other Literal based nodes in an RDF
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerationException;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.datatypes.DatatypeFormatException;
import org.apache.jena.rdf.model.impl.LiteralImpl;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.graph.NodeFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract public class LiteralNode extends Node {
    static private final Logger logger = LoggerFactory.getLogger("RDFT:LitNode");

    protected ConstantResourceNode nodeDatatype;
    protected String strLanguage;

    @JsonProperty("datatype")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getDatatype() {
        return this.nodeDatatype.normalizeResourceAsString();
    }

    @JsonProperty("language")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getLanguage() {
        return this.strLanguage;
    }

    /*
     *  Method createObjects() creates the object list for triple statements
     *  from this node on Rows / Records.
     */
    @Override
    protected List<RDFNode> createObjects(ResourceNode nodeProperty) {
        if (Util.isDebugMode()) LiteralNode.logger.info("DEBUG: createObjects...");

        this.setObjectParameters(nodeProperty);

        // TODO: Create process for Sub-Records

        this.listNodes = null;

        //
        // Record Mode...
        //
        if ( nodeProperty.theRec.isRecordMode() ) { // ...property is Record based,
            // ...set to Row Mode and process on current row as set by rowNext()...
            this.theRec.setMode(nodeProperty, true);
        }
        //
        // Row Mode...
        //
        else {
            // ...process on current row as set by rowNext()...
            this.theRec.setMode(nodeProperty);
        }

        this.createStatementsWorker();
        this.theRec.clear();

        // Return the collected resources from the statement processing as Objects
        // to the given Property...
        return this.listNodes;
    }

    /*
     *  Method createStatementsWorker() for Literal Node types
     *
     *  Return: void
     *
     *  Stores the Literal as generic Values since these are "object" elements in
     *    ( source, predicate, object ) triples and need to be compatible with resources.
     */
    private void createStatementsWorker() {
        if (Util.isDebugMode()) logger.info("DEBUG: createStatementsWorker...");

        //
        // Transition from Record to Row processing...
        //
        if ( this.theRec.isRecordPerRow() ) {
            List<RDFNode> listLiteralsAll = new ArrayList<RDFNode>();
            while ( this.theRec.rowNext() ) {
                this.createRowLiterals(); // ...Row only
                if ( ! ( this.listNodes == null || this.listNodes.isEmpty() ) ) {
                    listLiteralsAll.addAll(this.listNodes);
                }
            }
            if ( listLiteralsAll.isEmpty() ) {
                listLiteralsAll = null;
            }
            this.listNodes = listLiteralsAll;
        }

        //
        // Standard Record or Row processing...
        //
        else {
            this.createLiterals(); // ...Record or Row
            if ( this.listNodes == null || this.listNodes.isEmpty() ) {
                this.listNodes = null;
            }
        }
    }

    /*
     *  Method createLiterals() for Literal Node types
     *
     *  Return: void
     *
     *  Stores the Literals as generic Values since these are "object" elements in
     *    ( source, predicate, object ) triples and need to be compatible with resources.
     */
    protected void createLiterals() {
        if (Util.isDebugMode()) logger.info("DEBUG: createLiterals...");

        // TODO: Create process for Sub-Records

        //
        // Record Mode
        //
        if ( this.theRec.isRecordMode() ) {
            // If a column node, the node should iterate all records in the Record group...
            if ( ! this.bIsIndex ) {
                this.createRecordLiterals();
            }
            // Otherwise, we only need to get a single "Record Number" literal for the Record group...
            else {
                this.theRec.rowNext(); // ...set index for first (or any) row in the Record
                this.createRowLiterals(); // ...get the one resource
                this.theRec.rowReset(); // ...reset for any other row run on the Record
            }
        }
        //
        // Row Mode
        //
        else {
            this.createRowLiterals();
        }
    }

    /*
     *  Method createRecordLiterals() creates the object list for triple statements
     *  from this node on Records
     */
    protected void createRecordLiterals() {
        if (Util.isDebugMode()) LiteralNode.logger.info("DEBUG: createRecordLiterals...");

        List<RDFNode> listLiterals = new ArrayList<RDFNode>();
        while ( this.theRec.rowNext() ) {
            this.createRowLiterals();
            if ( this.listNodes != null ) {
                listLiterals.addAll(this.listNodes);
            }
        }
        if ( listLiterals.isEmpty() ) {
            listLiterals = null;
        }

        this.listNodes = listLiterals;
    }

    abstract protected void createRowLiterals();

    /*
     *  Method normalizeLiteral() for Literal Node to Literal string
     */
    protected void normalizeLiteral(Object obj) {
        String strResult = obj.toString();
        if ( strResult == null || strResult.isEmpty() ) {
            return;
        }
        if ( Util.isDebugMode() ) LiteralNode.logger.info("DEBUG: normalizeLiteral: Result: " + strResult);

        //
        // Process each string as a Literal with the following preference:
        //    1. a given Datatype
        //    2. a given Language code
        //    3. nothing, just a simple string Literal
        //
        Literal literal = null;

        // If there is a datatype...
        if (this.nodeDatatype != null) {
            String strDatatype = this.nodeDatatype.normalizeResourceAsString();
            String strExpandedDatatype = this.expandPrefixedIRI(strDatatype);
            if ( strExpandedDatatype != null) {
                strDatatype = strExpandedDatatype;
            }
            RDFDatatype theDatatype = TypeMapper.getInstance().getSafeTypeByName(strDatatype);
            try {
                literal = new LiteralImpl( NodeFactory.createLiteral(strResult, theDatatype), null );
            }
            catch (DatatypeFormatException ex) {
                LiteralNode.logger.info("ERROR: normalizeLiteral: Datatype not valid: " + strResult + " ^^ " + strDatatype);
            }
        }
        // Else, if there is a language...
        else if (this.strLanguage != null) {
            literal = new LiteralImpl( NodeFactory.createLiteral(strResult, this.strLanguage), null );
        }
        // Otherwise...
        else {
            // ...don't decorate the value...
            literal = new LiteralImpl( NodeFactory.createLiteral(strResult), null );
        }

        // If there is a valid literal...
        if (literal != null) {
            this.listNodes.add(literal);
        }
    }

    abstract protected void writeNode(JsonGenerator writer)
            throws JsonGenerationException, IOException;

    public void write(JsonGenerator writer, /* unused */ boolean isRoot)
            throws JsonGenerationException, IOException {
        writer.writeStartObject();

        // Write node...
        this.writeNode(writer);

        writer.writeEndObject();
    }

}
