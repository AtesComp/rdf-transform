package org.openrefine.rdf.command;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.commands.Command;
import org.openrefine.rdf.model.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreferencesCommand extends Command {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:PrefCmd");

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ( Util.isVerbose(3) ) PreferencesCommand.logger.info("Setting and Getting preferences...");
        // NOTE: No CSRFToken required for this command.

        String strPreferences = "{ \"good\" : 0 }";
        try {
            //
            // Set up response...
            //   ...cause we're hand-jamming JSON responses directly...
            //
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");

            // // Set Preview Stream if given...
            // String strPreviewStream = request.getParameter("bPreviewStream");
            // if (strPreviewStream != null) {
            //     Util.setPreviewStream( Boolean.parseBoolean(strPreviewStream) );
            // }

            // // Set Sample Limit if given...
            // String strSampleLimit = request.getParameter("iSampleLimit");
            // if (strSampleLimit != null) {
            //     Util.setSampleLimit( Integer.parseInt(strSampleLimit) );
            // }

            // Get Preferences...
            strPreferences =
                "{ \"good\" : 1, " +
                  "\"iVerbosity\" : " +     Util.getVerbose() + ", " +
                  "\"iExportLimit\" : " +   Util.getExportLimit() + ", " +
                  "\"bPreviewStream\" : " + Util.isPreviewStream() + ", " +
                  "\"bDebugMode\" : " +     Util.isDebugMode() + ", " +
                  "\"bDebugJSON\" : " +     Util.isDebugJSON() + ", " +
                  "\"iSampleLimit\" : " +   Util.getSampleLimit() + "" +
                  " }";
        }
        catch (Exception ex) { // ...any other exception...
            if ( Util.isDebugMode() ) PreferencesCommand.logger.error("DEBUG: Preferences: Exception: " + ex.getMessage(), ex);
            PreferencesCommand.respondException(response, ex);
            return;
        }
        if ( Util.isVerbose(3) ) PreferencesCommand.logger.info("...got preferences.");
        PreferencesCommand.respond(response, strPreferences);
    }
}
