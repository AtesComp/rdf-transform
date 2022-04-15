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

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencer;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import com.google.refine.ProjectManager;
import com.google.refine.model.Project;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NamespaceAddFromFileCommand extends RDFTransformCommand {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:PfxAddFromFileCmd");

    String strPrefix;
    String strNamespace;
    RDFFormat theRDFFormat;
    String strProjectID;
    String strFilename;
    InputStream instreamFile;

    public NamespaceAddFromFileCommand() {
        super();

        this.strProjectID = null;
        this.strPrefix = null;
        this.strNamespace = null;
        this.theRDFFormat = null;
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

        Repository theRepository = null;
        try {
            // Create a new file upload handler...
            ServletFileUpload upload = new ServletFileUpload(factory);

            // Get the parameters...
            List<FileItem> items = upload.parseRequest(request);
            this.parseUploadItems(items);

            theRepository =
                new SailRepository(
                    new SchemaCachingRDFSInferencer( new MemoryStore() )
                );

            RepositoryConnection theRepoConnection = theRepository.getConnection();
            theRepoConnection.add(this.instreamFile, "", this.theRDFFormat);
            theRepoConnection.close();
        }
        catch (Exception ex) {
            NamespaceAddFromFileCommand.respondJSON(response, CodeResponse.error);
            return;
        }

        try {
            this.getTransform(theProject).addNamespace(this.strPrefix, this.strNamespace);

            RDFTransform.getGlobalContext().
                getVocabularySearcher().
                    importAndIndexVocabulary(this.strPrefix, this.strNamespace, theRepository, this.strProjectID);
        }
        catch (Exception ex) {
            NamespaceAddFromFileCommand.logger.error("ERROR: " + ex.getMessage(), ex);
            NamespaceAddFromFileCommand.respondJSON(response, CodeResponse.error);
            return;
        }

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

        this.theRDFFormat = RDFFormat.TURTLE;
        if (strFormat != null) {
            //
            // NOTE: See file "rdf-transform-prefix-add.html" for
            //      matching values.
            //
            if (strFormat.equals("auto-detect")) {
                this.theRDFFormat = this.guessFormat(strFilename);
            }
            else if (strFormat.equals("RDF/XML")) {
                this.theRDFFormat = RDFFormat.RDFXML;
            }
            else if (strFormat.equals("TTL")) {
                this.theRDFFormat = RDFFormat.TURTLE;
            }
            else if (strFormat.equals("N3")) {
                this.theRDFFormat = RDFFormat.N3;
            }
            else if (strFormat.equals("NTRIPLE")) {
                this.theRDFFormat = RDFFormat.NTRIPLES;
            }
            else if (strFormat.equals("JSON-LD")) {
                this.theRDFFormat = RDFFormat.JSONLD;
            }
            else if (strFormat.equals("NQUADS")) {
                this.theRDFFormat = RDFFormat.NQUADS;
            }
            else if (strFormat.equals("RDF/JSON")) {
                this.theRDFFormat = RDFFormat.RDFJSON;
            }
            else if (strFormat.equals("TRIG")) {
                this.theRDFFormat = RDFFormat.TRIG;
            }
            else if (strFormat.equals("TRIX")) {
                this.theRDFFormat = RDFFormat.TRIX;
            }
            else if (strFormat.equals("BINARY")) {
                this.theRDFFormat = RDFFormat.BINARY;
            }
        }
    }

    private RDFFormat guessFormat(String strFilename) {
        if (strFilename.lastIndexOf('.') != -1) {
            String strExtension = strFilename.substring(strFilename.lastIndexOf('.') + 1).toLowerCase();
            if (strExtension.equals("rdf")) {
                return RDFFormat.RDFXML;
            }
            else if (strExtension.equals("rdfs")) {
                return RDFFormat.RDFXML;
            }
            else if (strExtension.equals("owl")) {
                return RDFFormat.RDFXML;
            }
            else if (strExtension.equals("ttl")) {
                return RDFFormat.TURTLE;
            }
            else if (strExtension.equals("n3")) {
                return RDFFormat.N3;
            }
            else if (strExtension.equals("nt")) {
                return RDFFormat.NTRIPLES;
            }
            else if (strExtension.equals("jsonld")) {
                return RDFFormat.JSONLD;
            }
            else if (strExtension.equals("nq")) {
                return RDFFormat.NQUADS;
            }
            else if (strExtension.equals("rj")) {
                return RDFFormat.RDFJSON;
            }
            else if (strExtension.equals("trig")) {
                return RDFFormat.TRIG;
            }
            else if (strExtension.equals("trix")) {
                return RDFFormat.TRIX;
            }
            else if (strExtension.equals("xml")) {
                return RDFFormat.TRIX;
            }
            else if (strExtension.equals("brf")) {
                return RDFFormat.BINARY;
            }
        }
        return RDFFormat.TURTLE;
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
