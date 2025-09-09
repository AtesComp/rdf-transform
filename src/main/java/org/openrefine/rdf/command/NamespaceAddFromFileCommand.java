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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;

import org.openrefine.rdf.ApplicationContext;
import org.openrefine.rdf.RDFTransform;
import org.openrefine.rdf.model.Util;
import org.openrefine.rdf.model.vocab.Vocabulary;
import org.openrefine.rdf.model.vocab.Vocabulary.LocationType;
import org.openrefine.rdf.model.vocab.VocabularyImportException;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
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
        if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG: doPost(): Starting ontology import...");
        if ( ! this.hasValidCSRFToken(request) ) {
            NamespaceAddFromFileCommand.respondCSRFError(response);
            return;
        }

        ApplicationContext theContext = RDFTransform.getGlobalContext();

        Exception except = null;
        boolean bException = false;
        boolean bError     = false; // ...not fetchable
        boolean bFormatted = false;

        // For Project, DO NOT USE this.getProject(request) as we only need the string...
        String strProjectID = null;
        String strPrefix    = null;
        String strNamespace = null;
        String strLocation  = null;
        String strLocType   = null;
        String strFilename  = null;
        InputStream instreamFile = null;
        File dirCacheProject = null;
        boolean bSave = true;
        try {

            // ============================================================
            if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG: doPost():   Getting DiskFileItemFactory...");
            FileItemFactory factory = new DiskFileItemFactory();

            // Create a new file upload handler...
            if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG: doPost():   Getting ServletFileUpload...");
            ServletFileUpload upload = new ServletFileUpload(factory);

            // Parse the request into a file item list...
            if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG: doPost():   Parsing request into this ontology's related file items...");
            List<FileItem> items = upload.parseRequest(request);

            // Parse the file into an input stream...
            if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG: doPost():   Parsing the ontology's file items into action elements...");
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
            String strPathCache = theContext.getRDFTCacheDirectory().getPath();
            Path pathCacheProject = Path.of( strPathCache + "/" + strProjectID );
            dirCacheProject = pathCacheProject.toFile();

            // When adding, the "uploaded_file" field holds the filename and the uploaded file.
            // Then, strFilename holds the uploading filename and "instreamFile" contains the uploaded file...
            if (instreamFile != null) {
                bSave = true;
                if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG: doPost(): Uploading file:[{}/{}]", strProjectID, strFilename);
            }
            // Otherwise, when refreshing, the "uploaded_file" field is missing and the strLocation holds the
            // internally stored filename in the cache directory...
            else {
                bSave = false;
                strFilename = strLocation;
                if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG: doPost(): Refreshing from file:[{}/{}]", strProjectID, strFilename);
            }

            if ( Util.isDebugMode() ) {
                NamespaceAddFromFileCommand.logger.info(
                    "DEBUG: doPost(): Prefix:[{}] Namespace:[{}] Location:[{}] LocType:[{}] File:[{}] isFile:[{}]",
                    strPrefix, strNamespace, strLocation, strLocType, strFilename, (instreamFile != null)
                );
            }

            // When refreshing, the "uploaded_file" field is missing and the strLocation holds the internally stored filename...
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
                if (bSave) {
                    this.saveFile(theContext, strProjectID, strLocation, instreamFile); // ...instreamFile is now closed
                }
                this.parseRDFLang(strLocation, strLocType);

                if ( Util.isDebugMode() ) {
                    NamespaceAddFromFileCommand.logger.info(
                        "DEBUG: doPost(): Prefix:[{}] Namespace:[{}] Location:[{}] LocType:[{}] isFile:[{}]",
                        strPrefix, strNamespace, strLocation, strLocType, bSave
                    );
                }

                try {
                    if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG: doPost():   Getting project's RDF Transform...");
                    RDFTransform theTransform = this.getRDFTransformFromProject(strProjectID);

                    // Remove the namespace...
                    if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG: doPost():   Removing Namespace " + strPrefix);
                    theTransform.removeNamespace(strPrefix);

                    // Remove related vocabulary...
                    if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG: doPost():   Removing relate vocabulary...");
                    theContext.getVocabularySearcher().deleteVocabularyTerms(strPrefix, strProjectID);

                    // Prepare Dataset Graph for related vocabulary...
                    if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG: doPost():   Loading dataset graph with ontology file...");
                    DatasetGraph theDSGraph = DatasetGraphFactory.createTxnMem();
                    String strFilePath = theContext.getRDFTCacheDirectory().getPath() + "/" + strProjectID + "/" + strLocation;

                    // Check for existing ontology file...
                    File fileIn = new File(dirCacheProject, strFilename);
                    if ( fileIn.exists() ) {
                        if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG: doPost(): Loading from file:[{}/{}]", strProjectID, strFilename);
                    }
                    else {
                        throw new IOException("ERROR: Cannot load ontology from non-existent file [" + strProjectID + "/" + strFilename + "]!");
                    }

                    // Load Dataset Graph for related vocabulary...
                    if (this.theRDFLang != null) theDSGraph = RDFDataMgr.loadDatasetGraph(strFilePath, this.theRDFLang);
                    else                         theDSGraph = RDFDataMgr.loadDatasetGraph(strFilePath);

                    // Check Dataset Graph...
                    boolean bBad = false;
                    Iterator<Node> graphNodes = theDSGraph.listGraphNodes();
                    while (graphNodes.hasNext()) {
                        Node graphName = graphNodes.next();
                        Graph namedGraph = theDSGraph.getGraph(graphName);
                        if ( namedGraph.isEmpty() ) {
                            bBad = true;
                            break;
                        }
                    }
                    if (bBad) {
                        throw new IOException("ERROR: Dataset graph is empty!");
                    }

                    if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG: doPost():   Dataset graph loaded.");

                    // (Re)Add related vocabulary...
                    if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG: doPost():   Importing vocabulary from dataset graph...");
                    theContext.getVocabularySearcher().importAndIndexVocabulary(strPrefix, strNamespace, strLocation, theDSGraph, strProjectID);

                    // (Re)Add the namespace...
                    if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG: doPost():   Adding Namespace " + strPrefix);
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

        // Otherwise, all good...
        NamespaceAddFromFileCommand.respondJSON(response, CodeResponse.ok);

        if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG: doPost(): ...Ended ontology import.");
    }

    /*
     * Method saveFile(File fileRDFTCacheDirectory, String strProjectID, String strFilename, StringReader strreaderFile)
     *
     *  Save an ontology file to local storage for later use/reuse
     *      fileRDFTCacheDirectory: the directory for the ontology file
     *      strProjectID: the project's identifier (used as a directory name)
     *      strFilename: the ontology file name
     *      strreaderFile: the ontology file contents
     */
    boolean saveFile(ApplicationContext theContext, String strProjectID, String strFilename, InputStream instreamFile) {
        synchronized(strFilename) {
            String strPathCache = theContext.getRDFTCacheDirectory().getPath();
            File fileNew = null;
            File fileOld = null;
            try {
                Path pathCacheProject = Path.of( strPathCache + "/" + strProjectID );
                File dirCacheProject = pathCacheProject.toFile();
                if ( ! dirCacheProject.exists() ) {
                    dirCacheProject = Files.createDirectories(pathCacheProject).toFile();
                    // If the path still doesn't exist...
                    if ( ! dirCacheProject.exists() ) {
                        NamespaceAddFromFileCommand.logger.error( "ERROR: File Error: Cannot find/create directory [{}]!", dirCacheProject.getPath() );
                        return false;
                    }
                    else if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info( "DEBUG: saveFile(): Directory exists:[{}]", dirCacheProject.getPath() );
                }

                // Get the Files...
                fileNew = new File(dirCacheProject, strFilename);
                fileOld = new File(dirCacheProject, strFilename + ".old");

                // Process the Files...
                if ( fileNew.createNewFile() ) {
                    if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG: saveFile(): File created:[{}/{}]", strProjectID, strFilename);
                }
                else { // fileNew exists...
                    if ( fileOld.exists() ) {
                        if ( ! fileOld.delete() ) {
                            NamespaceAddFromFileCommand.logger.error("ERROR: Could not remove OLD archived ontology file [{}{}]!", strProjectID, strFilename);
                            return false;
                        }
                    }
                    if ( ! fileNew.renameTo(fileOld) ) {
                        NamespaceAddFromFileCommand.logger.error("ERROR: Could not archive ontology file [{}{}]!", strProjectID, strFilename);
                        return false;
                    }
                    if ( fileNew.createNewFile() ) {
                        if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG: saveFile(): File overwrite:[{}/{}]", strProjectID, strFilename);
                    }
                    else {
                        NamespaceAddFromFileCommand.logger.error("ERROR: File could not be overwritten:[{}/{}]", strProjectID, strFilename);
                        return false;
                    }
                }
            }
            catch (Exception ex) {
                NamespaceAddFromFileCommand.logger.error("ERROR: File Error on [{}/{}]", strProjectID, strFilename);
                NamespaceAddFromFileCommand.logger.error( "ERROR: File Error: {}", ex.getMessage() );
                return false;
            }

            // Write the file...
            try {
                Writer writer = new OutputStreamWriter( new FileOutputStream(fileNew) );
                IOUtils.copy(instreamFile, writer, "UTF-8");
                instreamFile.close();
                writer.close();

                NamespaceAddFromFileCommand.logger.info("INFO: Successfully wrote File:[{}/{}]", strProjectID, strFilename);
            }
            catch (Exception ex) {
                NamespaceAddFromFileCommand.logger.error("ERROR: File Write Error on [{}/{}]", strProjectID, strFilename);
                return false;
            }

            // Clean up archive...
            if ( fileOld.exists() ) {
                if ( ! fileOld.delete() ) {
                    NamespaceAddFromFileCommand.logger.error("ERROR: Could not remove archived ontology file [{}/{}]!", strProjectID, strFilename);
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
