package com.google.refine.rdf.command;

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
import com.google.refine.rdf.RDFTransform;
import com.google.refine.rdf.app.ApplicationContext;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencer;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import com.google.refine.ProjectManager;
import com.google.refine.model.Project;

public class AddPrefixFromFileCommand extends RDFTransformCommand {

    public AddPrefixFromFileCommand(ApplicationContext context) {
        super(context);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if(!hasValidCSRFTokenAsHeader(request)) {
            respondCSRFError(response);
            return;
        }
        try {
            FileItemFactory factory = new DiskFileItemFactory();
            // Create a new file upload handler
            ServletFileUpload upload = new ServletFileUpload(factory);
            String
                strPrefix = null,
                strNamespace = null,
                strFormat = null,
                strProjectID = null,
                strFilename = "";
            InputStream instreamFile = null;
            List<FileItem> items = upload.parseRequest(request);
            for (FileItem item : items) {
                if (item.getFieldName().equals("vocab-prefix")) {
                    strPrefix = item.getString();
                }
                else if (item.getFieldName().equals("vocab-iri")) {
                    strNamespace = item.getString();
                }
                else if (item.getFieldName().equals("file_format")) {
                    strFormat = item.getString();
                }
                else if (item.getFieldName().equals("project")) {
                    strProjectID = item.getString();
                }
                else {
                    strFilename = item.getName();
                    instreamFile = item.getInputStream();
                }
            }

            Repository theRepository =
				new SailRepository(
					new SchemaCachingRDFSInferencer( new MemoryStore() )
				);
            theRepository.init();
            RepositoryConnection theRepoConnection = theRepository.getConnection();
            RDFFormat theRDFFormat = RDFFormat.TURTLE;
            if (strFormat != null) {
                if (strFormat.equals("auto-detect")) {
                    theRDFFormat = guessFormat(strFilename);
                }
                else if (strFormat.equals("RDFXML")) {
                    theRDFFormat = RDFFormat.RDFXML;
                }
                else if (strFormat.equals("TTL")) {
                    theRDFFormat = RDFFormat.TURTLE;
                }
                else if (strFormat.equals("N3")) {
                    theRDFFormat = RDFFormat.N3;
                }
                else if (strFormat.equals("NTRIPLE")) {
                    theRDFFormat = RDFFormat.NTRIPLES;
                }
                else if (strFormat.equals("JSONLD")) {
                    theRDFFormat = RDFFormat.JSONLD;
                }
                else if (strFormat.equals("NQUADS")) {
                    theRDFFormat = RDFFormat.NQUADS;
                }
                else if (strFormat.equals("RDFJSON")) {
                    theRDFFormat = RDFFormat.RDFJSON;
                }
                else if (strFormat.equals("TRIG")) {
                    theRDFFormat = RDFFormat.TRIG;
                }
                else if (strFormat.equals("TRIX")) {
                    theRDFFormat = RDFFormat.TRIX;
                }
                else if (strFormat.equals("BINARY")) {
                    theRDFFormat = RDFFormat.BINARY;
                }
            }
            theRepoConnection.add(instreamFile, "", theRDFFormat);
            theRepoConnection.close();

            Project theProject = ProjectManager.singleton.getProject(Long.parseLong(strProjectID));
            RDFTransform theTransform = RDFTransform.getRDFTransform(getContext(), theProject);
            theTransform.addPrefix(strPrefix, strNamespace);
            this.getContext().
                getVocabularySearcher().
                    importAndIndexVocabulary(strPrefix, strNamespace, theRepository, strProjectID);

            respondJSON(response, CodeResponse.ok);
        }
        //catch (IOException ex) {
        //    respondException(response, ex);
        //}
        //catch (org.eclipse.rdf4j.RDF4JException ex) {
        //    respondException(response, ex);
        //}
        catch (Exception ex) {
            respondException(response, ex);
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
        return RDFFormat.RDFXML;
    }

}
