package org.openrefine.rdf.model.vocab;

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
import java.util.Iterator;
import java.util.Map.Entry;

import org.openrefine.rdf.ApplicationContext;
import org.openrefine.rdf.model.Util;
import com.google.refine.util.ParsingUtilities;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PredefinedVocabularyManager implements IPredefinedVocabularyManager{
    private final static Logger logger = LoggerFactory.getLogger("RDFT:PredefVocabMgr");

    private final static String PREDEFINED_VOCABS_FILE_NAME = "/files/PredefinedVocabs";
    private final static String SAVED_VOCABS_FILE_NAME = "VocabulariesMeta.json";

    private ApplicationContext context;
    private final File workingDir;
    private VocabularyList predefinedVocabularies = new VocabularyList();

    public PredefinedVocabularyManager() {
        this.workingDir = null;
    }

    public PredefinedVocabularyManager(ApplicationContext context, File workingDir) throws IOException {
        this.context = context;
        this.workingDir = workingDir;

        if ( Util.isDebugMode() ) PredefinedVocabularyManager.logger.info("Attempting vocabulary reconstruct...");
        try {
            this.reconstructVocabulariesFromFile();
            if ( predefinedVocabularies.isEmpty() ) {
                throw new FileNotFoundException();
            }
        }
        catch (FileNotFoundException ex1) {
            // Existing vocabularies are not found.
            // Try adding predefined vocabularies...
            if ( Util.isDebugMode() ) PredefinedVocabularyManager.logger.info("...missing local, adding remote...");
            try {
                this.addPredefinedVocabularies();
            }
            catch (Exception ex2) {
                // Predefined vocabularies are not defined properly...
                //   Ignore the exception, but log it...
                if ( Util.isVerbose() || Util.isDebugMode() ) {
                    PredefinedVocabularyManager.logger.warn("Loading predefined vocabularies failed: " + ex2.getMessage(), ex2);
                    if ( Util.isVerbose(2) || Util.isDebugMode() ) ex2.printStackTrace();
                }
            }
            try {
                this.save();
            }
            catch (Exception ex2) {
                // Saving predefined vocabularies failed...
                //   Ignore the exception, but log it...
                if ( Util.isVerbose() || Util.isDebugMode() ) {
                    PredefinedVocabularyManager.logger.warn("Saving local Vocabulary failed: ", ex2);
                    if ( Util.isVerbose(2) || Util.isDebugMode() ) ex2.printStackTrace();
                }
            }
        }
        if ( Util.isDebugMode() ) PredefinedVocabularyManager.logger.info("...reconstructed");
    }

    public VocabularyList getPredefinedVocabularies() {
        return this.predefinedVocabularies;
    }

    /*
     * Private methods
     */
    private void reconstructVocabulariesFromFile() throws IOException {
        File vocabulariesFile =  new File(this.workingDir, SAVED_VOCABS_FILE_NAME);
        if (vocabulariesFile.exists() && vocabulariesFile.length() != 0) {
            this.load();
        }
        else {
            throw new FileNotFoundException();
        }
    }

    private void addPredefinedVocabularies() throws IOException {
        InputStream inStream = getPredefinedVocabularyFile();
        if (inStream == null) {
            String strError = "Predefined Vocabulary resource not found!";
            throw new IOException(strError);
        }
        BufferedReader buffReader = new BufferedReader( new InputStreamReader(inStream) );
        String strLine, strPrefix = null, strNamespace = null, strFetchURL = null;

        //  Read ontology file lines...
        //      There should be at least 2 entries per line but ideally 3:
        //          Prefix, Namespace, URL
        //      The Namespace can also serve as the URL but may not be identical in all cases.
        //      Each entry should be separated by whitespace.
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
                strFetchURL = null;
            else
                strFetchURL = astrTokens[2];

            // Import and Index the ontology...
            try {
                this.context.
                    getVocabularySearcher().
                        importAndIndexVocabulary(strPrefix, strNamespace, strFetchURL);
                this.predefinedVocabularies.add( new Vocabulary(strPrefix, strNamespace) );
            }
            catch (Exception ex) {
                // Predefined vocabularies are not defined properly...
                //   Ignore the exception, but log it...
                if ( Util.isVerbose() ) {
                    PredefinedVocabularyManager.logger.warn("Predefined vocabulary import failed: ", ex);
                    if ( Util.isVerbose(2) || Util.isDebugMode() ) ex.printStackTrace();
                }
            }

        }
        buffReader.close();
        context.getVocabularySearcher().update();
    }

    protected InputStream getPredefinedVocabularyFile() {
        return this.getClass().getResourceAsStream(PREDEFINED_VOCABS_FILE_NAME);
    }

    protected void load() throws IOException {
        File vocabsFile = new File(this.workingDir, SAVED_VOCABS_FILE_NAME);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jnodeVocabs = mapper.readTree(vocabsFile);
        if ( jnodeVocabs != null && jnodeVocabs.has(Util.gstrNamespaces) ) {
            JsonNode jnodeNamespaces = jnodeVocabs.get(Util.gstrNamespaces);
            Iterator<Entry<String, JsonNode>> fields = jnodeNamespaces.fields();
            fields.forEachRemaining(prefix -> {
                String strPrefix = prefix.getKey();
                String strNamespace = prefix.getValue().asText();
                this.predefinedVocabularies.add( new Vocabulary(strPrefix, strNamespace) );
            });
        }
    }

    private void save() throws IOException {
        if ( predefinedVocabularies.isEmpty() ) {
            // Nothing to save...
            return;
        }

        synchronized(SAVED_VOCABS_FILE_NAME) {
            File fileTemp = new File(this.workingDir, "vocabs.temp.json");
            try {
                this.saveToFile(fileTemp);
            }
            catch (Exception ex) {
                PredefinedVocabularyManager.logger.error("ERROR: Project metadata save failed: ", ex);
                if ( Util.isVerbose() || Util.isDebugMode() ) ex.printStackTrace();
                return;
            }

            File fileNew = new File(this.workingDir, SAVED_VOCABS_FILE_NAME);
            File fileOld = new File(this.workingDir, "vocabs.old.json");

            if ( fileNew.exists() ) {
                if ( ! fileNew.renameTo(fileOld) ) {
                    PredefinedVocabularyManager.logger.error("ERROR: Could not archive existing Project metadata!");
                }
            }
            if ( ! fileTemp.renameTo(fileNew) ) {
                PredefinedVocabularyManager.logger.error("ERROR: Could not create Project metadata!");
            }
            if ( fileOld.exists() ) {
                if ( ! fileOld.delete() ) {
                    PredefinedVocabularyManager.logger.error("ERROR: Could not remove archived Project metadata!");
                }
            }
        }
    }

    private void saveToFile(File fileVocab) throws Exception {
        Writer writer = new OutputStreamWriter(new FileOutputStream(fileVocab));
        JsonGenerator jsonWriter = ParsingUtilities.mapper.getFactory().createGenerator(writer);
        write(jsonWriter);
        writer.close();
    }

    private void write(JsonGenerator writer) throws JsonGenerationException, IOException {
        writer.writeStartObject();
        writer.writeObjectFieldStart(Util.gstrNamespaces);
        for (Vocabulary vocab : this.predefinedVocabularies) {
            vocab.write(writer);
        }
        writer.writeEndObject();
        writer.writeEndObject();
        writer.flush();
    }
}
