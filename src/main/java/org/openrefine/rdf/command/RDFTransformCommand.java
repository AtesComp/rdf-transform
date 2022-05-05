package org.openrefine.rdf.command;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.openrefine.rdf.RDFTransform;
import org.openrefine.rdf.model.Util;
import com.google.refine.commands.Command;
import com.google.refine.model.Project;

import org.slf4j.Logger;

public abstract class RDFTransformCommand extends Command {

    public RDFTransformCommand() {
        super();
    }

    public RDFTransform getRDFTransform(HttpServletRequest request)
            throws ServletException {
        // Reset the RDF Transform preferences via the OpenRefine Preference Store
        // as it may have changed since last call...
        Util.setPreferencesByPreferenceStore();
        try {
            Project theProject = this.getProject(request);
            return RDFTransform.getRDFTransform(theProject);
        }
        catch (ServletException ex) {
            throw new ServletException("Unable to retrieve Project!", ex);
        }
        catch (Exception ex) {
            throw new ServletException("Unable to retrieve Project or RDF Transform! (Other)", ex);
        }
    }

    public void processException(Exception except, boolean bError, boolean bFormatted, Logger logger) {
        if (except == null) {
            return;
        }

        String strMsg =  except.getMessage();
        if (bError) { // ...error...
            if (!bFormatted) {
                strMsg = "ERROR: " + strMsg;
            }
            if (logger != null) logger.error(strMsg, except);
            if ( Util.isVerbose() || Util.isDebugMode() ) except.printStackTrace();
        }
        else { // ...warning...
            if (!bFormatted) {
                strMsg = "WARNING: " + strMsg;
            }
            if ( logger != null && (Util.isVerbose() || Util.isDebugMode() ) ) logger.warn(strMsg, except);
        }
    }
}
