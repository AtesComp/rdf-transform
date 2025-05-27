/*
 *  Interface IVocabularySearcher
 *
 *  A Vocabulary Searcher interface use by classes requiring
 *  vocabulary search capability.
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

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.jena.sparql.core.DatasetGraph;
//import org.apache.jena.rdf.model.Model;

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
    public void importAndIndexVocabulary(String strPrefix, String strNamespace, String strLocation, Vocabulary.LocationType theLocType)
                    throws VocabularyImportException, IOException;

    /*
     * Import the vocabulary from URL using namespace and assign the prefix to it...
     *   This vocabulary is to be used (searched) only with a project (strProjectID).
     * @param strPrefix
     * @param strNamespace
     * @param strFetch
     * @param loctype
     * @param strProjectID
     * @throws VocabularyImportException
     * @throws IOException
     */
    public void importAndIndexVocabulary(String strPrefix, String strNamespace, String strLocation, Vocabulary.LocationType theLocType,
                                            String strProjectID)
                    throws VocabularyImportException, IOException;
    /*
     * Import the vocabulary from a repository using namespace and assign the prefix to it...
     *   This vocabulary is to be used (searched) only with a project (strProjectID).
     * @param strPrefix
     * @param strNamespace
     * @param strFetch
     * @param theDSGraph
     * @param strProjectID
     * @throws VocabularyImportException
     * @throws IOException
     */
    public void importAndIndexVocabulary(String strPrefix, String strNamespace, String strLocation, DatasetGraph theDSGraph,
                                            String strProjectID)
                    throws VocabularyImportException, IOException;

    public List<SearchResultItem> searchClasses(String strClass, String strProjectID)
                    throws IOException;

    public List<SearchResultItem> searchProperties(String strProperty, String strProjectID)
                    throws IOException;

    public void addTerm(RDFTNode node, String strNodeType, String strProjectID)
                    throws IOException;

    public void deleteVocabularySetTerms(Set<Vocabulary> setVocabToRemove, String strProjectID)
                    throws IOException;

    public void deleteVocabularyTerms(String strVocab, String strProjectID)
                    throws IOException;

    public void addPredefinedVocabulariesToProject(long liProjectID)
                    throws IllegalArgumentException, IOException;

    public void update()
                    throws IOException;

    public void synchronize(String strProjectID, Set<String> setNamespaces)
                    throws IOException;
}
