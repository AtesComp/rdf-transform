package org.openrefine.rdf.model.vocab;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.repository.Repository;

public interface IVocabularySearcher {

    /*
     * Import the vocabulary from URL using namespace and assign the prefix to it...
     *   This vocabulary is not limited to a specific project--it's a Global vocabulary.
     * @param strPrefix
     * @param strNamespace
     * @param strFetchURL
     * @throws VocabularyImportException
     * @throws IOException
     */
    public void importAndIndexVocabulary(String strPrefix, String strNamespace, String strFetchURL)
                    throws VocabularyImportException, IOException;

    /*
     * Import the vocabulary from URL using namespace and assign the prefix to it...
     *   This vocabulary is to be used (searched) only with a project (strProjectID).
     * @param strPrefix
     * @param strNamespace
     * @param strFetchURL
     * @param strProjectID
     * @throws VocabularyImportException
     * @throws IOException
     */
    public void importAndIndexVocabulary(String strPrefix, String strNamespace, String strFetchURL,
                                            String strProjectID)
                    throws VocabularyImportException, IOException;
    /*
     * Import the vocabulary from a repository using namespace and assign the prefix to it...
     *   This vocabulary is to be used (searched) only with a project (strProjectID).
     * @param strPrefix
     * @param strNamespace
     * @param repository
     * @param strProjectID
     * @throws VocabularyImportException
     * @throws IOException
     */
    public void importAndIndexVocabulary(String strPrefix, String strNamespace, Repository theRepository,
                                            String strProjectID)
                    throws VocabularyImportException, IOException;

    public List<SearchResultItem> searchClasses(String strClass, String strProjectID)
                    throws IOException;

    public List<SearchResultItem> searchProperties(String strProperty, String strProjectID)
                    throws IOException;

    public void deleteTermsOfVocabs(Set<Vocabulary> setVocabToRemove, String strProjectID)
                    throws IOException;

    public void deleteTermsOfVocab(String strVocab, String strProjectID)
                    throws IOException;

    public void addPredefinedVocabulariesToProject(long liProjectID)
                    throws IOException;

    public void update()
                    throws IOException;

    public void synchronize(String strProjectID, Set<String> setNamespaces)
                    throws IOException;
}
