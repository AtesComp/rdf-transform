/*
 *  Class VocabularySearcher
 *
 *  A Vocabulary Searcher class used to manage a Lucene Indexer for
 *  vocabulary terms in an RDF Transform.
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

package org.openrefine.rdf.model.vocab;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.StandardCopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openrefine.rdf.model.Util;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.IndexFormatTooOldException;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.StringField;

import org.apache.jena.sparql.core.DatasetGraph;
//import org.apache.jena.rdf.model.Model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* Lucene Notes:
 *
 * StoredField -- NOT Searchable ----------------------------
 * StringField -- Searchable by Exact String ----------------
 *      Util.gstrIRI,         strIRI,        Field.Store.YES
 *      Util.gstrPrefix,      strPrefix,     Field.Store.YES
 *      Util.gstrNamespace,   strNamespace,  Field.Store.YES
 *      Util.gstrType,        strNodeType,   Field.Store.YES
 *      Util.gstrProject,     strProjectID,  Field.Store.NO
 * TextField ---- Seachable by Tokenized words --------------
 *      Util.gstrLabel,       strLabel,      Field.Store.YES
 *      Util.gstrDescription, strDesc,       Field.Store.YES
 *      Util.gstrLocalPart,   strLocalPart,  Field.Store.YES
 *
 * Search terms must be lowercase if explicitly used in Query Builders. Parsers will parse terms to
 * lowercase.
 */

public class VocabularySearcher implements IVocabularySearcher {
    static private final Logger logger = LoggerFactory.getLogger("RDFT:VocabSearcher");

    static public final String s_strLuceneDir = "luceneIndex";
    static public final String s_strLuceneDirOld = "luceneIndex_OLD";

    static private final String s_strClassType = "class";
    static private final String s_strPropertyType = "property";
    // "type": vocabulary AND "project": projectID AND "name": name
    // ("type": (class OR property) ) AND "project": strProjectID AND "prefix": prefix
    static private final BooleanQuery s_queryType =
            new BooleanQuery.Builder().
                add( new TermQuery( new Term(Util.gstrType, VocabularySearcher.s_strClassType) ), Occur.SHOULD ).
                add( new TermQuery( new Term(Util.gstrType, VocabularySearcher.s_strPropertyType) ), Occur.SHOULD ).
                build();

    // Since the Project ID is always a number, it is safe to use "g" as the "global" (or non-project) project ID...
    static private final String s_strGlobalProjectID = "g";

    private Directory dirLucene = null;
    private StandardAnalyzer analyzer = null;
    private IndexWriter writer = null;
    private DirectoryReader reader = null;
    private IndexSearcher searcher = null;

    public VocabularySearcher(File dir)
            throws IOException {
        if ( Util.isDebugMode() || Util.isVerbose(3) ) VocabularySearcher.logger.info("Creating vocabulary searcher...");

        Path pathLucene = null;
        try {
            pathLucene = new File(dir, s_strLuceneDir).toPath();
            this.dirLucene = FSDirectory.open(pathLucene);
        }
        catch (IOException ex) {
            VocabularySearcher.logger.error("  ERROR: ABORTED - Cannot open Lucene directory!");
            if ( Util.isDebugMode() || Util.isVerbose() ) VocabularySearcher.logger.error("  ERROR: ABORTED!", ex);
            throw ex;
        }

        this.analyzer = new StandardAnalyzer();
        try {
            this.writer = new IndexWriter(this.dirLucene, new IndexWriterConfig(this.analyzer));
            this.writer.commit();
        }
        // NOTE: IndexFormatTooOldException is an IOException
        catch (IndexFormatTooOldException | IllegalArgumentException ex) {
            VocabularySearcher.logger.warn("  WARNING: Lucene Index format incompatible. Attempting new indexing...");

            this.updateLucene(dir, pathLucene);
        }
        catch (IOException ex) { // ...other IOException
            VocabularySearcher.logger.error("  ERROR: ABORTED - IO Error!", ex);
            throw ex;
        }

        try {
            this.reader = DirectoryReader.open(this.writer);
        }
        catch (IOException ex) { // ...including CorruptIndexException
            VocabularySearcher.logger.error("  ERROR: ABORTED - Cannot create Lucene Directory Reader!");
            if ( Util.isDebugMode() || Util.isVerbose() ) VocabularySearcher.logger.error("  ERROR: ABORTED!", ex);
            throw ex;
        }
        this.searcher = new IndexSearcher(this.reader);

        //
        // Clean the store...
        // ====================================================================================================

        if ( this.isOldDocs() ) {
            if ( Util.isDebugMode() || Util.isVerbose() ) VocabularySearcher.logger.info("  Updating Lucene store to new indexing...");
            // Roll back searcher, reader, writer...
            this.searcher = null;
            this.reader.close();
            this.reader = null;
            this.writer.close();
            this.writer = null;

            this.updateLucene(dir, pathLucene);
            try {
                this.reader = DirectoryReader.open(this.writer);
            }
            catch (IOException ex) { // ...including CorruptIndexException
                VocabularySearcher.logger.error("  ERROR: ABORTED - Cannot create Lucene Directory Reader!");
                if ( Util.isDebugMode() || Util.isVerbose() ) VocabularySearcher.logger.error("  ERROR: ABORTED!", ex);
                throw ex;
            }
            this.searcher = new IndexSearcher(this.reader);
        }

        // ...end Clean the store

        if ( Util.isDebugMode() || Util.isVerbose(3) ) VocabularySearcher.logger.info("...created vocabulary searcher");
    }

    private void updateLucene(File dir, Path pathLucene)
            throws IOException {
        try {
            // Move current Lucene files to OLD directory so that a new directory can be built...
            File dirLuceneOld = new File(dir, s_strLuceneDirOld);
            if ( ! Util.recursiveDirDelete(dirLuceneOld) ) { // ...remove any existing OLD Lucene directory
                throw new IOException("Could not recursively delete OLD Lucene directory!");
            }
            Path pathLuceneOld = dirLuceneOld.toPath();
            Files.move(
                pathLucene,
                pathLuceneOld,
                StandardCopyOption.REPLACE_EXISTING
            );
            if (this.writer == null) {
                this.writer = new IndexWriter(this.dirLucene, new IndexWriterConfig(this.analyzer));
            }
            // Retry commit...
            this.writer.commit();
        }
        catch (IOException ex) {
            VocabularySearcher.logger.error("  ERROR: ABORTED - Cannot open Lucene directory (2)!", ex);
            throw ex;
        }
    }

    /**
     * Import a Global vocabulary from a URL.
     */
    @Override
    public void importAndIndexVocabulary(String strPrefix, String strNamespace, String strLocation, Vocabulary.LocationType theLocType)
            throws VocabularyImportException, IOException {
        // Since no Project ID was given, use the Global Project ID and pass to the Project method...
        this.importAndIndexVocabulary(strPrefix, strNamespace, strLocation, theLocType, VocabularySearcher.s_strGlobalProjectID);
    }

    /**
     * Import a Project vocabulary from a URL.
     */
    @Override
    public void importAndIndexVocabulary(String strPrefix, String strNamespace, String strLocation, Vocabulary.LocationType theLocType,
                                            String strProjectID)
            throws VocabularyImportException, IOException {
        String strDebug = null;
        if ( Util.isDebugMode() ) {
            strDebug = "DEBUG: Import And Index vocabulary " + strPrefix + ": <" + strNamespace + "> ";
        }
        if ( strLocation == null || strLocation.isEmpty() ) {
            if ( Util.isDebugMode() ) VocabularySearcher.logger.info(strDebug + "nothing to fetch!");
            return;
        }
        if ( Util.isDebugMode() ) VocabularySearcher.logger.info( strDebug + "from " + strLocation + " as type " + theLocType.toString() );

        if (theLocType == Vocabulary.LocationType.URL) {
            VocabularyImporter importer = new VocabularyImporter(strPrefix, strNamespace, strLocation);
            List<RDFTClass> classes = new ArrayList<RDFTClass>();
            List<RDFTProperty> properties = new ArrayList<RDFTProperty>();

            // Import classes & properties from Namespace at URL...
            importer.importVocabulary(classes, properties);
            this.indexTerms(strProjectID, classes, properties);
        }
        else {
            VocabularySearcher.logger.error( "Cannot import vocabulary from " + strLocation + " as type " + theLocType.toString() + "!" );
        }
    }

    /**
     * Import a Project vocabulary from a Dataset Graph (generally loaded from a file).
     */
    @Override
    public void importAndIndexVocabulary(String strPrefix, String strNamespace, String strLocation, DatasetGraph theDSGraph, String strProjectID)
            throws VocabularyImportException, IOException {
        VocabularyImporter importer = new VocabularyImporter(strPrefix, strNamespace, strLocation);
        List<RDFTClass> classes = new ArrayList<RDFTClass>();
        List<RDFTProperty> properties = new ArrayList<RDFTProperty>();

        // Import classes & properties from Namespace in Repository...
        if ( Util.isDebugMode() ) {
            VocabularySearcher.logger.info("DEBUG: Import And Index vocabulary " +
                    strPrefix + ": <" + strNamespace + ">");
        }
        importer.importVocabulary(theDSGraph, classes, properties);
        this.indexTerms(strProjectID, classes, properties);
    }

    @Override
    public List<SearchResultItem> searchClasses(String strQueryVal, String strProjectID)
            throws IOException {
        Query query = this.prepareQuery(strQueryVal, VocabularySearcher.s_strClassType, strProjectID);
        return this.searchDocs(query);
    }

    @Override
    public List<SearchResultItem> searchProperties(String strQueryVal, String strProjectID)
            throws IOException {
        Query query = this.prepareQuery(strQueryVal, VocabularySearcher.s_strPropertyType, strProjectID);
        return this.searchDocs(query);
    }

    private List<SearchResultItem> searchDocs(Query query)
            throws IOException {
        TopDocs docs = this.searcher.search( query, this.getMaxDoc() );
        return this.prepareSearchResults(docs);
    }

    @Override
    public void addTerm(RDFTNode node, String strNodeType, String strProjectID)
            throws IOException {
        if ( this.indexRDFTNode(node, strNodeType, strProjectID) ) this.update();
    }

    @Override
    public void deleteTerm(RDFTNode node, String strNodeType, String strProjectID)
            throws IOException {
        // If the node's IRI exists with the given Type and Project ID...
        if ( this.isIndexedRDFTNode(node.getIRI(), strNodeType, strProjectID) ) {
            // ...try to delete the specific node...
            if ( this.deleteRDFTNode(node, strNodeType, strProjectID) ) this.update();
        }
    }

    @Override
    public void deleteVocabularySetTerms(Set<Vocabulary> setVocab, String strProjectID)
            throws IOException {
        for (Vocabulary vocab : setVocab) {
            this.deleteTerms(vocab.getPrefix(), strProjectID);
        }
        this.update();
    }

    @Override
    public void addPredefinedVocabulariesToProject(long liProjectID)
            throws IllegalArgumentException, IOException {
        String strProjectID = String.valueOf(liProjectID);
        // Add all Global document to the current project...
        this.addGlobalDocumentsToProject(strProjectID);
        this.update();
    }

    @Override
    public void update()
            throws IOException {
        if ( this.writer.hasUncommittedChanges() ) {
            this.writer.commit();
            //this.reader.close();
            //this.reader = DirectoryReader.open(this.dirLucene);
            //this.searcher = new IndexSearcher(this.reader);
            DirectoryReader readerNew = DirectoryReader.openIfChanged(this.reader);
            if (readerNew != null) {
                this.reader = readerNew;
                this.searcher = new IndexSearcher(this.reader);
            }
        }
    }

    @Override
    public void synchronize(String strProjectID, Set<String> setPrefixes)
            throws IOException {
        Set<String> setRedactedPrefixes = this.getPrefixesOfProjectID(strProjectID);
        setRedactedPrefixes.removeAll(setPrefixes);
        if ( ! setRedactedPrefixes.isEmpty() ) {
            this.deletePrefixesOfProjectID(strProjectID, setRedactedPrefixes);
        }
        this.update();
    }

    @Override
    public void deleteVocabularyTerms(String strPrefix, String strProjectID)
            throws IOException {
        this.deleteTerms(strPrefix, strProjectID);
        this.update();
    }

    /*
     * Private methods
     */

    private boolean isOldDocs()
            throws IOException {
        TopDocs docs = null;
        boolean bCleanObject = true;

        //
        // Does the store have any old document structures?...
        //

        BooleanQuery queryDocs =
            new BooleanQuery.Builder().
                add(VocabularySearcher.s_queryType, Occur.MUST). // ...either "class" or "property"
                build();
        docs = this.searcher.search( queryDocs, this.getMaxDoc(0) );
        if ( Util.isDebugMode() ) VocabularySearcher.logger.info("  DEBUG: Cleaning Old Docs Val: " + docs.totalHits.value);
        if (docs.totalHits.value == 0) bCleanObject = false;

        if (bCleanObject) {
            for (ScoreDoc sdoc : docs.scoreDocs) {
                Document doc = this.searcher.storedFields().document(sdoc.doc);

                if (
                    doc.getField(Util.gstrIRI       ).fieldType().indexOptions() != IndexOptions.NONE &&
                    doc.getField(Util.gstrNamespace ).fieldType().indexOptions() != IndexOptions.NONE
                ) continue;

                return true; // ...on any change required
            }
        }
        return false;
    }

    private void deleteTerms(String strPrefix, String strProjectID)
            throws IOException {
        if ( strProjectID == null || strProjectID.isEmpty() ) {
            throw new RuntimeException("Project ID is missing!");
        }

        BooleanQuery queryTerms =
            new BooleanQuery.Builder().
                add(new TermQuery(new Term(Util.gstrPrefix, strPrefix)), Occur.MUST).
                add(new TermQuery(new Term(Util.gstrProject, strProjectID)), Occur.MUST).
                add(VocabularySearcher.s_queryType, Occur.MUST). // ...either "class" or "property"
                build();

        this.writer.deleteDocuments(queryTerms);
        this.update();
    }

    private void indexTerms(String strProjectID, List<RDFTClass> classes, List<RDFTProperty> properties)
            throws IOException {
        boolean bAdded = false;
        for (RDFTClass klass : classes) {
            if ( this.indexRDFTNode(klass, VocabularySearcher.s_strClassType, strProjectID) ) bAdded = true;
        }
        for (RDFTProperty prop : properties) {
            if ( this.indexRDFTNode(prop, VocabularySearcher.s_strPropertyType, strProjectID) ) bAdded = true;
        }
        if (bAdded) this.update();
    }

    private boolean isIndexedRDFTNode(String strIRI, String strNodeType, String strProjectID)
            throws IOException {
        if ( strProjectID == null || strProjectID.isEmpty() ) {
            if ( Util.isVerbose(2) ||  Util.isDebugMode() ) {
                VocabularySearcher.logger.error("ERROR: Project ID is missing!");
            }
            throw new RuntimeException("Project ID is missing!");
        }

        // From RDFTNode...
        //
        //  IRI         str   required
        //  Label       str   copy of IRI if not given
        //  Description null
        //  Prefix      null
        //  Namespace   null  generated from IRI if not given
        //  LocalPart   null  generated from IRI if not given

        if ( strIRI == null || strIRI.isEmpty() ) {
            if ( Util.isVerbose(2) ||  Util.isDebugMode() ) {
                VocabularySearcher.logger.error("ERROR: Indexing IRI cannot be null or empty!");
            }
            throw new RuntimeException("Indexing IRI is missing!");
        }

        if ( Util.isDebugMode() ) VocabularySearcher.logger.info("DEBUG: Searching: ");

        //StandardQueryParser parser = new StandardQueryParser(this.analyzer);

        BooleanQuery.Builder qbuilderTerms = new BooleanQuery.Builder();

        // strIRI (String)
        qbuilderTerms.add(new TermQuery(new Term(Util.gstrIRI, strIRI)),  Occur.MUST); // ...exact string

        // strNodeType (String)
        if ( strNodeType != null ) qbuilderTerms.add(new TermQuery(new Term(Util.gstrType, strNodeType)),  Occur.MUST); // ...exact string

        // strProjectID (String, Field.Store.NO)
        qbuilderTerms.add(new TermQuery(new Term(Util.gstrProject, strProjectID)), Occur.MUST);

        BooleanQuery queryTerms = qbuilderTerms.build();

        TopDocs docsCheck = this.searcher.search( queryTerms, this.getMaxDoc(2) );
        if ( Util.isDebugMode() ) VocabularySearcher.logger.info("DEBUG:   Found: " + docsCheck.totalHits.value);
        if (docsCheck.totalHits.value > 0) {
            if ( Util.isDebugMode() ) {
                VocabularySearcher.logger.info(
                    "DEBUG:   [" +
                        strIRI + "] (found " + docsCheck.totalHits.value + ")");
            }
            return true;
        }

        return false;
    }

    private boolean deleteRDFTNode(RDFTNode node, String strNodeType, String strProjectID)
            throws IOException {
        if ( strProjectID == null || strProjectID.isEmpty() ) {
            if ( Util.isVerbose(2) ||  Util.isDebugMode() ) {
                VocabularySearcher.logger.error("ERROR: Project ID is missing!");
            }
            throw new RuntimeException("Project ID is missing!");
        }

        // From RDFTNode...
        //
        //  IRI         str   required
        //  Label       str   copy of IRI if not given
        //  Description null
        //  Prefix      null
        //  Namespace   null  generated from IRI if not given
        //  LocalPart   null  generated from IRI if not given

        String strIRI = node.getIRI();
        if ( strIRI == null || strIRI.isEmpty() ) {
            if ( Util.isVerbose(2) ||  Util.isDebugMode() ) {
                VocabularySearcher.logger.error("ERROR: Indexing IRI cannot be null or empty! Skipping...");
            }
            return false;
        }

        String strLabel = node.getLabel();
        if (strLabel == null)     strLabel = "";

        String strDesc = node.getDescription();
        if (strDesc == null)      strDesc = "";

        String strPrefix = node.getPrefix();
        if (strPrefix == null)    strPrefix = "";

        String strNamespace = node.getNamespace();
        if (strNamespace == null) strNamespace = "";

        String strLocalPart = node.getLocalPart();
        if (strLocalPart == null) strLocalPart = "";

        if ( Util.isDebugMode() ) VocabularySearcher.logger.info("DEBUG: Deleting: ");

        StandardQueryParser parser = new StandardQueryParser(this.analyzer);

        BooleanQuery.Builder qbuilderTerms = new BooleanQuery.Builder();

        // strIRI (String)
        qbuilderTerms.add(new TermQuery(new Term(Util.gstrIRI, strIRI)),  Occur.MUST); // ...exact string

        // strLabel (Terms)
        if ( strLabel != null && ! strLabel.isEmpty() ) {
            try {
                Query queryLabel = parser.parse(Util.gstrLabel + ":\"" + strLabel + "\"", Util.gstrLabel); // ...exact terms in phrase
                qbuilderTerms.add(queryLabel, Occur.MUST);
            }
            catch (QueryNodeException ex) {
                VocabularySearcher.logger.warn("  WARNING: Cannot delete current doc.");
                if ( Util.isDebugMode() ) VocabularySearcher.logger.warn("  WARNING: Deleting Docs!", ex);
                return false;
            }
        }

        // strDesc (Terms)
        if ( strDesc != null && ! strDesc.isEmpty() ) {
            try {
                Query queryDesc = parser.parse(Util.gstrDescription + ":\"" + strDesc + "\"", Util.gstrDescription); // ...exact terms in phrase
                qbuilderTerms.add(queryDesc, Occur.MUST);
            }
            catch (QueryNodeException ex) {
                VocabularySearcher.logger.warn("  WARNING: Cannot clean current doc.");
                if ( Util.isDebugMode() ) VocabularySearcher.logger.warn("  WARNING: Deleting Docs!", ex);
                return false;
            }
        }

        // strPrefix (String)
        if ( strPrefix != null ) qbuilderTerms.add(new TermQuery(new Term(Util.gstrPrefix, strPrefix)),  Occur.MUST); // ...exact string

        // strNamespace (String)
        if ( strNamespace != null ) qbuilderTerms.add(new TermQuery(new Term(Util.gstrNamespace, strNamespace)),  Occur.MUST); // ...exact string

        // strLocalPart (Terms)
        if ( strLocalPart != null && ! strDesc.isEmpty() ) {
            try {
                Query queryLocalPart = parser.parse(Util.gstrLocalPart + ":\"" + strLocalPart + "\"", Util.gstrLocalPart); // ...exact terms in phrase
                qbuilderTerms.add(queryLocalPart, Occur.MUST);
            }
            catch (QueryNodeException ex) {
                VocabularySearcher.logger.warn("  WARNING: Cannot delete current doc.");
                if ( Util.isDebugMode() ) VocabularySearcher.logger.warn("  WARNING: Deleting Docs!", ex);
                return false;
            }
        }

        // strNodeType (String)
        if ( strNodeType  != null ) qbuilderTerms.add(new TermQuery(new Term(Util.gstrType, strNodeType)),  Occur.MUST); // ...exact string

        // strProjectID (String, Field.Store.NO)
        qbuilderTerms.add(new TermQuery(new Term(Util.gstrProject, strProjectID)), Occur.MUST);

        BooleanQuery queryTerms = qbuilderTerms.build();

        if ( Util.isDebugMode() ) {
            VocabularySearcher.logger.info( "DEBUG:   Searching...\n" +
                "          IRI: " + strIRI + "\n" +
                "        Label: " + strLabel + "\n" +
                "         Desc: " + strDesc + "\n" +
                "       Prefix: " + strPrefix + "\n" +
                "    Namespace: " + strNamespace + "\n" +
                "    LocalPart: " + strLocalPart + "\n" +
                "         Type: " + strNodeType + "\n" +
                "      Project: " + strProjectID);
        }
        TopDocs docsDelete = this.searcher.search( queryTerms, this.getMaxDoc(2) );
        if ( Util.isDebugMode() ) VocabularySearcher.logger.info("DEBUG:   Found: " + docsDelete.totalHits.value);
        if (docsDelete.totalHits.value > 0) {
            if ( Util.isDebugMode() ) {
                VocabularySearcher.logger.info(
                    "DEBUG:   [" +
                        strPrefix + ", " +
                        strNamespace + ", " +
                        strLocalPart + " = " +
                        strIRI + "] (found " + docsDelete.totalHits.value + ")");
            }
            this.writer.deleteDocuments(queryTerms);
            return true;
        }

        return false;
    }

    private boolean indexRDFTNode(RDFTNode node, String strNodeType, String strProjectID)
            throws IOException {
        if ( strProjectID == null || strProjectID.isEmpty() ) {
            if ( Util.isVerbose(2) ||  Util.isDebugMode() ) {
                VocabularySearcher.logger.error("ERROR: Project ID is missing!");
            }
            throw new RuntimeException("Project ID is missing!");
        }

        // From RDFTNode...
        //
        //  IRI         str   required
        //  Label       str   copy of IRI if not given
        //  Description null
        //  Prefix      null
        //  Namespace   null  generated from IRI if not given
        //  LocalPart   null  generated from IRI if not given

        String strIRI = node.getIRI();
        if (strIRI == null) {
            if ( Util.isVerbose(2) ||  Util.isDebugMode() ) {
                VocabularySearcher.logger.warn("WARNING: Indexing IRI cannot be null! Skipping...");
            }
            return false;
        }
        // If the node's IRI exists with the given Type and Project ID, don't add another new one...
        if ( this.isIndexedRDFTNode(strIRI, strNodeType, strProjectID) ) return false;

        String strLabel = node.getLabel();
        if (strLabel == null)     strLabel = "";

        String strDesc = node.getDescription();
        if (strDesc == null)      strDesc = "";

        String strPrefix = node.getPrefix();
        if (strPrefix == null)    strPrefix = "";

        String strNamespace = node.getNamespace();
        if (strNamespace == null) strNamespace = "";

        String strLocalPart = node.getLocalPart();
        if (strLocalPart == null) strLocalPart = "";

        if ( Util.isDebugMode() ) VocabularySearcher.logger.info("DEBUG: Indexing: ");

        /*
         * Create a new lucene document to store and index the related content
         * for an RDF Node.
         */
        Document doc = new Document();

        // NOTES:
        //  StoredField is not indexed
        //  StringField is indexed but not token analyzed
        //  TextField is indexed and token analyzed

        doc.add( new StringField( Util.gstrIRI,         strIRI,        Field.Store.YES) );
        doc.add( new TextField(   Util.gstrLabel,       strLabel,      Field.Store.YES) );
        doc.add( new TextField(   Util.gstrDescription, strDesc,       Field.Store.YES) );
        doc.add( new StringField( Util.gstrPrefix,      strPrefix,     Field.Store.YES) );
        doc.add( new StringField( Util.gstrNamespace,   strNamespace,  Field.Store.YES) );
        doc.add( new TextField(   Util.gstrLocalPart,   strLocalPart,  Field.Store.YES) );
        // From Node Type (Class or Property)...
        doc.add( new StringField( Util.gstrType,        strNodeType,   Field.Store.YES) );
        // From Project ID...
        doc.add( new StringField( Util.gstrProject,     strProjectID,  Field.Store.NO ) );

        // Add the node document...
        if ( Util.isDebugMode() ) VocabularySearcher.logger.info("DEBUG:   Adding IRI: " + strIRI);
        this.writer.addDocument(doc);

        return true;
    }

    private Query prepareQuery(String strQueryVal, String strNodeType, String strProjectID)
            throws IOException {
        BooleanQuery.Builder qbuilderTerms = new BooleanQuery.Builder();

        if (strQueryVal != null && strQueryVal.strip().length() > 0) {
            int iColon = strQueryVal.indexOf(":");
            TokenStream stream;
            CharTermAttribute termAttrib;

            if (iColon == -1) { // ...just a term...
                // The Query: search for term in "label", "description", "prefix", and "localPart"
                // -----------------------------------------
                // ( "label" : OR 0..n termAttrib* OR        \
                //   "description" : OR 0..n termAttrib* OR  | These four are below
                //   "prefix" : strQueryVal* OR              |
                //   "localPart" : OR 0..n termAttrib* )     /
                // "type" : strNodeType AND                  \ These two are at the end
                // "project" : strProjectID AND              /
                //
                // "iri" and "namespace" are excluded

                //
                // Add "label" terms...
                //
                stream = this.analyzer.tokenStream(Util.gstrLabel, new StringReader(strQueryVal));
                stream.reset();
                // Get the TermAttribute from the TokenStream...
                termAttrib = (CharTermAttribute) stream.addAttribute(CharTermAttribute.class);
                // For each token, do a wildcard search on each term (via CharTermAttribute)...
                BooleanQuery.Builder qbuilderLabel = new BooleanQuery.Builder();
                while ( stream.incrementToken() ) {
                    qbuilderLabel.
                        add(new WildcardQuery(new Term(Util.gstrLabel, termAttrib.toString() + "*")), Occur.SHOULD);
                }
                stream.close();
                stream.end();

                //
                // Add "description" terms...
                //
                stream = this.analyzer.tokenStream(Util.gstrDescription, new StringReader(strQueryVal));
                stream.reset();
                // Get the TermAttribute from the TokenStream...
                termAttrib = (CharTermAttribute) stream.addAttribute(CharTermAttribute.class);
                // For each token, do a wildcard search on each term (via CharTermAttribute)...
                BooleanQuery.Builder qbuilderDesc = new BooleanQuery.Builder();
                while ( stream.incrementToken() ) {
                    qbuilderDesc.
                        add(new WildcardQuery(new Term(Util.gstrDescription, termAttrib.toString() + "*")), Occur.SHOULD);
                }
                stream.close();
                stream.end();

                //
                // Add "prefix" term...
                //
                BooleanQuery.Builder qbuilderPrefix = new BooleanQuery.Builder();
                qbuilderPrefix.
                    add(new WildcardQuery(new Term(Util.gstrPrefix, strQueryVal + "*")), Occur.SHOULD);

                //
                // Add "localPart" terms...
                //
                stream = this.analyzer.tokenStream(Util.gstrLocalPart, new StringReader(strQueryVal));
                stream.reset();
                // Get the TermAttribute from the TokenStream...
                termAttrib = (CharTermAttribute) stream.addAttribute(CharTermAttribute.class);
                // For each token, do a wildcard search on each term (via CharTermAttribute)...
                BooleanQuery.Builder qbuilderLocalPart = new BooleanQuery.Builder();
                while ( stream.incrementToken() ) {
                    qbuilderLocalPart.
                        add(new WildcardQuery(new Term(Util.gstrLocalPart, termAttrib.toString() + "*")), Occur.SHOULD);
                }
                stream.close();
                stream.end();

                qbuilderTerms.
                    add(qbuilderLabel.build(),     Occur.MUST).
                    add(qbuilderDesc.build(),      Occur.MUST).
                    add(qbuilderPrefix.build(),    Occur.MUST).
                    add(qbuilderLocalPart.build(), Occur.MUST);
            }
            else { // ..."prefix:localPart"...
                // The Query: search for prefix as "prefix" and localPart in "localPart"
                // -----------------------------------
                // "prefix" : strPrefix AND            \ These two are below
                // ( localPart : OR 0..n termAttrib* ) /
                // "type" : strNodeType AND            \ These two are at the end
                // "project" : strProjectID AND        /

                String strPrefix    = strQueryVal.substring(0, iColon);
                String strLocalPart = "";
                if (iColon < strQueryVal.length() - 1) strLocalPart = strQueryVal.substring(iColon + 1);

                //
                // Add "prefix" term...
                //
                qbuilderTerms.
                    add(new TermQuery(new Term(Util.gstrPrefix, strPrefix)), Occur.MUST);

                //
                // Add "localPart" terms...
                //
                if ( ! strLocalPart.isEmpty() ) {
                    stream = this.analyzer.tokenStream(Util.gstrLocalPart, new StringReader(strLocalPart));
                    stream.reset();
                    // Get the TermAttribute from the TokenStream...
                    termAttrib = (CharTermAttribute) stream.addAttribute(CharTermAttribute.class);
                    // For each token, do a wildcard search on each term (via CharTermAttribute)...
                    BooleanQuery.Builder qbuilderLocalPart = new BooleanQuery.Builder();
                    while ( stream.incrementToken() ) {
                        // NOTE: On the stream increment, the associated termAttrib is updated...
                        qbuilderLocalPart.
                            add(new WildcardQuery(new Term(Util.gstrLocalPart, termAttrib.toString() + "*")), Occur.SHOULD);
                    }
                    stream.end();
                    stream.close();

                    qbuilderTerms.
                        add(qbuilderLocalPart.build(), Occur.MUST);
                }
            }
        }

        qbuilderTerms.
            add(new TermQuery(new Term(Util.gstrType,    strNodeType)),  Occur.MUST).
            add(new TermQuery(new Term(Util.gstrProject, strProjectID)), Occur.MUST);

        return qbuilderTerms.build();
    }

    private List<SearchResultItem> prepareSearchResults(TopDocs docs)
            throws IOException {
        List<SearchResultItem> results = new ArrayList<SearchResultItem>();
        for (ScoreDoc sdoc : docs.scoreDocs) {
            Document doc = this.searcher.storedFields().document(sdoc.doc);

            String strIRI       = doc.get(Util.gstrIRI);
            String strLabel     = doc.get(Util.gstrLabel);
            String strDesc      = doc.get(Util.gstrDescription);
            String strPrefix    = doc.get(Util.gstrPrefix);
            String strNamespace = doc.get(Util.gstrNamespace);
            String strLocalPart = doc.get(Util.gstrLocalPart);

            results.
                add(
                    new SearchResultItem(
                        strIRI,
                        strLabel,
                        strDesc,
                        strPrefix,
                        strNamespace,
                        strLocalPart
                    )
                );
        }

        return results;
    }

    private void addGlobalDocumentsToProject(String strProjectID)
            throws IllegalArgumentException, IOException {
        // The TopDocs are documents with the Global (Non-Project related) Project ID.
        TopDocs docsGlobal = this.getDocumentsOfProjectID(VocabularySearcher.s_strGlobalProjectID);

        // These docs are "copied" for use in the specified project.
        // See the calling function: addPredefinedVocabulariesToProject()

        // Set new Project ID for "copied" documents ( just like in indexRDFTNode() ) for query only...
        IndexableField fieldProjectID = new StringField(Util.gstrProject, strProjectID, Field.Store.NO);

        // Iterate through the Global documents...
        for (ScoreDoc sdoc : docsGlobal.scoreDocs) {
            // Get an existing Global document...
            Document docGlobal = this.searcher.storedFields().document(sdoc.doc);

            // Prepare a new project document...
            Document docProject = new Document();

            // Copy all fields to the project doc...
            List<IndexableField> fields = docGlobal.getFields();
            for (IndexableField field : fields) {
                docProject.add(field);
            }

            // Change the Global Project ID field to the specified Project ID field...
            docProject.removeField(Util.gstrProject);   // ...should never need removal since "project" field
                                                        // is not a stored field (Field.Store.NO) and not returned
                                                        // in the fields on a query.
            docProject.add(fieldProjectID); // ...add the "project" field to the document for query only

            // Store and index the new project document...
            this.writer.addDocument(docProject);
        }
    }

    private TopDocs getDocumentsOfProjectID(String strProjectID)
            throws IOException {
        // Query for ALL document with Project ID...
        Query queryTerms = new TermQuery(new Term(Util.gstrProject, strProjectID));
        return this.searcher.search( queryTerms, this.getMaxDoc(0) ); // ...no limit
    }

    private Set<String> getPrefixesOfProjectID(String strProjectID)
            throws IOException {
        // Query for ALL prefixes in ALL document with Project ID...
        Set<String> setPrefixes = new HashSet<String>();
        BooleanQuery queryTerms =
            new BooleanQuery.Builder().
                add(new WildcardQuery(new Term(Util.gstrPrefix, "*")), Occur.MUST).
                add(new TermQuery(new Term(Util.gstrProject, strProjectID)), Occur.MUST).
                build();
        TopDocs docs = this.searcher.search( queryTerms, this.getMaxDoc(100000) ); // ...well, better than no limit
        String strPrefix = null;
        for (ScoreDoc sdoc : docs.scoreDocs) {
            Document doc = this.searcher.storedFields().document(sdoc.doc);
            strPrefix = doc.get(Util.gstrPrefix);
            if (strPrefix != null) setPrefixes.add( strPrefix );
        }
        return setPrefixes;
    }

    private void deletePrefixesOfProjectID(String strProjectID, Set<String> setPrefixes)
            throws IOException {
        if ( strProjectID == null || strProjectID.isEmpty() ) {
            throw new RuntimeException("Project ID is missing!");
        }

        BooleanQuery.Builder qbuilderTerms = new BooleanQuery.Builder();
        for (String strPrefix : setPrefixes) {
            qbuilderTerms.
                add( new TermQuery( new Term(Util.gstrPrefix, strPrefix) ), Occur.SHOULD );
        }

        BooleanQuery queryDelete =
            new BooleanQuery.Builder().
                add(qbuilderTerms.build(), Occur.SHOULD).
                add(VocabularySearcher.s_queryType, Occur.MUST). // ...either "class" or "property"
                add( new TermQuery( new Term(Util.gstrProject, strProjectID) ), Occur.MUST ).
                build();

        this.writer.deleteDocuments(queryDelete);
    }

    /**
     * Get a maximum (the given limit at most) number of documents to return for a search.
     * @return int - maximum number of documents limit
     */
    private int getMaxDoc(int iLimit) {
        int iMaxDoc = reader.maxDoc();
        if (iLimit < 0) iLimit = 100;
        if (iLimit == 0) return (iMaxDoc > 0) ? iMaxDoc : 1000000; // ...no limits, live dangerously
        return (iMaxDoc > 0 && iMaxDoc < iLimit) ? iMaxDoc : iLimit;
    }

    /**
     * Get a maximum (100 at most) number of documents to return for a search.
     * @return int - maximum number of documents limit
     */
    private int getMaxDoc() {
        return getMaxDoc(100); // ...a reasonable limit
    }
}
