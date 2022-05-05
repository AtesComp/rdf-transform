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

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;

import com.google.refine.ProjectManager;
import com.google.refine.model.Project;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NamespaceAddFromFileCommand extends RDFTransformCommand {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:PfxAddFromFileCmd");

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
        if ( ! this.hasValidCSRFToken(request) ) {
            NamespaceAddFromFileCommand.respondCSRFError(response);
            return;
        }
        Project theProject = this.getProject(request);
        FileItemFactory factory = new DiskFileItemFactory();

        try {
            // Create a new file upload handler...
            ServletFileUpload upload = new ServletFileUpload(factory);

            // Get the parameters...
            List<FileItem> items = upload.parseRequest(request);
            this.parseUploadItems(items);

            // NOTE: use createOntologyModel() to do ontology include processing.
            //      createDefaultModel() just processes the given file without incluing.
            Model theModel = ModelFactory.createDefaultModel();
            if (this.theRDFLang != null) {
                theModel.read(this.instreamFile, "", this.theRDFLang.getName());
            }
            else {
                theModel.read(this.instreamFile, "");
            }

            RDFTransform.getGlobalContext().
                getVocabularySearcher().
                    importAndIndexVocabulary(this.strPrefix, this.strNamespace, theModel, this.strProjectID);
        }
        catch (Exception ex) {
            NamespaceAddFromFileCommand.logger.error("ERROR: " + ex.getMessage(), ex);
            NamespaceAddFromFileCommand.respondJSON(response, CodeResponse.error);
            return;
        }

        // Otherwise, all good...

        // Add the namespace...
        this.getTransform(theProject).addNamespace(this.strPrefix, this.strNamespace);

        NamespaceAddFromFileCommand.respondJSON(response, CodeResponse.ok);
    }

    private void parseUploadItems(List<FileItem> items)
            throws IOException {
        String strFormat = null;
        // Get the form values by name attributes...
        for (FileItem item : items) {
            if (item.getFieldName().equals("vocab_prefix")) {
                this.strPrefix = item.getString();
            }
            else if (item.getFieldName().equals("vocab_namespace")) {
                this.strNamespace = item.getString();
            }
            else if (item.getFieldName().equals("vocab_project")) {
                this.strProjectID = item.getString();
            }
            else if (item.getFieldName().equals("file_format")) {
                strFormat = item.getString();
            }
            else {
                this.strFilename = item.getName();
                this.instreamFile = item.getInputStream();
            }
        }

        this.theRDFLang = Lang.TURTLE;
        if (strFormat != null) {
            //
            // NOTE: See file "rdf-transform-prefix-add.html" for
            //      matching values.
            //
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
            else if (strFormat.equals("BINARY")) {
                this.theRDFLang = Lang.RDFTHRIFT;
            }
        }
    }

    private Lang guessFormat(String strFilename) {
        if (strFilename.lastIndexOf('.') != -1) {
            String strExtension = strFilename.substring(strFilename.lastIndexOf('.') + 1).toLowerCase();
            if (strExtension.equals("rdf")) {
                return Lang.RDFXML;
            }
            else if (strExtension.equals("rdfs")) {
                return Lang.RDFXML;
            }
            else if (strExtension.equals("owl")) {
                return Lang.RDFXML;
            }
            else if (strExtension.equals("ttl")) {
                return Lang.TURTLE;
            }
            else if (strExtension.equals("n3")) {
                return Lang.N3;
            }
            else if (strExtension.equals("nt")) {
                return Lang.NTRIPLES;
            }
            else if (strExtension.equals("jsonld")) {
                return Lang.JSONLD;
            }
            else if (strExtension.equals("nq")) {
                return Lang.NQUADS;
            }
            else if (strExtension.equals("rj")) {
                return Lang.RDFJSON;
            }
            else if (strExtension.equals("trig")) {
                return Lang.TRIG;
            }
            else if (strExtension.equals("trix")) {
                return Lang.TRIX;
            }
            else if (strExtension.equals("xml")) {
                return Lang.TRIX;
            }
            else if (strExtension.equals("trdf")) {
                return Lang.RDFTHRIFT;
            }
            else if (strExtension.equals("rt")) {
                return Lang.RDFTHRIFT;
            }
        }
        return Lang.TURTLE;
    }

    private RDFTransform getTransform(Project theDefaultProject)
            throws ServletException { // ...just because
        Project theProject = theDefaultProject;

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

        RDFTransform transform = RDFTransform.getRDFTransform(theProject);
        if (transform == null) {
            throw new ServletException("RDF Transform for Project ID [" + strProjectID + "] not found!");
        }
        return transform;
    }
}
