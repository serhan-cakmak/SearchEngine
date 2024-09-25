/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Dmytro Kalpakchi, 2018
 */

package ir;

import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;


public class KGramIndex {

    /** Mapping from term ids to actual term strings */
    HashMap<Integer,String> id2term = new HashMap<Integer,String>();

    /** Mapping from term strings to term ids */
    HashMap<String,Integer> term2id = new HashMap<String,Integer>();

    /** Index from k-grams to list of term ids that contain the k-gram */
    HashMap<String,List<KGramPostingsEntry>> index = new HashMap<String,List<KGramPostingsEntry>>();

    /** The ID of the last processed term */
    int lastTermID = -1;

    /** Number of symbols to form a K-gram */
    int K = 3;

    public KGramIndex(int k) {
        K = k;
        if (k <= 0) {
            System.err.println("The K-gram index can't be constructed for a negative K value");
            System.exit(1);
        }
    }

    /** Generate the ID for an unknown term */
    private int generateTermID() {
        return ++lastTermID;
    }

    public int getK() {
        return K;
    }


    /**
     *  Get intersection of two postings lists
     */
    // todo it was private
    public List<KGramPostingsEntry> intersect(List<KGramPostingsEntry> p1, List<KGramPostingsEntry> p2) {
        List<KGramPostingsEntry> result = new ArrayList<KGramPostingsEntry>();
        int i = 0, j = 0;
        while ( i< p1.size() && j < p2.size() ) {
            if ( p1.get(i).tokenID == p2.get(j).tokenID ) {
                result.add(p1.get(i));
                i++;
                j++;
            } else if ( p1.get(i).tokenID < p2.get(j).tokenID ) {
                i++;
            } else {
                j++;
            }
        }
        return result;
    }
    HashSet<String> tokens = new HashSet<String>();


    /** Inserts all k-grams from a token into the index. */
    public void insert( String token ) {
        if (tokens.contains(token)) {
            return;
        }
        tokens.add(token);
        token = "^" + token + "$";
        generateTermID();
        term2id.put(token, lastTermID);
        id2term.put(lastTermID, token);
        for (int i = 0; i < token.length() - K + 1; i++) {
            String kgram = token.substring(i, i + K);
            if (!index.containsKey(kgram)) {
                index.put(kgram, new ArrayList<KGramPostingsEntry>());
            }
            List<KGramPostingsEntry> postings = index.get(kgram);
            // Add a new posting or update the existing "momo" case. In order not to add mo twice we need to check
            // the last added term id since it can be the same
            if (postings.isEmpty() || postings.get(postings.size() - 1).tokenID != lastTermID) {
                postings.add(new KGramPostingsEntry(lastTermID));
            }
            // may need the code below to add positions to the posting "momo" -> indexes 0 and 2.
            // postings.get(postings.size() - 1).addPosition(i);
        }

    }

    /** Get postings for the given k-gram */
    public List<KGramPostingsEntry> getPostings(String kgram) {
        List<KGramPostingsEntry> postings = index.get(kgram);
        return postings != null ? postings : new ArrayList<KGramPostingsEntry>();
    }

    /** Get id of a term */
    public Integer getIDByTerm(String term) {
        return term2id.get(term);
    }

    /** Get a term by the given id */
    public String getTermByID(Integer id) {
        return id2term.get(id);
    }

    public void displayWords(List<KGramPostingsEntry> postings){
        for (KGramPostingsEntry entry : postings) {
            System.out.println(getTermByID(entry.tokenID));
        }
    }

    private static HashMap<String,String> decodeArgs( String[] args ) {
        HashMap<String,String> decodedArgs = new HashMap<String,String>();
        int i=0, j=0;
        while ( i < args.length ) {
            if ( "-p".equals( args[i] )) {
                i++;
                if ( i < args.length ) {
                    decodedArgs.put("patterns_file", args[i++]);
                }
            } else if ( "-f".equals( args[i] )) {
                i++;
                if ( i < args.length ) {
                    decodedArgs.put("file", args[i++]);
                }
            } else if ( "-k".equals( args[i] )) {
                i++;
                if ( i < args.length ) {
                    decodedArgs.put("k", args[i++]);
                }
            } else if ( "-kg".equals( args[i] )) {
                i++;
                if ( i < args.length ) {
                    decodedArgs.put("kgram", args[i++]);
                }
            } else {
                System.err.println( "Unknown option: " + args[i] );
                break;
            }
        }
        return decodedArgs;
    }

    public static void main(String[] arguments) throws FileNotFoundException, IOException {
        HashMap<String,String> args = decodeArgs(arguments);

        int k = Integer.parseInt(args.getOrDefault("k", "3"));
        KGramIndex kgIndex = new KGramIndex(k);

        File f = new File(args.get("file"));
        Reader reader = new InputStreamReader( new FileInputStream(f), StandardCharsets.UTF_8 );
        Tokenizer tok = new Tokenizer( reader, true, false, true, args.get("patterns_file") );
        while ( tok.hasMoreTokens() ) {
            String token = tok.nextToken();
            kgIndex.insert(token);
        }

        String[] kgrams = args.get("kgram").split(" ");
        List<KGramPostingsEntry> postings = null;
        for (String kgram : kgrams) {
            if (kgram.length() != k) {
                System.err.println("Cannot search k-gram index: " + kgram.length() + "-gram provided instead of " + k + "-gram");
                System.exit(1);
            }

            if (postings == null) {
                postings = kgIndex.getPostings(kgram);
            } else {
                postings = kgIndex.intersect(postings, kgIndex.getPostings(kgram));
            }
        }
        if (postings == null) {
            System.err.println("Found 0 posting(s)");
        } else {
            int resNum = postings.size();
            System.err.println("Found " + resNum + " posting(s)");
            if (resNum > 10) {
                System.err.println("The first 10 of them are:");
                resNum = 10;
            }
            for (int i = 0; i < resNum; i++) {
                System.err.println(kgIndex.getTermByID(postings.get(i).tokenID));
            }
        }
    }
}
