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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openrefine.rdf.RDFTransform;
import org.openrefine.rdf.model.Util;
import org.openrefine.rdf.model.vocab.Vocabulary.LocationType;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.core.DatasetGraph;

import com.google.refine.ProjectManager;
import com.google.refine.model.Project;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NamespaceAddFromFileCommand extends RDFTransformCommand {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:NamespaceAddFromFileCmd");

    static private RDFTransform getProjectTransform(String strProjID)
            throws ServletException { // ...just because
        Project theProject = null;

        if ( ! ( strProjID == null || strProjID.isEmpty() ) ) {
            Long liProjectID;
            try {
                liProjectID = Long.parseLong(strProjID);
            }
            catch (NumberFormatException ex) {
                throw new ServletException("Project ID not a long int!", ex);
            }

            theProject = ProjectManager.singleton.getProject(liProjectID);
        }

        if (theProject == null) {
            throw new ServletException("Project ID [" + strProjID + "] not found! May be corrupt.");
        }

        RDFTransform theTransform = RDFTransform.getRDFTransform(theProject);
        if (theTransform == null) {
            throw new ServletException("RDF Transform for Project ID [" + strProjID + "] not found!");
        }
        return theTransform;
    }

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
        if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG: Starting ontology import...");
        if ( ! this.hasValidCSRFToken(request) ) {
            NamespaceAddFromFileCommand.respondCSRFError(response);
            return;
        }

        // For Project, DO NOT USE this.getProject(request) as we only need the string...
        String strProjectID = request.getParameter(Util.gstrProject);

        String strPrefix    = request.getParameter(Util.gstrPrefix);
        String strNamespace = request.getParameter(Util.gstrNamespace);
        String strLocation  = request.getParameter(Util.gstrLocation);
        String strLocType   = request.getParameter(Util.gstrLocType);
        if (strLocation == null) strLocation = "";
        if ( strLocation.isEmpty() ) {
            theLocType = LocationType.NONE;
        }
        else {
            RDFTransform theTransform = null;
            this.parseRDFLang(strLocation, strLocType);
            try {
                if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG:   Getting project's RDF Transform...");
                theTransform = NamespaceAddFromFileCommand.getProjectTransform(strProjectID);

                // Remove the namespace...
                theTransform.removeNamespace(strPrefix);

                if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG:   Reading dataset graph from ontology file...");
                DatasetGraph theDSGraph = null;
                if (this.theRDFLang != null) {
                    theDSGraph = RDFDataMgr.loadDatasetGraph(strLocation, this.theRDFLang);
                }
                else {
                    theDSGraph = RDFDataMgr.loadDatasetGraph(strLocation);
                }

                if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG:   Importing ontology vocabulary from dataset graph...");
                RDFTransform.getGlobalContext().
                    getVocabularySearcher().
                        importAndIndexVocabulary(strPrefix, strNamespace, strLocation, theDSGraph, strProjectID);
            }
            catch (Exception ex) {
                NamespaceAddFromFileCommand.logger.error("ERROR: " + ex.getMessage(), ex);
                NamespaceAddFromFileCommand.respondJSON(response, CodeResponse.error);
                return;
            }

            // Add the namespace...
            if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG:   Adding Namespace...");
            if (theTransform != null) theTransform.addNamespace(strPrefix, strNamespace, strLocation, this.theLocType);
        }

        NamespaceAddFromFileCommand.respondJSON(response, CodeResponse.ok);

        if ( Util.isDebugMode() ) NamespaceAddFromFileCommand.logger.info("DEBUG: ...Ended ontology import.");
    }

    private void parseRDFLang(String strLocType, String strLocation) {
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
