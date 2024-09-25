/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class PostingsList {
    
    /** The postings list */
    private ArrayList<PostingsEntry> list = new ArrayList<PostingsEntry>();
    public HashMap<String, Integer> docIDMap = new HashMap<String, Integer>();
    // hashset?



    /** Number of postings in this list. */
    public int size() {
    return list.size();
    }

    /** Returns the ith posting. */
    public PostingsEntry get( int i ) { return list.get( i );}


    public void add(int docID, int offset) {
        /**
         * I used hashmap to keep track of the index of the docID in the list. Otherwise, I would have to iterate
         * through the list to find the index of the docID.
         * */
        if (docIDMap.containsKey(docID+"")) {
            // already in the list
            int index = docIDMap.get(docID+"");
            list.get(index).offsets.add(offset);
            return;
        }
        // not found
        PostingsEntry postingsEntry = new PostingsEntry();
        postingsEntry.docID = docID;
        postingsEntry.offsets.add(offset);
        list.add(postingsEntry);
        docIDMap.put(docID+"", list.size()-1);

    }

    public void add_scored(int docID, double score, int offset) {
        /**
         * I used hashmap to keep track of the index of the docID in the list. Otherwise, I would have to iterate
         * through the list to find the index of the docID.
         * */
        if (docIDMap.containsKey(docID+"")) {
            // already in the list
            int index = docIDMap.get(docID+"");
            list.get(index).offsets.add(offset);
            /**
             * Just add the score cumulatively. It is like dot product since I just want to calculate the dot
             * product with the query and don't want to increase the score in some query like "zombie zombie zombie".
             * */
            list.get(index).score = list.get(index).score + score;
            return;
        }
        // not found
        PostingsEntry postingsEntry = new PostingsEntry();
        postingsEntry.docID = docID;
        postingsEntry.offsets.add(offset);
        postingsEntry.score = score;
        list.add(postingsEntry);
        docIDMap.put(docID+"", list.size()-1);
    }
    public void sort_list() {
        Collections.sort(list);
    }

    @Override
    public String toString() {
        String result = " ";
        for (int i = 0; i < list.size(); i++) {
            result += list.get(i).toString() + "*";
        }
        return result.length() + result + "-";
    }
}

