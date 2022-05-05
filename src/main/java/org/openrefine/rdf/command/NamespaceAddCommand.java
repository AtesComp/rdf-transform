package org.openrefine.rdf.command;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openrefine.rdf.RDFTransform;
import org.openrefine.rdf.model.Util;
import org.openrefine.rdf.model.vocab.VocabularyImportException;

public class NamespaceAddCommand extends RDFTransformCommand {

    public NamespaceAddCommand() {
        super();
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ( ! this.hasValidCSRFToken(request) ) {
            NamespaceAddCommand.respondCSRFError(response);
            return;
        }
        // For Project, DO NOT USE this.getProject(request) as we only need the string...
        String strProjectID = request.getParameter(Util.gstrProject);

        String strPrefix       = request.getParameter(Util.gstrPrefix).strip();
        String strNamespace    = request.getParameter(Util.gstrNamespace).strip();
        String strFetchOption  = request.getParameter("fetch").strip();

        if ( strFetchOption.equals("web") ) {
            String strFetchURL = request.getParameter("fetchURL");
            if (strFetchURL == null) {
                strFetchURL = strNamespace;
            }

            Exception except = null;
            boolean bError = false; // ...not fetchable
            boolean bFormatted = false;
            try {
                // Add related vocabulary...
                RDFTransform.getGlobalContext().
                    getVocabularySearcher().
                        importAndIndexVocabulary(
                            strPrefix, strNamespace, strFetchURL, strProjectID);
            }
            catch (VocabularyImportException ex) {
                bFormatted = true;
                except = ex;
            }
            catch (Exception ex) { // IOException
                bError = true;
                except = ex;
            }

            // Some problem occurred....
            if (except != null) {
                this.processException(except, bError, bFormatted, logger);

                NamespaceAddCommand.respondJSON(response, CodeResponse.error);
                return;
            }
        }

        // Otherwise, all good...

        // Add the namespace...
        this.getRDFTransform(request).addNamespace(strPrefix, strNamespace);

        NamespaceAddCommand.respondJSON(response, CodeResponse.ok);
    }
}
