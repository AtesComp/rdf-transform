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

public class PreferencesCommand extends Command {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:PrefCmd");

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ( Util.isVerbose(3) ) PreferencesCommand.logger.info("Setting and Getting preferences...");
        // NOTE: No CSRFToken required for this command.

        String strPreferences = "{ \"good\" : \"0\" }";
        try {
            //
            // Set up response...
            //   ...cause we're hand-jamming JSON responses directly...
            //
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");

            // Set Preview Stream if given...
            boolean bPreviewStream = Util.isPreviewStream(); // ...get
            String strPreviewStream = request.getParameter("PreviewStream");
            if (strPreviewStream != null) {
                bPreviewStream = Boolean.parseBoolean(strPreviewStream);
                Util.setPreviewStream(bPreviewStream);
            }

            // Set Sample Limit if given...
            int iSampleLimit = Util.getSampleLimit(); // ...get
            String strSampleLimit = request.getParameter("SampleLimit");
            if (strSampleLimit != null) {
                iSampleLimit = Integer.parseInt(strSampleLimit);
                Util.setSampleLimit(iSampleLimit);
            }

            // Get Preferences...
            strPreferences =
                "{ \"good\" : \"1\", " +
                  "\"Verbosity\" : \"" +     Util.getVerbose() + "\", " +
                  "\"ExportLimit\" : \"" +   Util.getExportLimit() + "\", " +
                  "\"PreviewStream\" : \"" + bPreviewStream + "\", " +
                  "\"DebugMode\" : \"" +     Util.isDebugMode() + "\", " +
                  "\"DebugJSON\" : \"" +     Util.isDebugJSON() + "\", " +
                  "\"SampleLimit\" : \"" +   iSampleLimit + "\"" +
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
