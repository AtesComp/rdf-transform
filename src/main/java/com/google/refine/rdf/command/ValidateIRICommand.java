package com.google.refine.rdf.command;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.commands.Command;
import com.google.refine.rdf.model.Util;

import org.eclipse.rdf4j.common.net.ParsedIRI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidateIRICommand extends Command {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:ValidIRICmd");
    
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
        if ( Util.isVerbose(3) ) logger.info("Validating IRI...");
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
                //if ( Util.isDebugMode() ) logger.info("Validating IRI: Success");
            }
            catch (URISyntaxException ex) {
                if ( Util.isDebugMode() ) logger.info("Validating IRI: Failure");
            	ValidateIRICommand.respond(response, "{ \"good\" : \"0\" }");
                return;
            }
        }
		catch (Exception ex) {
            if ( Util.isDebugMode() ) logger.info("Validating IRI: ExceptionError");
            ValidateIRICommand.respondException(response, ex);
            return;
        }
        ValidateIRICommand.respond(response, "{ \"good\" : \"1\" }");
    }
}
