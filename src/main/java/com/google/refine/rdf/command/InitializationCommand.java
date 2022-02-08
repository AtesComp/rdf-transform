package com.google.refine.rdf.command;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.RefineServlet;
import com.google.refine.commands.Command;
import com.google.refine.rdf.ApplicationContext;
import com.google.refine.rdf.RDFTransform;
import com.google.refine.rdf.model.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitializationCommand extends Command {
	private final static Logger logger = LoggerFactory.getLogger("RDFT:AppContext");

	private ApplicationContext theContext;

	public InitializationCommand(ApplicationContext theContext) {
		this.theContext = theContext;
		RDFTransform.setGlobalContext(theContext);

	}

	@Override
	public void init(RefineServlet servlet) {
		super.init(servlet);

		// Set the RDF Transform preferences via the OpenRefine Preference Store...
		Util.setPreferencesByPreferenceStore();

		// From refine.ini (or defaults)...
		String strHost =  System.getProperty("refine.host");
		if (strHost == null)
			strHost = "localhost"; // Default
		String strIFace = System.getProperty("refine.iface");
		if (strIFace == null)
			strIFace = ""; // Default
		String strPort =  System.getProperty("refine.port");
		if (strPort == null)
			strPort = "3333"; // Default
		File fileWorkingDir = servlet.getCacheDir(RDFTransform.KEY);

		try {
			this.theContext.init(strHost, strIFace, strPort, fileWorkingDir);
		}
		catch (IOException ex) {
			InitializationCommand.logger.error("ERROR: Initialize Context: ", ex);
			if ( Util.isVerbose() || Util.isDebugMode() ) ex.printStackTrace();
		}
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		this.doGet(request, response);
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		throw new UnsupportedOperationException("This command is not meant to be called. It is just necessary for initialization.");
	}
}
