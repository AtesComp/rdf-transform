/*
 *  Class ApplicationContext
 *
 *  The Application Context class use to manage the RDF Transform extension.
 *
 *  Copyright 2025 Keven L. Ates
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.openrefine.rdf;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.openrefine.rdf.model.Util;
import org.openrefine.rdf.model.vocab.IPredefinedVocabularyManager;
import org.openrefine.rdf.model.vocab.IVocabularySearcher;
import org.openrefine.rdf.model.vocab.PredefinedVocabularyManager;
import org.openrefine.rdf.model.vocab.NamespaceManager;
import org.openrefine.rdf.model.vocab.VocabularySearcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationContext {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:AppContext");

    private final static String CURATED_VOCABS_FILE_NAME = "/files/Namespaces";
    private final static String DEFAULT_SITE = "http://localhost:3333/";

    private String strPort;
    private String strHost;
    private String strIFace;
    private File fileWorkingDir = null;
    private IPredefinedVocabularyManager predefinedVocabularyManager = null;
    private IVocabularySearcher vocabularySearcher = null;
    private NamespaceManager nsManager = null;

    public IPredefinedVocabularyManager getPredefinedVocabularyManager() {
        return predefinedVocabularyManager;
    }

    public IVocabularySearcher getVocabularySearcher() {
        return vocabularySearcher;
    }

    public void init(String strHost, String strIFace, String strPort, File fileWorkingDir) throws IOException {
        if (Util.isVerbose(3) || Util.isDebugMode() ) ApplicationContext.logger.info("Initializing Context...");
        this.strHost =  ( strHost  == null || strHost.isEmpty()  ) ? null : strHost;
        this.strIFace = ( strIFace == null || strIFace.isEmpty() ) ? null : strIFace;
        this.strPort =  ( strPort  == null || strPort.isEmpty()  ) ? null : strPort;
        this.fileWorkingDir = fileWorkingDir;
        if ( Util.isDebugMode() ) {
            ApplicationContext.logger.info(
                "Default Context: Host=" + ( this.strHost  == null ? "<undef>" : this.strHost ) + ", " +
                "IFace=" + ( this.strIFace == null ? "<undef>" : this.strIFace ) + ", " +
                "Port=" + ( this.strPort  == null ? "<undef>" : this.strPort ) );
        }

        try {
            this.vocabularySearcher = new VocabularySearcher(this.fileWorkingDir);
            this.predefinedVocabularyManager = new PredefinedVocabularyManager(this, this.fileWorkingDir);
            InputStream inStream = this.getClass().getResourceAsStream(CURATED_VOCABS_FILE_NAME);
            this.nsManager = new NamespaceManager(inStream);
        }
        catch (Exception e) {
            ApplicationContext.logger.error("  ERROR: ABORTED - Context failed to initialize!");
            throw e;
        }
        if (Util.isVerbose(3) || Util.isDebugMode() ) ApplicationContext.logger.info("...Context initialized.");
    }

    public void setPredefinedVocabularyManager(IPredefinedVocabularyManager predefinedVocabularyManager) {
        this.predefinedVocabularyManager = predefinedVocabularyManager;
    }

    public void setVocabularySearcher(IVocabularySearcher vocabularySearcher) {
        this.vocabularySearcher = vocabularySearcher;
    }

    public NamespaceManager getNSManager() {
        return nsManager;
    }

    public String getDefaultBaseIRI() {
        if (this.strHost.isEmpty() && this.strIFace.isEmpty())
            return DEFAULT_SITE; // ...default
        String strIRI =
            "http://" +
            ( this.strHost.isEmpty() ? this.strIFace : this.strHost ) +
            ( this.strPort.isEmpty() ? "" : ":" + this.strPort ) +
            "/";
        return strIRI;
    }
}
