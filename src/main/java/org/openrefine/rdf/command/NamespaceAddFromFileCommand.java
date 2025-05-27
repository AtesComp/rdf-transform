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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import org.openrefine.rdf.ApplicationContext;
import org.openrefine.rdf.RDFTransform;
import org.openrefine.rdf.model.Util;
import org.openrefine.rdf.model.vocab.Vocabulary;
import org.openrefine.rdf.model.vocab.Vocabulary.LocationType;
import org.openrefine.rdf.model.vocab.VocabularyImportException;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.core.DatasetGraph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NamespaceAddFromFileCommand extends RDFTransformCommand {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:NamespaceAddFromFileCmd");

    Lang theRDFLang;
    LocationType theLocType;

    public NamespaceAddFromFileCommand() {
        super();

        this.theRDFLang = null;
        this.theLocType = LocationType.NONE;
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG: doPost: Starting ontology import...");
        if ( ! this.hasValidCSRFToken(request) ) {
            NamespaceAddFromFileCommand.respondCSRFError(response);
            return;
        }

        ApplicationContext theContext = RDFTransform.getGlobalContext();

        Exception except = null;
        boolean bException = false;
        boolean bError = false; // ...not fetchable
        boolean bFormatted = false;

        // For Project, DO NOT USE this.getProject(request) as we only need the string...
        String strProjectID = null;
        String strPrefix    = null;
        String strNamespace = null;
        String strLocation  = null;
        String strLocType   = null;
        StringReader strreaderFile = null;
        Boolean bSave = true;
        try {
            InputStream instreamFile = null;

            // ============================================================
            if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG: doPost:   Getting DiskFileItemFactory...");
            FileItemFactory factory = new DiskFileItemFactory();

            // Create a new file upload handler...
            if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG: doPost:   Getting ServletFileUpload...");
            ServletFileUpload upload = new ServletFileUpload(factory);

            // Parse the request into a file item list...
            if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG: doPost:   Parsing request into this ontology's related file items...");
            List<FileItem> items = upload.parseRequest(request);

            // Parse the file into an input stream...
            if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG: doPost:   Parsing the ontology's file items into action elements...");
            String strFilename  = null;
            for (FileItem item : items) {
                if      ( item.getFieldName().equals(Util.gstrProject) )   strProjectID = item.getString();
                else if ( item.getFieldName().equals(Util.gstrPrefix) )    strPrefix    = item.getString();
                else if ( item.getFieldName().equals(Util.gstrNamespace) ) strNamespace = item.getString();
                else if ( item.getFieldName().equals(Util.gstrLocation) )  strLocation  = item.getString();
                else if ( item.getFieldName().equals(Util.gstrLocType) )   strLocType   = item.getString();
                else if ( item.getFieldName().equals("uploaded_file") ) {
                    strFilename  = item.getName();
                    instreamFile = item.getInputStream();
                }
            }
            // ============================================================

            if (instreamFile != null) {
                byte[] bytes = instreamFile.readAllBytes();
                //strFileContent = new String(bytes, StandardCharsets.UTF_8);
                strreaderFile = new StringReader( new String(bytes) );
                instreamFile.close();
                bSave = true;
            }
            else {
                strFilename = strLocation;
                File fileIn = new File(theContext.getWorkingDirectory(), strFilename);
                if ( fileIn.exists() ) {
                    FileInputStream fileInStream = new FileInputStream(fileIn);
                    byte[] bytes = fileInStream.readAllBytes();
                    strreaderFile = new StringReader( new String(bytes) );
                    fileInStream.close();
                    bSave = false;
                }
            }

            if ( Util.isDebugMode() ) {
                NamespaceAddFromFileCommand.logger.info(
                    "DEBUG: doPost: Prefix:[{}] Namespace:[{}] Location:[{}] LocType:[{}] File:[{}] isFile:[{}]",
                    strPrefix, strNamespace, strLocation, strLocType, strFilename, (strreaderFile != null)
                );
            }

            // When refreshing, the "uploaded_file" field is missing and the strLocation holds the entire file path...
            if (strFilename == null) strFilename = strLocation;
            // Otherwise, the "uploaded_file" field holds the entire file path...
            else strLocation = strFilename;
        }
        catch (Exception ex) {
            // Some problem occurred....
            bError = true;
            except = ex;
            bException = true;
        }

        if ( ! bException ) {
            LocationType theLocType = Vocabulary.fromLocTypeString(strLocType);

            if (strLocation == null) strLocation = "";
            if ( strLocation.isEmpty() ) theLocType = LocationType.NONE;

            if (theLocType != LocationType.NONE && theLocType != LocationType.URL) { // ...anything else is a file type
                if (bSave) this.saveFile(theContext.getWorkingDirectory(), strLocation, strreaderFile);
                this.parseRDFLang(strLocation, strLocType);

                if ( Util.isDebugMode() ) {
                    NamespaceAddFromFileCommand.logger.info(
                        "DEBUG: doPost: Prefix:[{}] Namespace:[{}] Location:[{}] LocType:[{}] isFile:[{}]",
                        strPrefix, strNamespace, strLocation, strLocType, (strreaderFile != null)
                    );
                }

                try {
                    if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG: doPost:   Getting project's RDF Transform...");
                    RDFTransform theTransform = this.getRDFTransformFromProject(strProjectID);

                    // Remove the namespace...
                    if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG: doPost:   Removing Namespace " + strPrefix);
                    theTransform.removeNamespace(strPrefix);

                    // Remove related vocabulary...
                    if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG: doPost:   Removing relate vocabulary...");
                    theContext.
                        getVocabularySearcher().
                            deleteVocabularyTerms(strPrefix, strProjectID);

                    // Load Dataset Graph for related vocabulary...
                    if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG: doPost:   Loading dataset graph from ontology file...");
                    DatasetGraph theDSGraph = DatasetGraphFactory.createTxnMem();
                    if (strreaderFile != null) {
                        if (this.theRDFLang != null) {
                            //theDSGraph = RDFDataMgr.loadDatasetGraph(strLocation, this.theRDFLang);
                            RDFDataMgr.read(theDSGraph, strreaderFile, strNamespace, this.theRDFLang);
                        }
                        else {
                            //theDSGraph = RDFDataMgr.loadDatasetGraph(strLocation);
                            RDFDataMgr.read(theDSGraph, strreaderFile, strNamespace, Lang.RDFXML);
                        }
                    }

                    // (Re)Add related vocabulary...
                    if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG: doPost:   Importing vocabulary from dataset graph...");
                    theContext.
                        getVocabularySearcher().
                            importAndIndexVocabulary(strPrefix, strNamespace, strLocation, theDSGraph, strProjectID);

                    // (Re)Add the namespace...
                    if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG: doPost:   Adding Namespace " + strPrefix);
                    if (theTransform != null) theTransform.addNamespace(strPrefix, strNamespace, strLocation, this.theLocType);
                }
                catch (VocabularyImportException ex) {
                    bFormatted = true;
                    except = ex;
                    bException = true;
                }
                catch (Exception ex) {
                    bError = true;
                    except = ex;
                    bException = true;
                }
            }
        }

        // If some problem occurred....
        if (bException) {
            this.processException(except, bError, bFormatted, logger);
            NamespaceAddFromFileCommand.respondJSON(response, CodeResponse.error);
            return;
        }

        NamespaceAddFromFileCommand.respondJSON(response, CodeResponse.ok);

        if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG: doPost: ...Ended ontology import.");
    }

    /*
     * Method saveFile(File fileWorkingDirectory, String strFilename, StringReader strreaderFile)
     *
     *  Save an ontology file to local storage for later use/reuse
     *      fileWorkingDirectory: the file for he parent directory
     *      strFilename: the ontology file name
     *      strreaderFile: the ontology file contents
     */
    Boolean saveFile(File fileWorkingDirectory, String strFilename, StringReader strreaderFile) {
        synchronized(strFilename) {
            // Get the Files...
            File fileNew = new File(fileWorkingDirectory, strFilename);
            File fileOld = new File(fileWorkingDirectory, strFilename + ".old");
            try {
                // Get the File Stream...
                if ( fileNew.createNewFile() ) NamespaceAddFromFileCommand.logger.info("saveFile: File created:[{}]", strFilename);
                else {
                    fileNew.renameTo(fileOld);
                    if ( fileNew.createNewFile() ) NamespaceAddFromFileCommand.logger.info("saveFile: File overwrite:[{}]", strFilename);
                    else {
                        NamespaceAddFromFileCommand.logger.error("saveFile: File could not be created:[{}]", strFilename);
                        return false;
                    }
                }
            }
            catch (IOException ex) {
                NamespaceAddFromFileCommand.logger.error("saveFile: File Error on [{}]", strFilename);
                return false;
            }

            // Write the file...
            try {
                Writer writer = new OutputStreamWriter( new FileOutputStream(fileNew) );
                strreaderFile.transferTo(writer);
                writer.close();
                NamespaceAddFromFileCommand.logger.info("saveFile: Successfully wrote File:[{}]", strFilename);
            }
            catch (IOException ex) {
                NamespaceAddFromFileCommand.logger.error("saveFile: File Write Error on [{}]", strFilename);
                return false;
            }

            if ( fileOld.exists() ) {
                if ( ! fileOld.delete() ) {
                    NamespaceAddFromFileCommand.logger.error("ERROR: Could not remove archived ontology file!");
                    return false;
                }
            }
            return true;
        }
    }

    private void parseRDFLang(String strLocation, String strLocType) {
        //this.theRDFLang = null;
        this.theLocType = LocationType.FILE;
        if (strLocType != null) {
            // Check the strLocType value to set RDF language...
            //      NOTE: See file "rdf-transform-prefix-add.html".

            if ( strLocType.equals("auto-detect") || strLocType.equals( LocationType.FILE.toString() ) ) {
                this.guessFormat(strLocation);
            }
            else if ( strLocType.equals("RDF/XML") || strLocType.equals( LocationType.RDF_XML.toString() ) ) {
                this.theRDFLang = Lang.RDFXML;
                this.theLocType = LocationType.RDF_XML;
            }
            else if ( strLocType.equals( LocationType.TTL.toString() ) ) {
                this.theRDFLang = Lang.TURTLE;
                this.theLocType = LocationType.TTL;
            }
            else if (strLocType.equals( LocationType.N3.toString() ) ) {
                this.theRDFLang = Lang.N3;
                this.theLocType = LocationType.N3;
            }
            else if ( strLocType.equals( LocationType.NTRIPLE.toString() ) ) {
                this.theRDFLang = Lang.NTRIPLES;
                this.theLocType = LocationType.NTRIPLE;
            }
            else if ( strLocType.equals("JSON-LD") || strLocType.equals( LocationType.JSON_LD.toString() ) ) { // default version is JSON-LD 1.1
                this.theRDFLang = Lang.JSONLD;
                this.theLocType = LocationType.JSON_LD;
            }
            else if ( strLocType.equals( LocationType.NQUADS.toString() ) ) {
                this.theRDFLang = Lang.NQUADS;
                this.theLocType = LocationType.NQUADS;
            }
            else if ( strLocType.equals("RDF/JSON") || strLocType.equals( LocationType.RDF_JSON.toString() ) ) {
                this.theRDFLang = Lang.RDFJSON;
                this.theLocType = LocationType.RDF_JSON;
            }
            else if ( strLocType.equals( LocationType.TRIG.toString() ) ) {
                this.theRDFLang = Lang.TRIG;
                this.theLocType = LocationType.TRIG;
            }
            else if ( strLocType.equals( LocationType.TRIX.toString() ) ) {
                this.theRDFLang = Lang.TRIX;
                this.theLocType = LocationType.TRIX;
            }
            else if ( strLocType.equals( LocationType.RDFTHRIFT.toString() ) ) {
                this.theRDFLang = Lang.RDFTHRIFT;
                this.theLocType = LocationType.RDFTHRIFT;
            }
        }
    }

    private void guessFormat(String strLocation) {
        if (strLocation.lastIndexOf('.') != -1) {
            String strExtension = strLocation.substring(strLocation.lastIndexOf('.') + 1).toLowerCase();
            if (strExtension.equals("rdf")) {
                this.theRDFLang = Lang.RDFXML;
                this.theLocType = LocationType.RDF_XML;
            }
            else if (strExtension.equals("rdfs")) {
                this.theRDFLang = Lang.RDFXML;
                this.theLocType = LocationType.RDF_XML;
            }
            else if (strExtension.equals("owl")) {
                this.theRDFLang = Lang.RDFXML;
                this.theLocType = LocationType.RDF_XML;
            }
            else if (strExtension.equals("ttl")) {
                this.theRDFLang = Lang.TURTLE;
                this.theLocType = LocationType.TTL;
            }
            else if (strExtension.equals("n3")) {
                this.theRDFLang = Lang.N3;
                this.theLocType = LocationType.N3;
            }
            else if (strExtension.equals("nt")) {
                this.theRDFLang = Lang.NTRIPLES;
                this.theLocType = LocationType.NTRIPLE;
            }
            else if (strExtension.equals("jsonld")) { // default version is JSON-LD 1.1
                this.theRDFLang = Lang.JSONLD;
                this.theLocType = LocationType.JSON_LD;
            }
            else if (strExtension.equals("nq")) {
                this.theRDFLang = Lang.NQUADS;
                this.theLocType = LocationType.NQUADS;
            }
            else if (strExtension.equals("rj")) {
                this.theRDFLang = Lang.RDFJSON;
                this.theLocType = LocationType.RDF_JSON;
            }
            else if (strExtension.equals("trig")) {
                this.theRDFLang = Lang.TRIG;
                this.theLocType = LocationType.TRIG;
            }
            else if (strExtension.equals("trix")) {
                this.theRDFLang = Lang.TRIX;
                this.theLocType = LocationType.TRIX;
            }
            else if (strExtension.equals("xml")) {
                this.theRDFLang = Lang.TRIX;
                this.theLocType = LocationType.TRIX;
            }
            else if (strExtension.equals("trdf")) {
                this.theRDFLang = Lang.RDFTHRIFT;
                this.theLocType = LocationType.RDFTHRIFT;
            }
            else if (strExtension.equals("rt")) {
                this.theRDFLang = Lang.RDFTHRIFT;
                this.theLocType = LocationType.RDFTHRIFT;
            }
        }
    }
}
