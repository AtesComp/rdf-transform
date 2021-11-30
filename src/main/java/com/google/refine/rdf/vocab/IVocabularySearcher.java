package com.google.refine.rdf.vocab;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.lucene.index.CorruptIndexException;
import org.eclipse.rdf4j.repository.Repository;

public interface IVocabularySearcher {

	/*
	 * Import the vocabulary from URL using namespace and assign the prefix to it...
	 *   This vocabulary is not limited to a specific project--it's a Global vocabulary.
	 * @param strPrefix
	 * @param strNamespace
	 * @param strFetchURL
	 * @throws VocabularyImportException
	 * @throws VocabularyIndexException
	 * @throws PrefixExistException
	 * @throws CorruptIndexException
	 * @throws IOException
	 */
	public void importAndIndexVocabulary(String strPrefix, String strNamespace, String strFetchURL)
					throws VocabularyImportException, VocabularyIndexException, PrefixExistException,
							CorruptIndexException, IllegalArgumentException, IOException;

	/*
	 * Import the vocabulary from URL using namespace and assign the prefix to it...
	 *   This vocabulary is to be used (searched) only with a project (strProjectID).
	 * @param strPrefix
	 * @param strNamespace
	 * @param strFetchURL
	 * @param strProjectID
	 * @throws VocabularyImportException
	 * @throws VocabularyIndexException
	 * @throws PrefixExistException
	 * @throws CorruptIndexException
	 * @throws IOException
	 */
	public void importAndIndexVocabulary(String strPrefix, String strNamespace, String strFetchURL,
											String strProjectID)
					throws VocabularyImportException, VocabularyIndexException, PrefixExistException,
							CorruptIndexException, IllegalArgumentException, IOException;
	/*
	 * Import the vocabulary from a repository using namespace and assign the prefix to it...
	 *   This vocabulary is to be used (searched) only with a project (strProjectID).
	 * @param strPrefix
	 * @param strNamespace
	 * @param repository
	 * @param strProjectID
	 * @throws VocabularyImportException
	 * @throws VocabularyIndexException
	 * @throws PrefixExistException
	 * @throws CorruptIndexException
	 * @throws IOException
	 */
	public void importAndIndexVocabulary(String strPrefix, String strNamespace, Repository repository,
											String strProjectID)
					throws VocabularyImportException, VocabularyIndexException, PrefixExistException,
							CorruptIndexException, IllegalArgumentException, IOException;

	public List<SearchResultItem> searchClasses(String str, String strProjectID)
					throws IOException;

	public List<SearchResultItem> searchProperties(String str, String strProjectID)
					throws IOException;

	public void deleteTermsOfVocabs(Set<Vocabulary> toRemove,String strProjectID)
					throws CorruptIndexException, IOException;

	public void deleteTermsOfVocab(String vocabName, String strProjectID)
					throws CorruptIndexException, IOException;

	public void addPredefinedVocabulariesToProject(long liProjectID)
					throws VocabularyIndexException, IOException;

	public void update()
					throws CorruptIndexException, IOException;

	public void synchronize(String strProjectID, Set<String> prefixes)
					throws IOException;
}
