/*
 *  Class VocabularySearcher
 *
 *  A Vocabulary Searcher class used to search a Lucene Indexer for
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

import org.apache.lucene.analysis.Analyzer;
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
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.StringField;

import org.apache.jena.sparql.core.DatasetGraph;
//import org.apache.jena.rdf.model.Model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VocabularySearcher implements IVocabularySearcher {
    static private final Logger logger = LoggerFactory.getLogger("RDFT:VocabSearcher");

    static public final String LUCENE_DIR = "luceneIndex";
    static public final String LUCENE_DIR_OLD = "luceneIndex_OLD";

    static private final String CLASS_TYPE = "class";
    static private final String PROPERTY_TYPE = "property";
    // "type": vocabulary AND "project": projectID AND "name": name
    // ("type": (class OR property) ) AND "project": strProjectID AND "prefix": prefix
    static private final BooleanQuery TYPE_QUERY =
            new BooleanQuery.Builder().
                add( new TermQuery( new Term(Util.gstrType, VocabularySearcher.CLASS_TYPE) ), Occur.SHOULD ).
                add( new TermQuery( new Term(Util.gstrType, VocabularySearcher.PROPERTY_TYPE) ), Occur.SHOULD ).
                build();

    // Since the Project ID is always a number, it is safe to use "g" as the global marker placeholder...
    static private final String GLOBAL_VOCABULARY_PLACE_HOLDER = "g";

    private Directory dirLucene = null;
    private IndexWriter writer = null;
    private DirectoryReader reader = null;
    private IndexSearcher searcher = null;

    public VocabularySearcher(File dir)
            throws IOException {
        if ( Util.isDebugMode() || Util.isVerbose(3) ) VocabularySearcher.logger.info("Creating vocabulary searcher...");

        Path pathLuceneNew = null;
        try {
            pathLuceneNew = new File(dir, LUCENE_DIR).toPath();
            this.dirLucene = FSDirectory.open(pathLuceneNew);
        }
        catch (IOException e) {
            VocabularySearcher.logger.error("  ERROR: ABORTED - Cannot open Lucene directory!");
            if ( Util.isDebugMode() || Util.isVerbose() ) VocabularySearcher.logger.error("  ERROR: ABORTED!", e);
            throw e;
        }
        Analyzer analyzer = new StandardAnalyzer();

        try {
            this.writer = new IndexWriter(this.dirLucene, new IndexWriterConfig(analyzer));
            this.writer.commit();
        }
        // NOTE: IndexFormatTooOldException is an IOException
        catch (IndexFormatTooOldException | IllegalArgumentException e) {
            VocabularySearcher.logger.warn("  WARNING: Lucene Index format incompatible. Attempting new indexing...");

            try {
                // Move current Lucene files to OLD directory so that a new directory can be built...
                File dirLuceneOld = new File(dir, LUCENE_DIR_OLD);
                if ( ! Util.recursiveDirDelete(dirLuceneOld) ) { // ...remove any existing OLD Lucene directory
                    throw new IOException("Could not recursively delete OLD Lucene directory!");
                }
                Path pathLuceneOld = dirLuceneOld.toPath();
                Files.move(
                    pathLuceneNew,
                    pathLuceneOld,
                    StandardCopyOption.REPLACE_EXISTING
                );
                if (this.writer == null) this.writer = new IndexWriter(this.dirLucene, new IndexWriterConfig(analyzer));
                // Retry commit...
                this.writer.commit();
            }
            catch (IOException e2) {
                VocabularySearcher.logger.error("  ERROR: ABORTED - Cannot open Lucene directory (2)!", e2);
                throw e2;
            }
        }
        catch (IOException e) { // ...other IOException
            VocabularySearcher.logger.error("  ERROR: ABORTED - IO Error!", e);
            throw e;
        }

        try {
            this.reader = DirectoryReader.open(this.writer);
        }
        catch (IOException e) { // ...including CorruptIndexException
            VocabularySearcher.logger.error("  ERROR: ABORTED - Cannot create Lucene Directory Reader!");
            if ( Util.isDebugMode() || Util.isVerbose() ) VocabularySearcher.logger.error("  ERROR: ABORTED!", e);
            throw e;
        }
        this.searcher = new IndexSearcher(this.reader);

        if ( Util.isDebugMode() || Util.isVerbose(3) ) VocabularySearcher.logger.info("...created vocabulary searcher");
    }

    @Override
    public void importAndIndexVocabulary(String strPrefix, String strNamespace, String strLocation, Vocabulary.LocationType theLocType)
            throws VocabularyImportException, IOException {
        // Since no Project ID was given, use a Global ID and pass to regular method...
        this.importAndIndexVocabulary(strPrefix, strNamespace, strLocation, theLocType, GLOBAL_VOCABULARY_PLACE_HOLDER);
    }

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
        Query query = this.prepareQuery(strQueryVal, VocabularySearcher.CLASS_TYPE, strProjectID);
        return this.searchDocs(query, strProjectID);
    }

    @Override
    public List<SearchResultItem> searchProperties(String strQueryVal, String strProjectID)
            throws IOException {
        Query query = this.prepareQuery(strQueryVal, VocabularySearcher.PROPERTY_TYPE, strProjectID);
        return this.searchDocs(query, strProjectID);
    }

    private List<SearchResultItem> searchDocs(Query query, String strProjectID)
            throws IOException {
        TopDocs docs = this.searcher.search(query, getMaxDoc());
        return this.prepareSearchResults(docs, strProjectID);
    }

    @Override
    public void addTerm(RDFTNode node, String strNodeType, String strProjectID)
            throws IOException {
        this.indexRDFTNode(node, strNodeType, strProjectID);
        this.update();
    }

    @Override
    public void deleteTerm(RDFTNode node, String strNodeType, String strProjectID)
            throws IOException {
        this.deleteRDFTNode(node, strNodeType, strProjectID);
        this.update();
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
        // Get all documents from the global scope...
        TopDocs docs = this.getDocumentsOfProjectID(GLOBAL_VOCABULARY_PLACE_HOLDER);
        // Add all of them to the current project...
        this.addGlobalDocumentsToProject(docs, String.valueOf(liProjectID));
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
    public void synchronize(String strProjectID, Set<String> setNamespaces)
            throws IOException {
        Set<String> setAllNamespaces = this.getNamespacesOfProjectID(strProjectID);
        setAllNamespaces.removeAll(setNamespaces);
        if ( ! setAllNamespaces.isEmpty() ) {
            this.deleteNamespacesOfProjectID(strProjectID, setAllNamespaces);
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
    private void deleteTerms(String strPrefix, String strProjectID)
            throws IOException {
        if ( strProjectID == null || strProjectID.isEmpty() ) {
            throw new RuntimeException("Project ID is missing!");
        }

        BooleanQuery termsQuery =
            new BooleanQuery.Builder().
                add(TYPE_QUERY, Occur.SHOULD).
                add(new TermQuery(new Term(Util.gstrProject, strProjectID)), Occur.MUST).
                add(new TermQuery(new Term(Util.gstrPrefix, strPrefix)), Occur.MUST).
                build();

        this.writer.deleteDocuments(termsQuery);
    }

    private void indexTerms(String strProjectID, List<RDFTClass> classes, List<RDFTProperty> properties)
            throws IOException {
        for (RDFTClass klass : classes) {
            this.indexRDFTNode(klass, VocabularySearcher.CLASS_TYPE, strProjectID);
        }
        for (RDFTProperty prop : properties) {
            this.indexRDFTNode(prop, VocabularySearcher.PROPERTY_TYPE, strProjectID);
        }
        this.update();
    }

    private void deleteRDFTNode(RDFTNode node, String strNodeType, String strProjectID)
            throws IOException {
         if ( strProjectID == null || strProjectID.isEmpty() ) {
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
            return;
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

        if ( Util.isDebugMode() ) VocabularySearcher.logger.info("Indexing: ");

        BooleanQuery termsQuery =
            new BooleanQuery.Builder().
                add(TYPE_QUERY, Occur.SHOULD).
                add(new TermQuery(new Term(Util.gstrProject,     strProjectID)), Occur.SHOULD).
                add(new TermQuery(new Term(Util.gstrIRI,         strIRI)),       Occur.MUST).
                add(new TermQuery(new Term(Util.gstrLabel,       strLabel)),     Occur.MUST).
                add(new TermQuery(new Term(Util.gstrDescription, strDesc)),      Occur.MUST).
                add(new TermQuery(new Term(Util.gstrPrefix,      strPrefix)),    Occur.MUST).
                add(new TermQuery(new Term(Util.gstrNamespace,   strNamespace)), Occur.MUST).
                add(new TermQuery(new Term(Util.gstrLocalPart,   strLocalPart)), Occur.MUST).
                add(new TermQuery(new Term(Util.gstrType,        strNodeType)),  Occur.MUST).
                build();

        this.writer.deleteDocuments(termsQuery);
    }

    private void indexRDFTNode(RDFTNode node, String strNodeType, String strProjectID)
            throws IOException {
        /*
         * Create a new lucene document to store and index the related content
         * for an RDF Node.
         */
        Document doc = new Document();

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
            return;
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

        if ( Util.isDebugMode() ) VocabularySearcher.logger.info("Indexing: ");

        // NOTES:
        //  TextField is indexed and analyzed
        //  StringField is indexed but not analyzed
        //  StoredField is not indexed

        doc.add( new StoredField( Util.gstrIRI,         strIRI ) );
        doc.add( new TextField(   Util.gstrLabel,       strLabel,      Field.Store.YES) );
        doc.add( new TextField(   Util.gstrDescription, strDesc,       Field.Store.YES) );
        doc.add( new StringField( Util.gstrPrefix,      strPrefix,     Field.Store.YES) );
        doc.add( new StoredField( Util.gstrNamespace,   strNamespace ) );
        doc.add( new TextField(   Util.gstrLocalPart,   strLocalPart,  Field.Store.YES) );
        // From Node Type (Class or Property)...
        doc.add( new StringField( Util.gstrType,        strNodeType,   Field.Store.YES ) );
        // From Project ID...
        doc.add( new StringField( Util.gstrProject,     strProjectID,  Field.Store.NO ) );

        this.deleteRDFTNode(node, strNodeType, strProjectID);
        this.writer.addDocument(doc);
    }

    private Query prepareQuery(String strQueryVal, String strNodeType, String strProjectID)
            throws IOException {
        BooleanQuery.Builder qbuilderResult =
            new BooleanQuery.Builder().
                add(new TermQuery(new Term(Util.gstrProject, strProjectID)), Occur.MUST).
                add(new TermQuery(new Term(Util.gstrType, strNodeType)), Occur.MUST);

        if (strQueryVal != null && strQueryVal.strip().length() > 0) {
            StandardAnalyzer analyzer = new StandardAnalyzer();
            if (strQueryVal.indexOf(":") == -1) { // ...just a "prefix"...
                // The Query:
                // -----------------------------------------
                // "project" : strProjectID AND              \ These two are above
                // "type" : strType AND                      /
                // ( "prefix" : strQueryVal* OR              \
                //   "localPart" : OR 0..n termAttrib* OR    | These four are below
                //   "description" : OR 0..n termAttrib* OR  |
                //   "label" : OR 0..n termAttrib* )         /

                //
                // Add "Prefix" part...
                //
                BooleanQuery.Builder qbuilderPrefix =
                    new BooleanQuery.Builder().
                        add(new WildcardQuery(new Term(Util.gstrPrefix, strQueryVal + "*")), Occur.SHOULD);

                //
                // Add "localPart" parts...
                //
                TokenStream stream = analyzer.tokenStream(Util.gstrLocalPart, new StringReader(strQueryVal));
                stream.reset();

                // Get the TermAttribute from the TokenStream...
                CharTermAttribute termAttrib =
                    (CharTermAttribute) stream.addAttribute(CharTermAttribute.class);

                // For each token, do a wildcard search on each term (via CharTermAttribute)...
                while ( stream.incrementToken() ) {
                    qbuilderPrefix.
                        add(new WildcardQuery(
                                new Term(Util.gstrLocalPart, termAttrib.toString() + "*")
                            ),
                            Occur.SHOULD
                        );
                }
                stream.close();
                stream.end();

                //
                // Add "description" parts...
                //
                stream = analyzer.tokenStream(Util.gstrDescription, new StringReader(strQueryVal));
                stream.reset();

                // Get the TermAttribute from the TokenStream...
                termAttrib =
                    (CharTermAttribute) stream.addAttribute(CharTermAttribute.class);

                // For each token, do a wildcard search on each term (via CharTermAttribute)...
                while ( stream.incrementToken() ) {
                    qbuilderPrefix.
                        add(new WildcardQuery(
                                new Term(Util.gstrDescription, termAttrib.toString() + "*")
                            ),
                            Occur.SHOULD
                        );
                }
                stream.close();
                stream.end();

                //
                // Add "label" parts...
                //
                stream = analyzer.tokenStream(Util.gstrLabel, new StringReader(strQueryVal));
                stream.reset();

                // Get the TermAttribute from the TokenStream...
                termAttrib =
                    (CharTermAttribute) stream.addAttribute(CharTermAttribute.class);

                // For each token, do a wildcard search on each term (via CharTermAttribute)...
                while ( stream.incrementToken() ) {
                    qbuilderPrefix.
                        add(new WildcardQuery(
                                new Term(Util.gstrLabel, termAttrib.toString() + "*")
                            ),
                            Occur.SHOULD
                        );
                }
                stream.close();
                stream.end();

                qbuilderResult.add(qbuilderPrefix.build(), Occur.MUST);
            }
            else { // ..."prefix:localPart"...
                // The Query:
                // -----------------------------------
                // "project" : strProjectID AND        \ These two are above
                // "type" : type AND                   /
                // "prefix" : strPrefix AND            \ These two are below
                // ( localPart : OR 0..n termAttrib* ) /

                //
                // Add "Prefix" part...
                //
                String strPrefix = strQueryVal.substring(0, strQueryVal.indexOf(":"));
                qbuilderResult.add(new TermQuery(new Term(Util.gstrPrefix, strPrefix)), Occur.MUST);

                //
                // Add "localPart" parts...
                //
                String strLocalPart = strQueryVal.substring(strQueryVal.indexOf(":") + 1);
                if ( ! strLocalPart.isEmpty() ) {
                    BooleanQuery.Builder queryLocalPart = new BooleanQuery.Builder();

                    TokenStream stream =
                        analyzer.tokenStream(Util.gstrLocalPart, new StringReader(strLocalPart));
                    stream.reset();

                    // Get the TermAttribute from the TokenStream...
                    CharTermAttribute termAttrib =
                        (CharTermAttribute) stream.addAttribute(CharTermAttribute.class);

                    // For each token, do a wildcard search on each term (via CharTermAttribute)...
                    while ( stream.incrementToken() ) {
                        // NOTE: On the stream increment, the associated termAttrib is updated...
                        queryLocalPart.
                            add(new WildcardQuery(
                                    new Term(Util.gstrLocalPart, termAttrib.toString() + "*")
                                ),
                                Occur.SHOULD
                            );
                    }
                    stream.end();
                    stream.close();

                    qbuilderResult.add(queryLocalPart.build(), Occur.MUST);
                }
            }
            analyzer.close();
        }
        return qbuilderResult.build();
    }

    private List<SearchResultItem> prepareSearchResults(TopDocs docs, String strProjectID)
            throws IOException {
        List<SearchResultItem> results = new ArrayList<SearchResultItem>();
        for (ScoreDoc sdoc : docs.scoreDocs) {
            Document doc = this.searcher.storedFields().document(sdoc.doc);

            String [] astrLoader = new String[6];
            astrLoader[RDFTNode.iIRI]       = doc.get(Util.gstrIRI);
            astrLoader[RDFTNode.iLabel]     = doc.get(Util.gstrLabel);
            astrLoader[RDFTNode.iDesc]      = doc.get(Util.gstrDescription);
            astrLoader[RDFTNode.iPrefix]    = doc.get(Util.gstrPrefix);
            astrLoader[RDFTNode.iNamespace] = doc.get(Util.gstrNamespace);
            astrLoader[RDFTNode.iLocalPart] = doc.get(Util.gstrLocalPart);

            // Clean up erronious documents...
            if (astrLoader[RDFTNode.iIRI].startsWith("[object Object]") ) {
                RDFTNode node = new RDFTNode(astrLoader);
                this.deleteRDFTNode(node, VocabularySearcher.CLASS_TYPE, strProjectID);
                this.deleteRDFTNode(node, VocabularySearcher.PROPERTY_TYPE, strProjectID);

                continue;
            }
            results.
                add(
                    new SearchResultItem(
                        astrLoader[RDFTNode.iIRI],
                        astrLoader[RDFTNode.iLabel],
                        astrLoader[RDFTNode.iDesc],
                        astrLoader[RDFTNode.iPrefix],
                        astrLoader[RDFTNode.iNamespace],
                        astrLoader[RDFTNode.iLocalPart]
                    )
                );
        }

        return results;
    }

    private void addGlobalDocumentsToProject(TopDocs docs, String strProjectID)
            throws IllegalArgumentException, IOException {
        // The TopDocs are documents with the Global (Non-Project related) Vocabulary.
        // These docs are "copied" for use in the specified project.
        // See the calling function: addPredefinedVocabulariesToProject()

        // Set new Project ID for "copied" documents ( just like in indexRDFTNode() )...
        IndexableField fieldProjectID = new StringField(Util.gstrProject, strProjectID, Field.Store.NO);

        // Iterate through the Global documents...
        for (ScoreDoc sdoc : docs.scoreDocs) {
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
            docProject.removeField(Util.gstrProject);
            docProject.add(fieldProjectID);

            // Store and index the new project document...
            this.writer.addDocument(docProject);
        }
    }

    private TopDocs getDocumentsOfProjectID(String strProjectID)
            throws IOException {
        // Query for Project ID...
        Query query = new TermQuery(new Term(Util.gstrProject, strProjectID));
        return searcher.search( query, this.getMaxDoc() );
    }

    private Set<String> getNamespacesOfProjectID(String strProjectID)
            throws IOException {
        // Query for Project ID...
        Set<String> setNamespaces = new HashSet<String>();
        Query query = new TermQuery(new Term(Util.gstrProject, strProjectID));
        TopDocs docs =  searcher.search( query, this.getMaxDoc() );
        for (ScoreDoc sdoc : docs.scoreDocs) {
            Document doc = this.searcher.storedFields().document(sdoc.doc);
            setNamespaces.add( doc.get(Util.gstrPrefix) );
        }
        return setNamespaces;
    }

    private void deleteNamespacesOfProjectID(String strProjectID, Set<String> setDelete)
            throws IOException {
        if ( strProjectID == null || strProjectID.isEmpty() ) {
            throw new RuntimeException("Project ID is missing!");
        }

        BooleanQuery.Builder qbuilderNamespaces = new BooleanQuery.Builder();
        for (String strPrefix : setDelete) {
            qbuilderNamespaces.
                add( new TermQuery( new Term(Util.gstrPrefix, strPrefix) ), Occur.SHOULD );
        }

        BooleanQuery queryDelete =
            new BooleanQuery.Builder().
                add(TYPE_QUERY, Occur.SHOULD).
                add( new TermQuery( new Term(Util.gstrProject, strProjectID) ), Occur.MUST ).
                add(qbuilderNamespaces.build(), Occur.SHOULD).
                build();

        this.writer.deleteDocuments(queryDelete);
    }

    private int getMaxDoc() {
        return reader.maxDoc() > 0 ? reader.maxDoc() : 100000;
    }
}
