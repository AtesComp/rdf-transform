package org.openrefine.rdf.command;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.commands.Command;

import org.openrefine.rdf.model.Util;

import org.apache.jena.iri.IRI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidateIRICommand extends Command {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:ValidIRICmd");

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ( Util.isVerbose(3) ) ValidateIRICommand.logger.info("Validating IRI...");
        // NOTE: No CSRFToken required for this command.

        try {
            //
            // Set up response...
            //   ...cause we're hand-jamming JSON responses directly...
            //
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");

            String strIRI = request.getParameter("iri").strip();

            IRI theIRI = Util.buildIRI(strIRI, true);
            if (theIRI == null) {
                if ( Util.isDebugMode() ) ValidateIRICommand.logger.error("DEBUG: Validating IRI: Failure [" + strIRI + "]");
                // TODO: Convert to CodeResponse and respondJSON(), modify caller
                ValidateIRICommand.respond(response, "{ \"good\" : 0 }");
                return;
            }
        }
        catch (Exception ex) { // ...any other exception...
            if ( Util.isDebugMode() ) ValidateIRICommand.logger.error("DEBUG: Validating IRI: Exception: " + ex.getMessage(), ex);
            ValidateIRICommand.respondException(response, ex);
            return;
        }
        if ( Util.isVerbose(3) ) ValidateIRICommand.logger.info("...IRI validated.");
        // TODO: Convert to CodeResponse and respondJSON(), modify caller
        ValidateIRICommand.respond(response, "{ \"good\" : 1 }");
    }
}
