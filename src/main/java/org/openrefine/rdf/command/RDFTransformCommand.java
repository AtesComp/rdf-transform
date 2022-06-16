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

    /**
     * Utility method for retrieving the CSRF token stored in either:
     * 1. the "csrf_token" parameter of the request or
     * 2. the "X-CSRF-TOKEN" in the header of the request,
     * and checking that it is valid.
     *
     * @param request
     * @return
     * @throws ServletException
     */
    protected boolean hasValidCSRFToken(HttpServletRequest request) throws ServletException {
        if ( super.hasValidCSRFToken(request) ) { // ...try to validate the normal way
            return true;
        }

        //
        // Otherwise, try the second method...
        //
        //      For form submits with file streams, we use a header parameter "X-CSRF-TOKEN".
        //
        try {
            String token = request.getHeader("X-CSRF-TOKEN");
            return token != null && RDFTransformCommand.csrfFactory.validToken(token);
        } catch (Exception e) {
            // ignore
        }
        throw new ServletException("Can't find CSRF token: missing or bad URL parameter");
    }
}
