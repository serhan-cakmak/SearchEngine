/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  


package ir;

import java.util.HashMap;
import java.util.Iterator;


/**
 *   Implements an inverted index as a Hashtable from words to PostingsLists.
 */
public class HashedIndex implements Index{


    /** The index as a hashtable. */
    private HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();


    /**
     *  Inserts this token in the hashtable.
     */
    public void insert( String token, int docID, int offset ) {
        if (index.containsKey(token)) {
            PostingsList postingsList = index.get(token);
            // postingsList.add(docID, offset);
            postingsList.add(docID, offset);
            index.put(token, postingsList);
        } else {
            PostingsList postingsList = new PostingsList();
            postingsList.add(docID, offset);
            index.put(token, postingsList);
        }

    }


    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String token ) {
        return index.get(token);
    }

    @Override
    public String toString() {
        return "HashedIndex{" +
                "index=" + index +
                '}';
    }

    /**
     *  No need for cleanup in a HashedIndex.
     */
    public void cleanup() {
    }
}
