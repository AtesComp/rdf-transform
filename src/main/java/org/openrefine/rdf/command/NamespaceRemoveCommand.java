package org.openrefine.rdf.command;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openrefine.rdf.RDFTransform;
import org.openrefine.rdf.model.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NamespaceRemoveCommand extends RDFTransformCommand {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:PfxRemoveCmd");

    public NamespaceRemoveCommand() {
        super();
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ( Util.isDebugMode() ) NamespaceRemoveCommand.logger.info("Removing prefix...");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json");
        if ( ! this.hasValidCSRFToken(request) ) {
            NamespaceRemoveCommand.respondCSRFError(response);
            return;
        }
        // For Project, DO NOT USE this.getProject(request) as we only need the string...
        String strProjectID = request.getParameter(Util.gstrProject);

        String strPrefix = request.getParameter(Util.gstrPrefix);

        if ( ! this.getRDFTransform(request).removeNamespace(strPrefix) ) {
            if ( Util.isDebugMode() ) NamespaceRemoveCommand.logger.info("...failed.");
            NamespaceRemoveCommand.respondJSON(response, CodeResponse.error);
            return;
        }

        try {
            RDFTransform.getGlobalContext().
                getVocabularySearcher().
                    deleteTermsOfVocab(strPrefix, strProjectID);
        }
        catch (Exception ex) {
            if ( Util.isDebugMode() ) NamespaceRemoveCommand.logger.info("...vocabulary removal problems...");
        }

        if ( Util.isDebugMode() ) NamespaceRemoveCommand.logger.info("...removed.");
        NamespaceRemoveCommand.respondJSON(response, CodeResponse.ok);
    }
}
