package com.google.refine.rdf.model.vocab;

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
    		listVocabsCopy.add( new Vocabulary( entryVocab.getPrefix(), entryVocab.getNamespace() )
            );
    	}

    	return listVocabsCopy;
    }

}
