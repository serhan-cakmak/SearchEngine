/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.io.Serializable;

public class PostingsEntry implements Comparable<PostingsEntry>, Serializable {

    public int docID;
    public double score = 0;
    public ArrayList<Integer> offsets = new ArrayList<Integer>();
    // treeset? Actually I wouldn't use arraylist for this task but since the given documents and offsets are
    // created in a sequential manner we happen to get sorted list.


    /**
     *  PostingsEntries are compared by their score (only relevant
     *  in ranked retrieval).
     *
     *  The comparison is defined so that entries will be put in 
     *  descending order.
     */
    public int compareTo( PostingsEntry other ) {
       return Double.compare( other.score, score );
    }



    @Override
    public String toString() {
        String result = "";
        for (int i = 0; i < offsets.size(); i++) {
            result += " " + offsets.get(i) ;
        }
        return docID + " " + score + result ;
    }
}

