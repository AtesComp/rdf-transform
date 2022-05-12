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
            this.createRecordLiterals();
        }
        //
        // Row Mode...
        //
        else {
            // ...process on current row as set by rowNext()...
            this.theRec.setMode(nodeProperty);
            this.createRowLiterals();
        }

        this.theRec.clear();

        // Return the collected resources from the statement processing as Objects
        // to the given Property...
        return this.listNodes;
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
            if (this.listNodes != null) {
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
