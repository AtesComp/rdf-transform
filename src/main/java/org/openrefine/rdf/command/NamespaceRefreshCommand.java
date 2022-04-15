package org.openrefine.rdf.command;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openrefine.rdf.RDFTransform;
import org.openrefine.rdf.model.Util;
import org.openrefine.rdf.model.vocab.VocabularyImportException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NamespaceRefreshCommand extends RDFTransformCommand {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:PfxRefreshCmd");

    public NamespaceRefreshCommand() {
        super();
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json");
        if ( ! this.hasValidCSRFToken(request) ) {
            NamespaceRefreshCommand.respondCSRFError(response);
            return;
        }
        // For Project, DO NOT USE this.getProject(request) as we only need the string...
        String strProjectID = request.getParameter(Util.gstrProject);

        String strPrefix    = request.getParameter(Util.gstrPrefix);
        String strNamespace = request.getParameter(Util.gstrNamespace);

        RDFTransform theTransform = this.getRDFTransform(request);

        // Remove the namespace...
        theTransform.removeNamespace(strPrefix);

        Exception except = null;
        boolean bError = false;
        String strError = null;
        try{
            // Remove related vocabulary...
            RDFTransform.getGlobalContext().
                getVocabularySearcher().
                    deleteTermsOfVocab(strPrefix, strProjectID);

            // Re-add related vocabulary...
            RDFTransform.getGlobalContext().
                getVocabularySearcher().
                    importAndIndexVocabulary(strPrefix, strNamespace, strNamespace, strProjectID);
        }
        catch (VocabularyImportException ex) {
            bError = true;
            strError = "Importing";
            except = ex;
        }
        catch (Exception ex) {
            bError = true;
            strError = "Processing";
            except = ex;
        }

        // Some problem occurred....
        if (except != null) {
            if (bError) {// ...error...
                NamespaceRefreshCommand.logger.error("ERROR: " + strError + " vocabulary: ", except);
                if ( Util.isVerbose() || Util.isDebugMode() ) except.printStackTrace();
            }
            else { // ...warning...
                if ( Util.isVerbose() ) NamespaceRefreshCommand.logger.warn("Prefix exists: ", except);
            }

            NamespaceRefreshCommand.respondJSON(response, CodeResponse.error);
            return;
        }

        // Otherwise, all good...

        // Re-add the namespace...
        theTransform.addNamespace(strPrefix, strNamespace);

        NamespaceRefreshCommand.respondJSON(response, CodeResponse.ok);
    }
}
