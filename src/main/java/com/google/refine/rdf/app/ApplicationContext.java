package com.google.refine.rdf.app;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.google.refine.rdf.Util;
import com.google.refine.rdf.vocab.IPredefinedVocabularyManager;
import com.google.refine.rdf.vocab.IVocabularySearcher;
import com.google.refine.rdf.vocab.PrefixManager;
import com.google.refine.rdf.vocab.imp.PredefinedVocabularyManager;
import com.google.refine.rdf.vocab.imp.VocabularySearcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationContext {
	private final static Logger logger = LoggerFactory.getLogger("RDFT:AppContext");

	private final static String CURATED_VOCABS_FILE_NAME = "/files/prefixes";
	private final static String DEFAULT_SITE = "http://localhost:3333/";

	private String strPort;
	private String strHost;
	private String strIFace;
	private File fileWorkingDir;
	private IPredefinedVocabularyManager predefinedVocabularyManager;
	private IVocabularySearcher vocabularySearcher;
	private PrefixManager prefixManager;

	public IPredefinedVocabularyManager getPredefinedVocabularyManager() {
		return predefinedVocabularyManager;
	}

	public IVocabularySearcher getVocabularySearcher() {
		return vocabularySearcher;
	}

    protected void init(String strHost, String strIFace, String strPort, File fileWorkingDir) throws IOException {
		this.strHost =  (strHost  == null || strHost  == "") ? null : strHost;
		this.strIFace = (strIFace == null || strIFace == "") ? null : strIFace;
		this.strPort =  (strPort  == null || strPort  == "") ? null : strPort;
		this.fileWorkingDir = fileWorkingDir;
		logger.info( "Init: Host=" + ( this.strHost  == null ? "<undef>" : this.strHost ) + ", " +
                          "IFace=" + ( this.strIFace == null ? "<undef>" : this.strIFace ) + ", " +
                           "Port=" + ( this.strPort  == null ? "<undef>" : this.strPort ) );

		this.vocabularySearcher = new VocabularySearcher(this.fileWorkingDir);
		this.predefinedVocabularyManager = new PredefinedVocabularyManager(this, this.fileWorkingDir);
		InputStream inStream = this.getClass().getResourceAsStream(CURATED_VOCABS_FILE_NAME);
		this.prefixManager = new PrefixManager(inStream);
		if (Util.isVerbose(3))
			logger.info("Init: Completed");
	}

	public void setPredefinedVocabularyManager(IPredefinedVocabularyManager predefinedVocabularyManager) {
		this.predefinedVocabularyManager = predefinedVocabularyManager;
	}

	public void setVocabularySearcher(IVocabularySearcher vocabularySearcher) {
		this.vocabularySearcher = vocabularySearcher;
	}

	public PrefixManager getPrefixManager() {
		return prefixManager;
	}

	public String getDefaultBaseIRI() {
		if (this.strHost.isEmpty() && this.strIFace.isEmpty())
			return DEFAULT_SITE; // Default
		String strIRI =
			"http://" +
			( this.strHost.isEmpty() ? this.strIFace : this.strHost ) +
			( this.strPort.isEmpty() ? "" : ":" + this.strPort ) +
			"/";
		return strIRI;
	}
}
