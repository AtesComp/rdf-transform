package org.openrefine.rdf.command;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openrefine.rdf.RDFTransform;
import org.openrefine.rdf.model.Util;
import com.google.refine.util.ParsingUtilities;

import com.fasterxml.jackson.core.JsonGenerator;
//import com.fasterxml.jackson.databind.ObjectMapper;


public class SuggestNamespaceCommand extends RDFTransformCommand {

    public SuggestNamespaceCommand() {
        super();
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String strPrefix = request.getParameter(Util.gstrPrefix);
        String strNamespace = RDFTransform.getGlobalContext().getNSManager().getNamespace(strPrefix);

        try {
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");

            Writer writerBase = response.getWriter();
            JsonGenerator theWriter = ParsingUtilities.mapper.getFactory().createGenerator(writerBase);

            theWriter.writeStartObject();
            theWriter.writeStringField("code", "ok");
            theWriter.writeStringField(Util.gstrNamespace, strNamespace);
            theWriter.writeEndObject();

            theWriter.flush();
            theWriter.close();
            writerBase.close();
        }
        catch (Exception ex) {
            respondException(response, ex);
        }
    }

}
