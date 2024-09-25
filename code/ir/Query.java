/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.*;
import java.nio.charset.*;
import java.io.*;


/**
 *  A class for representing a query as a list of words, each of which has
 *  an associated weight.
 */
public class Query {

    /**
     *  Help class to represent one query term, with its associated weight. 
     */
    class QueryTerm {
        String term;
        double weight;
        QueryTerm( String t, double w ) {
            term = t;
            weight = w;
        }
    }

    /** 
     *  Representation of the query as a list of terms with associated weights.
     *  In assignments 1 and 2, the weight of each term will always be 1.
     */
    public ArrayList<QueryTerm> queryterm = new ArrayList<QueryTerm>();

    /**  
     *  Relevance feedback constant alpha (= weight of original query terms). 
     *  Should be between 0 and 1.
     *  (only used in assignment 3).
     */
    double alpha = 0.2;

    /**  
     *  Relevance feedback constant beta (= weight of query terms obtained by
     *  feedback from the user). 
     *  (only used in assignment 3).
     */
    double beta = 1 - alpha;
    
    
    /**
     *  Creates a new empty Query 
     */
    public Query() {
    }
    
    
    /**
     *  Creates a new Query from a string of words
     */
    public Query( String queryString  ) {
        StringTokenizer tok = new StringTokenizer( queryString );
        while ( tok.hasMoreTokens() ) {
            queryterm.add( new QueryTerm(tok.nextToken(), 1.0) );
        }    
    }
    
    
    /**
     *  Returns the number of terms
     */
    public int size() {
        return queryterm.size();
    }
    
    
    /**
     *  Returns the Manhattan query length
     */
    public double length() {
        double len = 0;
        for ( QueryTerm t : queryterm ) {
            len += t.weight; 
        }
        return len;
    }
    
    
    /**
     *  Returns a copy of the Query
     */
    public Query copy() {
        Query queryCopy = new Query();
        for ( QueryTerm t : queryterm ) {
            queryCopy.queryterm.add( new QueryTerm(t.term, t.weight) );
        }
        return queryCopy;
    }
    
    
    /**
     *  Expands the Query using Relevance Feedback
     *
     *  @param results The results of the previous query.
     *  @param docIsRelevant A boolean array representing which query results the user deemed relevant.
     *  @param engine The search engine object
     */
    public void relevanceFeedback( PostingsList results, boolean[] docIsRelevant, Engine engine ) {
        // Storing total word count of the relevant documents enable us to find the center of the relevant documents
        // by dividing the word count by the number of relevant documents (1 1 0) (2 1 1) -> (1.5 1 0.5)
        // Now we can apply rocchio algorithm to find the new query vector
        HashMap<String, Integer> word_count = new HashMap<String, Integer>();
        double numRelevant = 0;
        for (int i = 0; i < docIsRelevant.length; i++) {
            if (docIsRelevant[i]) {
                System.out.println("Document " + results.get(i).docID +" name: " + engine.index.docNames.get(results.get(i).docID) +" is relevant");
                word_count = readFile(engine.index.docNames.get(results.get(i).docID), word_count);
                numRelevant++;
            }
        }
        HashSet<String> unique_words = new HashSet<String>();
        for (QueryTerm t : queryterm) {
            if (word_count.containsKey(t.term)) {
                // Giving alpha to the original query terms
                t.weight = alpha * t.weight + beta * ((double) word_count.get(t.term) / numRelevant);
                unique_words.add(t.term);
            }
        }
        for (String word : word_count.keySet()) {
            if (!unique_words.contains(word)) {
                queryterm.add(new QueryTerm(word, beta * ((double) word_count.get(word) / numRelevant)));
            }
        }
    }

    /**
     * This function reads the file and returns modified version of the word count variable
     * */
    public HashMap<String, Integer> readFile(String fileName, HashMap<String, Integer> word_count){

        try {
            Reader reader = new InputStreamReader(new FileInputStream(fileName), StandardCharsets.UTF_8);
            Tokenizer tok = new Tokenizer(reader, true, false, true, "patterns.txt");

            while (tok.hasMoreTokens()) {
                String token = tok.nextToken();
                if (word_count.containsKey(token)) {
                    word_count.put(token, word_count.get(token) + 1);
                } else {
                    word_count.put(token, 1);
                }
            }
            reader.close();
        } catch (IOException e) {
            System.out.println(e);
            System.err.println("Warning: IOException during indexing.");
        }
        return word_count;
    }
}


