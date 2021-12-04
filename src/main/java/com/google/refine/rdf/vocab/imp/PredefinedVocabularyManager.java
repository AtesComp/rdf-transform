package com.google.refine.rdf.vocab.imp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
//import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import com.google.refine.rdf.app.ApplicationContext;
import com.google.refine.rdf.vocab.IPredefinedVocabularyManager;
import com.google.refine.rdf.vocab.Vocabulary;
import com.google.refine.rdf.Util;

import com.google.refine.util.ParsingUtilities;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PredefinedVocabularyManager implements IPredefinedVocabularyManager{
	private final static Logger logger = LoggerFactory.getLogger("RDFT:PredefVocabMgr");

	private final static String PREDEFINED_VOCABS_FILE_NAME = "/files/predefined_vocabs";
	private final static String SAVED_VOCABS_FILE_NAME = "vocabularies_meta.json";

	private ApplicationContext applicationContext;
	private final File workingDir;
	private Map<String, Vocabulary> predefinedVocabulariesMap = new HashMap<String, Vocabulary>();

    public PredefinedVocabularyManager() {
    	this.workingDir = null;
    }

	public PredefinedVocabularyManager(ApplicationContext context, File workingDir) throws IOException {
		this.applicationContext = context;
		this.workingDir = workingDir;

		try {
			reconstructVocabulariesFromFile();
			return;
		}
		catch (FileNotFoundException ex) {
			// Existing vocabularies are not found.
			// Try adding predefined vocabularies...
		}

		try {
			addPredefinedVocabularies();
			save();
		}
		catch (Exception ex) {
			// Predefined vocabularies are not defined properly...
			//   Ignore the exception, but log it...
			if ( Util.isVerbose() ) {
				logger.warn("Predefined Vocabulary failed: ", ex);
				if ( Util.isVerbose(2) ) ex.printStackTrace();
			}
			throw ex;
		}
	}

	public Map<String,Vocabulary> getPredefinedVocabulariesMap() {
		return predefinedVocabulariesMap;
	}

	/*
	 * Private methods
	 */
	private void addPredefinedVocabularies() throws IOException {
		InputStream inStream = getPredefinedVocabularyFile();
		if (inStream == null) {
			String strError = "Predefined Vocabulary resource not found!";
			throw new IOException(strError);
		}
		BufferedReader buffReader = new BufferedReader(new InputStreamReader(inStream));
		String strLine, strPrefix = null, strNamespace = null, strFetchURL = null;

		//	Read ontology file lines...
		//  	There should be at least 2 entries per line but ideally 3:
		//			Prefix, Namespace, URL
		//		The Namespace can also serve as the URL but may not be identical in all cases.
		//		Each entry should be separated by whitespace.
		String[] astrTokens;
		while ((strLine = buffReader.readLine()) != null) {
			// Parse entries...
			astrTokens = strLine.split("\\s+");

			// Are there enough entries?
			if (astrTokens.length < 2)
				continue;
			
			// Organize entries...
			strPrefix    = astrTokens[0];
			strNamespace = astrTokens[1];
			if (astrTokens.length < 3)
				strFetchURL = strNamespace;
			else
				strFetchURL = astrTokens[2];

			// Import and Index the ontology...
			try {
				this.applicationContext.
					getVocabularySearcher().
						importAndIndexVocabulary(strPrefix, strNamespace, strFetchURL);
				this.predefinedVocabulariesMap.put(strPrefix, new Vocabulary(strPrefix, strNamespace));
			}
			catch (Exception ex) {
				// Predefined vocabularies are not defined properly...
				//   Ignore the exception, but log it...
				if ( Util.isVerbose() ) {
					logger.warn("Predefined vocabulary import failed: ", ex);
					if ( Util.isVerbose(2) ) ex.printStackTrace();
				}
			}

		}
		buffReader.close();
		applicationContext.getVocabularySearcher().update();
	}

	protected InputStream getPredefinedVocabularyFile() {
		return this.getClass().getResourceAsStream(PREDEFINED_VOCABS_FILE_NAME);
	}

	private void reconstructVocabulariesFromFile() throws IOException {
		File vocabulariesFile =  new File(this.workingDir, SAVED_VOCABS_FILE_NAME);
		if (vocabulariesFile.exists() && vocabulariesFile.length() != 0) {
			this.load();
		}
		else {
			throw new FileNotFoundException();
		}
	}

	private void save()	throws IOException {
        File tempFile = new File(this.workingDir, "vocabs.temp.json");
        try {
            saveToFile(tempFile);
        }
		catch (Exception ex) {
            logger.error("ERROR: Project metadata save failed: ", ex);
			if ( Util.isVerbose() ) ex.printStackTrace();
			return;
        }

        File file = new File(this.workingDir, SAVED_VOCABS_FILE_NAME);
        File oldFile = new File(this.workingDir, "vocabs.old.json");

        if (file.exists()) {
            file.renameTo(oldFile);
        }
        tempFile.renameTo(file);
        if (oldFile.exists()) {
            oldFile.delete();
        }
	}

	private void saveToFile(File metadataFile) throws Exception {
        Writer writer = new OutputStreamWriter(new FileOutputStream(metadataFile));
        try {
			JsonGenerator jsonWriter = ParsingUtilities.mapper.getFactory().createGenerator(writer);
            write(jsonWriter);
        }
		finally {
            writer.close();
        }
    }

    protected void load() throws IOException {
    	File vocabsFile = new File(this.workingDir, SAVED_VOCABS_FILE_NAME);
    	ObjectMapper mapper = new ObjectMapper();
		JsonNode vocabs = mapper.readTree(vocabsFile);
		JsonNode prefixes = vocabs.get("prefixes");

		for (JsonNode prefix : prefixes) {
			String name = prefix.get("name").asText();
			String strIRI = prefix.get("iri").asText();
			this.predefinedVocabulariesMap.put(name, new Vocabulary(name, strIRI));
		}
    }

    private void write(JsonGenerator writer) throws JsonGenerationException, IOException {
    	writer.writeStartObject();
    	writer.writeFieldName("prefixes");
    	writer.writeStartArray();
    	for (Vocabulary vocab : this.predefinedVocabulariesMap.values()) {
    		vocab.write(writer);
    	}
    	writer.writeEndArray();
		writer.writeEndObject();
		writer.flush();
	}
}
