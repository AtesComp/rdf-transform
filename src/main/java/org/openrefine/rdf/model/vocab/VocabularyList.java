/*
 *  Class VocabularyList
 *
 *  A Vocabulary List class used to manage a list of vocabularies for an
 *  RDF Transform.
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

public class VocabularyList extends ArrayList<Vocabulary> {
    @Override
    public boolean add(Vocabulary vocab) {
        synchronized(this) {
            if ( ! this.containsPrefix( vocab.getPrefix() ) ) {
                return super.add(vocab);
            }
            return true; // already exists
        }
    }

    public Vocabulary findByPrefix(String strPrefix) {
        Iterator<Vocabulary> iterVocab = this.iterator();
        while ( iterVocab.hasNext() ) {
            Vocabulary vocab = iterVocab.next();
            if (vocab.getPrefix().equals(strPrefix)) {
                return vocab;
            }
        }
        return null;
    }

    public String findNamespaceByPrefix(String strPrefix) {
        Vocabulary vocab = this.findByPrefix(strPrefix);
        if (vocab != null) {
            return vocab.getNamespace();
        }
        return null;
    }

    public boolean containsPrefix(String strPrefix) {
        return (this.findByPrefix(strPrefix) != null);
    }

    public boolean removeByPrefix(String strPrefix) {
        synchronized(this) {
            Vocabulary vocab = this.findByPrefix(strPrefix);
            if (vocab != null) {
                return this.remove(vocab);
            }
            return false;
        }
    }

    public HashSet<String> getPrefixSet() {
        HashSet<String> setNamespaces = new HashSet<String>();
        Iterator<Vocabulary> iterVocab = this.iterator();
        while (iterVocab.hasNext()) {
            Vocabulary vocab = iterVocab.next();
            setNamespaces.add( vocab.getPrefix() );
        }
        return setNamespaces;
    }

    @Override
    public VocabularyList clone() {
        VocabularyList listVocabsCopy = new VocabularyList();

        for ( Vocabulary entryVocab : this ) {
            listVocabsCopy.add(
                new Vocabulary(
                    entryVocab.getPrefix(),
                    entryVocab.getNamespace(),
                    entryVocab.getLocation(),
                    entryVocab.getLocationType()
                )
            );
        }

        return listVocabsCopy;
    }

}
