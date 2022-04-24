package org.openrefine.rdf.command;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openrefine.rdf.RDFTransform;
import org.openrefine.rdf.model.Util;
import org.openrefine.rdf.model.vocab.Vocabulary;
import org.openrefine.rdf.model.vocab.VocabularyImportException;
import org.openrefine.rdf.model.vocab.VocabularyList;
import com.google.refine.util.ParsingUtilities;

import com.fasterxml.jackson.core.JsonGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NamespacesGetDefaultCommand extends RDFTransformCommand {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:NSGetDefaultCmd");

    public NamespacesGetDefaultCommand() {
        super();
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ( Util.isDebugMode() ) NamespacesGetDefaultCommand.logger.info("DEBUG: Getting default namespaces...");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json");
        try {
            this.getDefaultNamespaces(request, response);
        }
        catch (Exception ex) {
            NamespacesGetDefaultCommand.respondJSON(response, CodeResponse.error);
        }
    }

    private void getDefaultNamespaces(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String strProjectID = request.getParameter(Util.gstrProject);

        //
        // Get vocabularies...
        //
        VocabularyList listVocabs = this.getRDFTransform(request).getNamespaces();
        if ( Util.isDebugMode() ) NamespacesGetDefaultCommand.logger.info("DEBUG: Existing namespaces: size=" + listVocabs.size());
        if ( listVocabs == null || listVocabs.isEmpty() ) {
            listVocabs =
                RDFTransform.getGlobalContext().
                    getPredefinedVocabularyManager().
                        getPredefinedVocabularies().clone();
            if ( Util.isDebugMode() ) NamespacesGetDefaultCommand.logger.info("DEBUG: Predefined namespaces: size=" + listVocabs.size());
        }

        //
        // Set up response...
        //
        Writer writerBase = response.getWriter();
        JsonGenerator theWriter = ParsingUtilities.mapper.getFactory().createGenerator(writerBase);

        theWriter.writeStartObject();
        theWriter.writeObjectFieldStart(Util.gstrNamespaces);

        //
        // Load vocabularies for vocabulary searcher and respond each namespace...
        //
        for (Vocabulary vocab : listVocabs) {
            if ( Util.isDebugMode() ) NamespacesGetDefaultCommand.logger.info("  Prefix: " + vocab.getPrefix() + "  Namespace: " + vocab.getNamespace());
            Exception except = null;
            boolean bError = false;
            String strError = null;
            try {
                RDFTransform.getGlobalContext().
                    getVocabularySearcher().
                        importAndIndexVocabulary(
                            vocab.getPrefix(), vocab.getNamespace(), vocab.getNamespace(), strProjectID);
            }
            catch (VocabularyImportException ex) {
                bError = true;
                strError = "Importing";
                except = ex;
            }
            catch (Exception ex) { // ...all other exceptions...
                bError = true;
                strError = "Processing";
                except = ex;
            }

            // Some problem occurred....
            if (except != null) {
                // A Default Prefix vocabulary is not defined properly...
                //   Ignore the exception, but log it...
                if (bError) {// ...error...
                    NamespacesGetDefaultCommand.logger.error("ERROR: " + strError + " vocabulary: ", except);
                    if ( Util.isVerbose() || Util.isDebugMode() ) except.printStackTrace();
                }
                else { // ...warning...
                    if ( Util.isVerbose() || Util.isDebugMode() ) NamespacesGetDefaultCommand.logger.warn("Prefix exists: ", except);
                }
                // ...continue processing the other vocabularies...
            }

            theWriter.writeStringField( vocab.getPrefix(), vocab.getNamespace() );
        }

        //
        // Finish response...
        //
        theWriter.writeEndObject();
        theWriter.writeEndObject();

        theWriter.flush();
        theWriter.close();
        writerBase.flush(); // ...commit response
        writerBase.close();
    }
}
