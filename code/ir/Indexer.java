/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.io.*;
import java.util.*;
import java.nio.charset.*;


/**
 *   Processes a directory structure and indexes all PDF and text files.
 */
public class Indexer{

    /** The index to be built up by this Indexer. */
    Index index;

    /** K-gram index to be built up by this Indexer */
    KGramIndex kgIndex;

    /** The next docID to be generated. */
    private int lastDocID = 0;

    private int doc_id = 0;
    /** The patterns matching non-standard words (e-mail addresses, etc.) */
    String patterns_file;

   //  HashMap<Integer, HashMap<String, Integer>> word_count = new HashMap<Integer, HashMap<String, Integer>>();

    HashMap<Integer, HashSet<String>> words = new HashMap<Integer, HashSet<String>>();
    HashMap<String, Integer> word_count = new HashMap<>();
    HashMap<Integer, Double> file_length = new HashMap<Integer, Double>();

    // HashMap<String, Integer> token_count = new HashMap<String, Integer>();



    /* ----------------------------------------------- */


    /** Constructor */
    public Indexer( Index index, KGramIndex kgIndex, String patterns_file ) {
        this.index = index;
        this.kgIndex = kgIndex;
        this.patterns_file = patterns_file;
    }


    /** Generates a new document identifier as an integer. */
    private int generateDocID() {
        return lastDocID++;
    }



    /**
     *  Tokenizes and indexes the file @code{f}. If <code>f</code> is a directory,
     *  all its files and subdirectories are recursively processed.
     */
    public void processFiles( File f, boolean is_indexing ) {
        // do not try to index fs that cannot be read
        if (is_indexing) {
            if (f.canRead()) {
                if (f.isDirectory()) {
                    String[] fs = f.list();
                    // an IO error could occur
                    if (fs != null) {
                        for (int i = 0; i < fs.length; i++) {
                            processFiles(new File(f, fs[i]), is_indexing);
                        }
                    }
                } else {
                    // First register the document and get a docID
                    int docID = generateDocID();
                    if (docID % 1000 == 0) System.err.println("Indexed " + docID + " files");
                    try {
                        Reader reader = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8);
                        Tokenizer tok = new Tokenizer(reader, true, false, true, patterns_file);
                        int offset = 0;
                        while (tok.hasMoreTokens()) {
                            String token = tok.nextToken();
                            insertIntoIndex(docID, token, offset++);
                        }
                        index.docNames.put(docID, f.getPath());
                        index.docLengths.put(docID, offset);
                        reader.close();
                    } catch (IOException e) {
                        System.err.println("Warning: IOException during indexing.");
                    }
                }
            }
        }
    }

    public void read(File f, boolean is_indexing) {
        if (f.canRead()) {
            if (f.isDirectory()) {
                String[] fs = f.list();
                // an IO error could occur
                if (fs != null) {
                    for (int i = 0; i < fs.length; i++) {
                        read(new File(f, fs[i]), is_indexing);
                    }
                }
            } else {
                int docID = doc_id++;

                if (docID % 1000 == 0) System.err.println("read " + docID + " files");

                try {
                    Reader reader = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8);
                    Tokenizer tok = new Tokenizer(reader, true, false, true, patterns_file);

                    while (tok.hasMoreTokens()) {
                        String token = tok.nextToken();
                        if (word_count.containsKey(token)) {
                            word_count.put(token, word_count.get(token) + 1);
                        } else {
                            word_count.put(token, 1);
                        }
                    }
                    file_length.put(docID, get_length());
                    word_count.clear();

                    reader.close();
                } catch (IOException e) {
                    System.out.println(e);
                    System.err.println("Warning: IOException during indexing.");
                }
            }

        }
    }

    double get_length() {
        double length = 0;
        for (String word : word_count.keySet()) {
            PostingsList postingsList = index.getPostings(word);
            double idf = Math.log((double) index.docNames.size() / postingsList.size());
            double tf = word_count.get(word);
            length += Math.pow(tf*idf, 2);
        }
        return Math.sqrt(length);
    }

    void write_file_length() {
        try {
            File file = new File("file_length.txt");
            if (file.exists()){
                return;
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            for (int docID : file_length.keySet()) {
                writer.write(docID + " " + file_length.get(docID) + "\n");
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    HashMap<Integer, Double> read_file_length() {
        HashMap<Integer, Double> id_length_map = new HashMap<Integer, Double>();
        try {
            File file = new File("file_length.txt");
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tmp = line.trim().split(" ");
                id_length_map.put(Integer.parseInt(tmp[0]), Double.parseDouble(tmp[1]));
            }
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        return id_length_map;
    }
    /* ----------------------------------------------- */


    /**
     *  Indexes one token.
     */
    public void insertIntoIndex( int docID, String token, int offset ) {
        index.insert( token, docID, offset );
        if (kgIndex != null)
            kgIndex.insert(token);
    }
}

