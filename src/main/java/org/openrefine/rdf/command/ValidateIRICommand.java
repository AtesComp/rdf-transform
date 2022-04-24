package org.openrefine.rdf.command;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.commands.Command;
import org.openrefine.rdf.model.Util;

import org.eclipse.rdf4j.common.net.ParsedIRI;

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

            try {
                new ParsedIRI(strIRI);
            }
            catch (URISyntaxException ex) {
                if ( Util.isDebugMode() ) ValidateIRICommand.logger.error("DEBUG: Validating IRI: Failure [" + strIRI + "]", ex);
                ValidateIRICommand.respond(response, "{ \"good\" : \"0\" }");
                return;
            }
        }
        catch (Exception ex) { // ...any other exception...
            if ( Util.isDebugMode() ) ValidateIRICommand.logger.error("DEBUG: Validating IRI: ExceptionError", ex);
            ValidateIRICommand.respondException(response, ex);
            return;
        }
        if ( Util.isVerbose(3) ) ValidateIRICommand.logger.info("...IRI validated.");
        ValidateIRICommand.respond(response, "{ \"good\" : \"1\" }");
    }
}
