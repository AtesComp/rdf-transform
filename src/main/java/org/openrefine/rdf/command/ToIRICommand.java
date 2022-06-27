package org.openrefine.rdf.command;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.commands.Command;

import org.openrefine.rdf.model.expr.functions.ToIRIString;
import org.openrefine.rdf.model.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToIRICommand extends Command {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:ToIRICmd");

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ( Util.isVerbose(3) ) ToIRICommand.logger.info("Creating IRI from string...");
        // NOTE: No CSRFToken required for this command.

        String strIRI = null;
        try {
            //
            // Set up response...
            //   ...cause we're hand-jamming JSON responses directly...
            //
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");

            String strConvert = request.getParameter("convert").strip();

            strIRI = ToIRIString.toIRIString(strConvert);
            if (strIRI == null) {
                if ( Util.isDebugMode() ) ToIRICommand.logger.error("DEBUG: Creating IRI: Failure [" + strConvert + "]");
                // TODO: Convert to CodeResponse and respondJSON(), modify caller
                ToIRICommand.respond(response, "{ \"good\" : 0 }");
                return;
            }
        }
        catch (Exception ex) { // ...any other exception...
            if ( Util.isDebugMode() ) ToIRICommand.logger.error("DEBUG: Creating IRI: Exception: " + ex.getMessage(), ex);
            ToIRICommand.respondException(response, ex);
            return;
        }
        if ( Util.isVerbose(3) ) ToIRICommand.logger.info("...IRI created.");
        // TODO: Convert to CodeResponse and respondJSON(), modify caller
        ToIRICommand.respond(response, "{ \"good\" : 1, \"iri\" : \"" + strIRI + "\" }");
    }
}
