package com.google.refine.rdf.vocab.imp;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.StandardCopyOption;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
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
//import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.StringField;

import com.google.refine.rdf.Util;
import com.google.refine.rdf.vocab.IVocabularySearcher;
import com.google.refine.rdf.vocab.PrefixExistException;
import com.google.refine.rdf.vocab.RDFNode;
import com.google.refine.rdf.vocab.RDFSClass;
import com.google.refine.rdf.vocab.RDFSProperty;
import com.google.refine.rdf.vocab.SearchResultItem;
import com.google.refine.rdf.vocab.Vocabulary;
import com.google.refine.rdf.vocab.VocabularyImportException;
import com.google.refine.rdf.vocab.VocabularyImporter;
import com.google.refine.rdf.vocab.VocabularyIndexException;

import org.eclipse.rdf4j.repository.Repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VocabularySearcher implements IVocabularySearcher {
	private final static Logger logger = LoggerFactory.getLogger("RDFT:VocabSearcher");

	public static final String LUCENE_DIR = "luceneIndex";
	public static final String LUCENE_DIR_OLD = "luceneIndex_OLD";

	private static final String CLASS_TYPE = "class";
	private static final String PROPERTY_TYPE = "property";
	// "type":vocabulary AND "projectId":projectId AND "name":name
	// ("type": (class OR property) ) AND "projectId":strProjectID AND "prefix":prefix
	private static final BooleanQuery TYPE_QUERY =
			new BooleanQuery.Builder().
				add(new TermQuery(new Term("type", CLASS_TYPE)), Occur.SHOULD).
				add(new TermQuery(new Term("type", PROPERTY_TYPE)), Occur.SHOULD).
				build();

	// The project ID is always a number. It is safe to use this placeholder...
	private static final String GLOBAL_VOCABULARY_PLACE_HOLDER = "g";

	private Directory dirLucene;
	private IndexWriter writer;
	private IndexSearcher searcher;
	private IndexReader reader;

	public VocabularySearcher(File dir) throws IOException {
		if ( Util.isVerbose(3) )
			logger.info("Creating vocabulary searcher...");

		this.dirLucene = FSDirectory.open( new File(dir, LUCENE_DIR).toPath() );
		Analyzer analyzer = new StandardAnalyzer();
		// IndexWriterConfig conf = new IndexWriterConfig(a);

		try {
			this.writer = new IndexWriter(this.dirLucene, new IndexWriterConfig(analyzer));
			this.writer.commit();
		}
		catch (org.apache.lucene.index.IndexFormatTooOldException e) {
			Files.move(
				new File(dir, LUCENE_DIR).toPath(),
				new File(dir, LUCENE_DIR_OLD).toPath(),
				StandardCopyOption.REPLACE_EXISTING
			);
			this.writer = new IndexWriter(this.dirLucene, new IndexWriterConfig(analyzer));
			this.writer.commit();
		}
        //this.reader = DirectoryReader.open( FSDirectory.open( new File(dir, LUCENE_DIR).toPath() ) );
        this.reader = DirectoryReader.open(this.dirLucene);
        this.searcher = new IndexSearcher(this.reader);
		if ( Util.isVerbose(3) )
			logger.info("...created vocabulary searcher");
	}

	@Override
	public void importAndIndexVocabulary(String strPrefix, String strNamespace, String strFetchURL)
			throws VocabularyImportException, VocabularyIndexException, PrefixExistException,
					CorruptIndexException, IllegalArgumentException, IOException {
		// Since no Project ID was given, use a Global ID and pass to regular method... 
		this.importAndIndexVocabulary(strPrefix, strNamespace, strFetchURL, GLOBAL_VOCABULARY_PLACE_HOLDER);
	}

	@Override
	public void importAndIndexVocabulary(String strPrefix, String strNamespace, String strFetchURL,
											String strProjectID)
			throws VocabularyImportException, VocabularyIndexException, PrefixExistException,
					CorruptIndexException, IllegalArgumentException, IOException {
		VocabularyImporter importer = new VocabularyImporter(strPrefix, strNamespace);
		List<RDFSClass> classes = new ArrayList<RDFSClass>();
		List<RDFSProperty> properties = new ArrayList<RDFSProperty>();

		// Import classes & properties from Namespace at URL...
		if ( Util.isVerbose(3) )
			logger.info(
				"Import And Index vocabulary " + strPrefix + ": <" + strNamespace + "> " +
				"from " + strFetchURL);
		importer.importVocabulary(strFetchURL, classes, properties);
		this.indexTerms(strProjectID, classes, properties);
	}

	@Override
	public void importAndIndexVocabulary(String strPrefix, String strNamespace, Repository repository,
											String strProjectID)
			throws VocabularyImportException, VocabularyIndexException, PrefixExistException,
					CorruptIndexException, IllegalArgumentException, IOException {
		VocabularyImporter importer = new VocabularyImporter(strPrefix, strNamespace);
		List<RDFSClass> classes = new ArrayList<RDFSClass>();
		List<RDFSProperty> properties = new ArrayList<RDFSProperty>();

		// Import classes & properties from Namespace in Repository...
		if ( Util.isVerbose(3) )
			logger.info("Import And Index vocabulary " + strPrefix + ": <" + strNamespace + ">");
		importer.importVocabulary(repository, classes, properties);
		this.indexTerms(strProjectID, classes, properties);
	}

	@Override
	public List<SearchResultItem> searchClasses(String strQueryVal, String strProjectID)
			throws IOException {
		Query query = prepareQuery(strQueryVal, CLASS_TYPE, strProjectID);
		return this.searchDocs(query);
	}

	@Override
	public List<SearchResultItem> searchProperties(String strQueryVal, String strProjectID)
			throws IOException {
		Query query = this.prepareQuery(strQueryVal, PROPERTY_TYPE, strProjectID);
		return this.searchDocs(query);
	}

	private List<SearchResultItem> searchDocs(Query query)
			throws IOException {
		TopDocs docs = this.searcher.search(query, getMaxDoc());
		return this.prepareSearchResults(docs);
	}

	@Override
	public void deleteTermsOfVocabs(Set<Vocabulary> toRemove, String strProjectID)
			throws CorruptIndexException, IOException {
		for (Vocabulary v : toRemove) {
			this.deleteTerms(v.getPrefix(), strProjectID);
		}
		update();
	}

	@Override
	public void addPredefinedVocabulariesToProject(long liProjectID)
			throws VocabularyIndexException, IOException {
		// Get all documents from the global scope...
		TopDocs docs = this.getDocumentsOfProjectID(GLOBAL_VOCABULARY_PLACE_HOLDER);
		// Add all of them to the current project...
		this.addGlobalDocumentsToProject(docs, String.valueOf(liProjectID));
		this.update();
	}

	@Override
	public void update() throws CorruptIndexException, IOException {
		this.writer.commit();
		// TODO: This shouldn't be required but it is not working without it...or is it???
		//this.reader.close();
		//this.reader = DirectoryReader.open(this.dirLucene);
		//this.searcher = new IndexSearcher(this.reader);
	}

	@Override
	public void synchronize(String strProjectID, Set<String> sstrPrefixes) throws IOException{
		Set<String> sstrAllPrefixes = getPrefixesOfProjectID(strProjectID);
		sstrAllPrefixes.removeAll(sstrPrefixes);
		if (!sstrAllPrefixes.isEmpty()) {
			this.deletePrefixesOfProjectID(strProjectID, sstrAllPrefixes);
		}
		this.update();
	}

	@Override
	public void deleteTermsOfVocab(String strPrefix, String strProjectID) throws CorruptIndexException, IOException {
		this.deleteTerms(strPrefix, strProjectID);
		this.update();
	}

	/*
	 * Private methods
	 */
	private void deleteTerms(String strPrefix, String strProjectID)
			throws CorruptIndexException, IOException {
		if (strProjectID == null || strProjectID.isEmpty()) {
			throw new RuntimeException("projectId is null");
		}

		BooleanQuery termsQuery =
			new BooleanQuery.Builder().
				add(TYPE_QUERY, Occur.MUST).
				add(new TermQuery(new Term("projectId", strProjectID)), Occur.MUST).
				add(new TermQuery(new Term("prefix", strPrefix)), Occur.MUST).
				build();

		this.writer.deleteDocuments(termsQuery);
	}

	private void indexTerms(String strProjectID, List<RDFSClass> classes, List<RDFSProperty> properties)
			throws CorruptIndexException, IllegalArgumentException, IOException {
		for (RDFSClass c : classes) {
			this.indexRDFNode(c, CLASS_TYPE, strProjectID);
		}
		for (RDFSProperty p : properties) {
			this.indexRDFNode(p, PROPERTY_TYPE, strProjectID);
		}
		this.update();
	}

	private void indexRDFNode(RDFNode node, String strNodeType, String strProjectID)
			throws CorruptIndexException, IllegalArgumentException, IOException {
		/*
		 * Create a new lucene document to store and index the related content
		 * for an RDF Node.
		 */ 
		Document doc = new Document();

		// From Node...
		//
        // 	IRI         str   required
        // 	Label       str   copy of IRI if not given
        // 	Description null
        // 	Prefix      null
        // 	Namespace   null  generated from IRI if not given
        // 	LocalPart   null  generated from IRI if not given
		String strLabel = node.getLabel();
		if (strLabel == null)
			strLabel = "";

		String strDesc = node.getDescription();
		if (strDesc == null)
			strDesc = "";

		String strPrefix = node.getPrefix();
		if (strPrefix == null)
			strPrefix = "";

		String strNamespace = node.getNamespace();
		if (strNamespace == null)
			strNamespace = "";

		String strLocalPart = node.getLocalPart();
		if (strLocalPart == null)
			strLocalPart = "";

		doc.add( new StoredField( "iri",         node.getIRI() ) );
		doc.add( new TextField(   "label",       strLabel,     Field.Store.YES) );
		doc.add( new TextField(   "description", strDesc,      Field.Store.YES) );
		doc.add( new StringField( "prefix",      strPrefix,    Field.Store.YES) );
		doc.add( new StoredField( "namespace",   strNamespace ) );
		doc.add( new TextField(   "localPart",   strLocalPart, Field.Store.YES) );
		// From Node Type (Class or Property)...
		doc.add( new StringField( "type",        strNodeType,  Field.Store.YES ) );
		// From Project ID...
		doc.add( new StringField( "projectId",   strProjectID, Field.Store.NO ) );

        this.writer.addDocument(doc);
	}

	private Query prepareQuery(String strQueryVal, String strType, String strProjectID)
			throws IOException {
		BooleanQuery.Builder qbuilderResult =
			new BooleanQuery.Builder().
                add(new TermQuery(new Term("projectId", strProjectID)), Occur.MUST).
                add(new TermQuery(new Term("type", strType)), Occur.MUST);

		if (strQueryVal != null && strQueryVal.strip().length() > 0) {
			StandardAnalyzer analyzer = new StandardAnalyzer();
			if (strQueryVal.indexOf(":") == -1) { // ...just a "prefix"...
				// The Query:
				// -----------------------------------------
				// "projectId" : strProjectID AND            \ These two are above
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
                        add(new WildcardQuery(new Term("prefix", strQueryVal + "*")), Occur.SHOULD);

				//
				// Add "localPart" parts...
				//
				TokenStream stream = analyzer.tokenStream("localPart", new StringReader(strQueryVal));
				stream.reset();

				// Get the TermAttribute from the TokenStream...
				CharTermAttribute termAttrib =
					(CharTermAttribute) stream.addAttribute(CharTermAttribute.class);

				// For each token, do a wildcard search on each term (via CharTermAttribute)...
				while ( stream.incrementToken() ) {
					qbuilderPrefix.
						add(new WildcardQuery(
								new Term("localPart", termAttrib.toString() + "*")
							),
							Occur.SHOULD
						);
				}
				stream.close();
				stream.end();

				//
				// Add "description" parts...
				//
				stream = analyzer.tokenStream("description", new StringReader(strQueryVal));
				stream.reset();

				// Get the TermAttribute from the TokenStream...
				termAttrib =
					(CharTermAttribute) stream.addAttribute(CharTermAttribute.class);

				// For each token, do a wildcard search on each term (via CharTermAttribute)...
				while ( stream.incrementToken() ) {
					qbuilderPrefix.
						add(new WildcardQuery(
								new Term("description", termAttrib.toString() + "*")
							),
							Occur.SHOULD
						);
				}
				stream.close();
				stream.end();

				//
				// Add "label" parts...
				//
				stream = analyzer.tokenStream("label", new StringReader(strQueryVal));
				stream.reset();

				// Get the TermAttribute from the TokenStream...
				termAttrib =
					(CharTermAttribute) stream.addAttribute(CharTermAttribute.class);

				// For each token, do a wildcard search on each term (via CharTermAttribute)...
				while ( stream.incrementToken() ) {
					qbuilderPrefix.
						add(new WildcardQuery(
								new Term("label", termAttrib.toString() + "*")
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
				// "projectId" : strProjectID AND      \ These two are above
				// "type" : type AND                   /
				// "prefix" : strPrefix AND            \ These two are below
				// ( localPart : OR 0..n termAttrib* ) /

				//
				// Add "Prefix" part...
				//
				String strPrefix = strQueryVal.substring(0, strQueryVal.indexOf(":"));
				qbuilderResult.add(new TermQuery(new Term("prefix", strPrefix)), Occur.MUST);

				//
				// Add "localPart" parts...
				//
				String strLocalPart = strQueryVal.substring(strQueryVal.indexOf(":") + 1);
				if ( ! strLocalPart.isEmpty() ) {
					BooleanQuery.Builder queryLocalPart = new BooleanQuery.Builder();

					TokenStream stream =
						analyzer.tokenStream("localPart", new StringReader(strLocalPart));
					stream.reset();

					// Get the TermAttribute from the TokenStream...
					CharTermAttribute termAttrib =
						(CharTermAttribute) stream.addAttribute(CharTermAttribute.class);

					// For each token, do a wildcard search on each term (via CharTermAttribute)...
					while ( stream.incrementToken() ) {
						// NOTE: On the stream increment, the associated termAttrib is updated...
						queryLocalPart.
							add(new WildcardQuery(
									new Term("localPart", termAttrib.toString() + "*")
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

	private List<SearchResultItem> prepareSearchResults(TopDocs docs)
			throws CorruptIndexException, IOException {
		List<SearchResultItem> results = new ArrayList<SearchResultItem>();
		for (ScoreDoc sdoc : docs.scoreDocs) {
			Document doc = this.searcher.doc(sdoc.doc);
			String strIRI       = doc.get("iri");
			String strLabel     = doc.get("label");
			String strDesc      = doc.get("description");
			String strPrefix    = doc.get("prefix");
			String strNamespace = doc.get("namespace");
			String strLocalPart = doc.get("localPart");

			SearchResultItem item = new SearchResultItem(strIRI, strLabel, strDesc, strPrefix, strNamespace, strLocalPart);
			results.add(item);
		}

		return results;
	}

	private void addGlobalDocumentsToProject(TopDocs docs, String strProjectID)
			throws CorruptIndexException, IOException {
		// The TopDocs are documents with the Global (Non-Project related) Vocabulary.
		// These docs are "copied" for use in the specified project.
		// See the calling function: addPredefinedVocabulariesToProject()

		// Set new Project ID for "copied" documents...
		IndexableField fieldProjectID = new StoredField("projectId", strProjectID);

		// Iterate through the Global documents...
		for (ScoreDoc sdoc : docs.scoreDocs) {
			// Get an existing Global document...
			Document docGlobal = this.searcher.doc(sdoc.doc);

			// Prepare a new project document...
			Document docProject = new Document();

			// Copy all fields to the project doc...
			List<IndexableField> fields = docGlobal.getFields();
			for (IndexableField field : fields) {
				docProject.add(field);
			}

			// Change the Global Project ID field to the specified Project ID field...
			docProject.removeField("projectId");
			docProject.add(fieldProjectID);

			// Store and index the new project document...
			this.writer.addDocument(docProject);
		}
	}

	private TopDocs getDocumentsOfProjectID(String strProjectID)
			throws IOException {
		// Query for "projectId"...
		Query query = new TermQuery(new Term("projectId", strProjectID));
		return searcher.search(query, getMaxDoc());
	}

	private Set<String> getPrefixesOfProjectID(String strProjectID)
			throws IOException {
		// Query for "projectId"...
		Set<String> prefixes = new HashSet<String>();
		Query query = new TermQuery(new Term("projectId", strProjectID));
		TopDocs docs =  searcher.search(query, getMaxDoc());
		for (ScoreDoc sdoc : docs.scoreDocs) {
			Document doc = searcher.doc(sdoc.doc);
			prefixes.add(doc.get("prefix"));
		}
		return prefixes;
	}

	private void deletePrefixesOfProjectID(String strProjectID, Set<String> toDelete)
			throws CorruptIndexException, IOException {
		if (strProjectID == null || strProjectID.isEmpty()) {
			throw new RuntimeException("ProjectID is null");
		}

		BooleanQuery.Builder qbuilderPrefixes = new BooleanQuery.Builder();
		for (String strPrefix : toDelete) {
			qbuilderPrefixes.
				add(new TermQuery(new Term("prefix", strPrefix)), Occur.SHOULD);
		}

		BooleanQuery queryDelete =
			new BooleanQuery.Builder().
				add(TYPE_QUERY, Occur.MUST).
				add(new TermQuery(new Term("projectId", strProjectID)), Occur.MUST).
				add(qbuilderPrefixes.build(), Occur.MUST).
				build();

		this.writer.deleteDocuments(queryDelete);
	}

	private int getMaxDoc() throws IOException {
		return reader.maxDoc() > 0 ? reader.maxDoc() : 100000;
	}
}
