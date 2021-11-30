package com.google.refine.rdf.command;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.rdf.app.ApplicationContext;
import com.google.refine.util.ParsingUtilities;

import com.fasterxml.jackson.core.JsonGenerator;
//import com.fasterxml.jackson.databind.ObjectMapper;


public class SuggestPrefixIRICommand extends RDFTransformCommand{

	public SuggestPrefixIRICommand(ApplicationContext ctxt) {
		super(ctxt);
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String prefix = request.getParameter("prefix");
		String iri = this.getContext().getPrefixManager().getNamespace(prefix);
		try {
			response.setCharacterEncoding("UTF-8");
	        response.setHeader("Content-Type", "application/json");
	        Writer w = response.getWriter();
	        JsonGenerator writer = ParsingUtilities.mapper.getFactory().createGenerator(w);
            writer.writeStartObject();
            writer.writeStringField("code", "ok");
            writer.writeStringField("iri", iri);
            writer.writeEndObject();
            writer.flush();
            writer.close();
            w.flush();
            w.close();
		} catch(Exception e) {
			respondException(response, e);
		}
	}

}
