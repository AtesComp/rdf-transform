/*
 *  Class NamespaceAddFromFileCommand
 *
 *  Adds a Namespace to the current RDF Transform including importing the
 *  ontology terms to the Lucene indexer.
 *
 *  Copyright 2025 Keven L. Ates
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
import java.io.InputStream;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import org.openrefine.rdf.RDFTransform;
import org.openrefine.rdf.model.Util;

//import org.apache.jena.rdf.model.Model;
//import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.core.DatasetGraph;

import com.google.refine.ProjectManager;
import com.google.refine.model.Project;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NamespaceAddFromFileCommand extends RDFTransformCommand {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:NSAddFromFileCmd");

    String strPrefix;
    String strNamespace;
    Lang theRDFLang;
    String strProjectID;
    String strFilename;
    InputStream instreamFile;

    public NamespaceAddFromFileCommand() {
        super();

        this.strProjectID = null;
        this.strPrefix = null;
        this.strNamespace = null;
        this.theRDFLang = null;
        this.strFilename = "";
        this.instreamFile = null;
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG: Starting ontology import...");
        if ( ! this.hasValidCSRFToken(request) ) {
            NamespaceAddFromFileCommand.respondCSRFError(response);
            return;
        }
        if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG:   Getting DiskFileItemFactory...");
        FileItemFactory factory = new DiskFileItemFactory();
        RDFTransform theTransform = null;

        try {
            // Create a new file upload handler...
            if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG:   Getting ServletFileUpload...");
            ServletFileUpload upload = new ServletFileUpload(factory);

            // Parse the request into a file item list...
            if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG:   Parsing request into this ontology's related file items...");
            List<FileItem> items = upload.parseRequest(request);

            // Parse the file into an input stream...
            if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG:   Parsing the ontology's file items into action elements...");
            this.parseUploadItems(items);

            if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG:   Getting project's RDF Transform...");
            theTransform = getProjectTransform();

            //if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG:   Creating dataset graph for this ontology file...");
            // NOTE: use createOntologyModel() to do ontology include processing.
            //      createDefaultModel() just processes the given file without including.
            //Model theModel = ModelFactory.createDefaultModel();

            if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG:   Reading dataset graph from ontology file...");
            DatasetGraph theDSGraph = null;
            if (this.theRDFLang != null) {
                //theModel.read( this.instreamFile, "", this.theRDFLang.getName() );
                theDSGraph = RDFDataMgr.loadDatasetGraph(strFilename, theRDFLang);
            }
            else {
                //theModel.read( this.instreamFile, "" ); // ...assumes the concrete syntax is RDF/XML
                theDSGraph = RDFDataMgr.loadDatasetGraph(strFilename);
            }

            if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG:   Importing ontology vocabulary from dataset graph...");
            RDFTransform.getGlobalContext().
                getVocabularySearcher().
                    importAndIndexVocabulary(this.strPrefix, this.strNamespace, theDSGraph, this.strProjectID);
        }
        catch (Exception ex) {
            NamespaceAddFromFileCommand.logger.error("ERROR: " + ex.getMessage(), ex);
            NamespaceAddFromFileCommand.respondJSON(response, CodeResponse.error);
            return;
        }

        // Otherwise, all good...

        // Add the namespace...
        if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG:   Adding Namespace...");
        theTransform.addNamespace(this.strPrefix, this.strNamespace);

        NamespaceAddFromFileCommand.respondJSON(response, CodeResponse.ok);

        if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG: ...Ended ontology import.");
    }

    private void parseUploadItems(List<FileItem> items)
            throws IOException {
        String strFormat = null;
        // Get the form values by attribute names...
        //      NOTE: See file "rdf-transform-prefix-add.html" for matching keys.
        for (FileItem item : items) {
            String strItemName = item.getFieldName();
            if (strItemName.equals("vocab_prefix")) {
                this.strPrefix = item.getString();
            }
            else if (strItemName.equals("vocab_namespace")) {
                this.strNamespace = item.getString();
            }
            else if (strItemName.equals("vocab_project")) {
                this.strProjectID = item.getString();
            }
            else if (strItemName.equals("file_format")) {
                strFormat = item.getString();
            }
            else { // if (strItemName.equals("uploaded_file"))
                this.strFilename = item.getName();
                this.instreamFile = item.getInputStream();
            }
        }

        this.theRDFLang = Lang.RDFXML;
        if (strFormat != null) {
            // Check the "file_format" key's value to set RDF language...
            //      NOTE: See file "rdf-transform-prefix-add.html" for matching key values.
            if (strFormat.equals("auto-detect")) {
                this.theRDFLang = this.guessFormat(strFilename);
            }
            else if (strFormat.equals("RDF/XML")) {
                this.theRDFLang = Lang.RDFXML;
            }
            else if (strFormat.equals("TTL")) {
                this.theRDFLang = Lang.TURTLE;
            }
            else if (strFormat.equals("N3")) {
                this.theRDFLang = Lang.N3;
            }
            else if (strFormat.equals("NTRIPLE")) {
                this.theRDFLang = Lang.NTRIPLES;
            }
            else if (strFormat.equals("JSON-LD")) {
                this.theRDFLang = Lang.JSONLD;
            }
            else if (strFormat.equals("NQUADS")) {
                this.theRDFLang = Lang.NQUADS;
            }
            else if (strFormat.equals("RDF/JSON")) {
                this.theRDFLang = Lang.RDFJSON;
            }
            else if (strFormat.equals("TRIG")) {
                this.theRDFLang = Lang.TRIG;
            }
            else if (strFormat.equals("TRIX")) {
                this.theRDFLang = Lang.TRIX;
            }
            else if (strFormat.equals("RDFTHRIFT")) {
                this.theRDFLang = Lang.RDFTHRIFT;
            }
        }
    }

    private Lang guessFormat(String strFilename) {
        Lang theLang = Lang.RDFXML; // ...default for unknown extension
        if (strFilename.lastIndexOf('.') != -1) {
            String strExtension = strFilename.substring(strFilename.lastIndexOf('.') + 1).toLowerCase();
            if (strExtension.equals("rdf")) {
                theLang = Lang.RDFXML;
            }
            else if (strExtension.equals("rdfs")) {
                theLang = Lang.RDFXML;
            }
            else if (strExtension.equals("owl")) {
                theLang = Lang.RDFXML;
            }
            else if (strExtension.equals("ttl")) {
                theLang = Lang.TURTLE;
            }
            else if (strExtension.equals("n3")) {
                theLang = Lang.N3;
            }
            else if (strExtension.equals("nt")) {
                theLang = Lang.NTRIPLES;
            }
            else if (strExtension.equals("jsonld")) {
                theLang = Lang.JSONLD;
            }
            else if (strExtension.equals("nq")) {
                theLang = Lang.NQUADS;
            }
            else if (strExtension.equals("rj")) {
                theLang = Lang.RDFJSON;
            }
            else if (strExtension.equals("trig")) {
                theLang = Lang.TRIG;
            }
            else if (strExtension.equals("trix")) {
                theLang = Lang.TRIX;
            }
            else if (strExtension.equals("xml")) {
                theLang = Lang.TRIX;
            }
            else if (strExtension.equals("trdf")) {
                theLang = Lang.RDFTHRIFT;
            }
            else if (strExtension.equals("rt")) {
                theLang = Lang.RDFTHRIFT;
            }
        }
        return theLang;
    }

    private RDFTransform getProjectTransform()
            throws ServletException { // ...just because
        Project theProject = null;

        if ( ! ( this.strProjectID == null || this.strProjectID.isEmpty() ) ) {
            Long liProjectID;
            try {
                liProjectID = Long.parseLong(this.strProjectID);
            }
            catch (NumberFormatException ex) {
                throw new ServletException("Project ID not a long int!", ex);
            }

            theProject = ProjectManager.singleton.getProject(liProjectID);
        }

        if (theProject == null) {
            throw new ServletException("Project ID [" + strProjectID + "] not found! May be corrupt.");
        }

        RDFTransform theTransform = RDFTransform.getRDFTransform(theProject);
        if (theTransform == null) {
            throw new ServletException("RDF Transform for Project ID [" + strProjectID + "] not found!");
        }
        return theTransform;
    }
}
