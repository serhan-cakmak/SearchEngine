/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, KTH, 2018
 */  

package ir;

import java.io.*;
import java.util.*;
import java.nio.charset.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.Duration;
import java.time.Instant;


/*
 *   Implements an inverted index as a hashtable on disk.
 *   
 *   Both the words (the dictionary) and the data (the postings list) are
 *   stored in RandomAccessFiles that permit fast (almost constant-time)
 *   disk seeks. 
 *
 *   When words are read and indexed, they are first put in an ordinary,
 *   main-memory HashMap. When all words are read, the index is committed
 *   to disk.
 */
public class PersistentHashedIndex implements Index {

    /** The directory where the persistent index files are stored. */
    public static final String INDEXDIR = "./index";

    /** The dictionary file name */
    public static final String DICTIONARY_FNAME = "dictionary";

    /** The data file name */
    public static final String DATA_FNAME = "data";

    /** The terms file name */
    public static final String TERMS_FNAME = "terms";

    /** The doc info file name */
    public static final String DOCINFO_FNAME = "docInfo";

    /** The dictionary hash table on disk can fit this many entries. */
    public static final long TABLESIZE = 611953L;  // todo uncomment this
    //public static final long TABLESIZE = 95L;
    /** The dictionary hash table is stored in this file. */
    RandomAccessFile dictionaryFile;

    /** The data (the PostingsLists) are stored in this file. */
    RandomAccessFile dataFile;

    /** Pointer to the first free memory cell in the data file. */
    long free = 0L;

    /** The cache as a main-memory hash map. */
    HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();


    // ===================================================================

    /**
     *   A helper class representing one entry in the dictionary hashtable.
     */ 
    public class Entry {
        public String key;
        public long ptr;
        public String data;
        public int conflicts;
        public long hash;
    }


    // ==================================================================

    
    /**
     *  Constructor. Opens the dictionary file and the data file.
     *  If these files don't exist, they will be created. 
     */
    public PersistentHashedIndex() {
        try {
            dictionaryFile = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME, "rw" );
            dataFile = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME, "rw" );
        } catch ( IOException e ) {
            e.printStackTrace();
        }

        try {
            readDocInfo();
        } catch ( FileNotFoundException e ) {
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    /**
     *  Writes data to the data file at a specified place.
     *
     *  @return The number of bytes written.
     */ 
    int writeData( String dataString, long ptr ) {
        try {
            dataFile.seek( ptr ); 
            byte[] data = dataString.getBytes();
            dataFile.write( data );
            return data.length;
        } catch ( IOException e ) {
            e.printStackTrace();
            return -1;
        }
    }


    /**
     *  Reads data from the data file
     */ 
    String readData( long ptr, int size ) {
        try {

            dataFile.seek( ptr );
            byte[] buffer = new byte[size];
            dataFile.readFully( buffer );
            String number = new String( buffer );
            String size_str = number.split(" ")[0];
            size = Integer.parseInt(size_str);
            // skip the size of the data
            dataFile.seek(ptr + size_str.length());
            byte[] data = new byte[size];
            dataFile.readFully( data );

            return new String(data);
        } catch ( IOException e ) {
            e.printStackTrace();
            return null;
        }
    }


    // ==================================================================
    //
    //  Reading and writing to the dictionary file.

    /*
     *  Writes an entry to the dictionary hash table file. 
     *
     *  @param entry The key of this entry is assumed to have a fixed length
     *  @param ptr   The place in the dictionary file to store the entry
     */
    HashSet<Long> hashes = new HashSet<Long>();
    void writeEntry( Entry entry, long ptr ) {
        int collisions = 0;
        long hash = hash_func(entry.key);
        /**
         * This while loops checks if the calculated hash is already used in the dictionary file.
         * Some other token hashed to the same value, so we need to find a new place
         * for this token using linear probing technique.
         * */
        while (hashes.contains(hash)) {
            hash = (hash + 1) % TABLESIZE;
            collisions++;
        }
        hashes.add(hash);
       /* try {
            dictionaryFile.seek( hash * 40);
            byte[] data = (collisions + " " + entry.key + " " + ptr).getBytes();
            dictionaryFile.write( data );
        } catch ( IOException e ) {
            e.printStackTrace();
        }*/

        try {
            dictionaryFile.seek( hash * 40);
            byte[] data = (collisions + " " + entry.key.hashCode() + " " + ptr).getBytes();
            dictionaryFile.write( data );
        } catch ( IOException e ) {
            e.printStackTrace();
        }

    }

    /**
     *  Reads an entry from the dictionary file.
     *
     *  @param ptr The place in the dictionary file where to start reading.
     */
    Entry readEntry( long ptr ) {
        try {
            dictionaryFile.seek( ptr * 40 );
            byte[] data = new byte[40];

            dictionaryFile.readFully( data );
            String res =  new String(data);
            if (res.trim().isEmpty()) {
                return null;
            }
            res = res.trim();

            String[] res_arr = res.split(" ");

            Entry e = new Entry();
            // First element is the number of conflicts, second element is second hash value,
            // third element is the pointer to the data file
            e.conflicts = Integer.parseInt(res_arr[0]);
            /*e.key = res_arr[1];*/
            e.hash = Long.parseLong(res_arr[1]);
            /**
             * Preference explanation:
             *
             * Since there is no limit for token size and we need to get a fixed size data from the dictionary file
             * I just ignore the conflicts happening from very long tokens. Since it is a very rare case, in my opinion
             * it is not a problem. But theoretically, we can solve this problem by increasing the size of
             * the dictionary considering the trade of between dictionary file size and 100% success rate.
             *
             *
             * Another solution might be like the following:
             * Again we will calculate the hash of the token and get the pointer from the dictionary file.
             * But this instead of writing the token for tracking collisions to dictionary file,
             * I would write another hash function to control if it's the token that I am trying to find or not.
             * If it is not, I would continue to search for the token by incrementing the hash value by 1.
             *
             * But this solution is not perfect as well since two different tokens might have the same
             * hash value for both of these hash functions. And I might get the wrong search result.
             *
             * My preference is to get correct results every time even though it may rarely print
             * "This entry conflicts with another entry which exceeds its determined size." message.
             *
             * This two solutions resemble montecarlo and las vegas algorithms. My approach is like las vegas.
             *
             * @update: I have changed the approach in order to decrease the indexing time and increase the performance.
             * I noticed that even though this approach is not perfect, it is still better than the other approach
             * considering the probability of getting 2 hash codes from 2 different words is way unlikely compared to
             * the probabilty of getting a token whose length is more than 40 chars.
             *
             * */
            /*if (res_arr.length < 3) {
                System.out.println("This entry conflicts with another entry which exceeds its determined size.");
                return null;
            }*/

            // I chose 20 because the number that states how long the data, shouldn't be longer than 20 chars for Davis_wiki .
            e.data = readData(Long.parseLong( res_arr[2]), 20);
            return e;

        } catch ( IOException e ) {
            e.printStackTrace();
            return null;
        }
    }


    // ==================================================================

    /**
     *  Writes the document names and document lengths to file.
     *
     * @throws IOException  { exception_description }
     */
    private void writeDocInfo() throws IOException {
        FileOutputStream fout = new FileOutputStream( INDEXDIR + "/docInfo" );
        for ( Map.Entry<Integer,String> entry : docNames.entrySet() ) {
            Integer key = entry.getKey();
            String docInfoEntry = key + ";" + entry.getValue() + ";" + docLengths.get(key) + "\n";
            fout.write( docInfoEntry.getBytes() );
        }
        fout.close();
    }


    /**
     *  Reads the document names and document lengths from file, and
     *  put them in the appropriate data structures.
     *
     * @throws     IOException  { exception_description }
     */
    private void readDocInfo() throws IOException {
        File file = new File( INDEXDIR + "/docInfo" );
        FileReader freader = new FileReader(file);
        try ( BufferedReader br = new BufferedReader(freader) ) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(";");
                docNames.put( new Integer(data[0]), data[1] );
                docLengths.put( new Integer(data[0]), new Integer(data[2]) );
            }
        }
        freader.close();
    }


    /**
     *  Write the index to files.
     */
    public long hash_func(String word){
        return (word.hashCode() & Integer.MAX_VALUE) % TABLESIZE; // To solve the negative hash problem
    }

    public void writeIndex() {
        int collisions = 0;
        try {
            // Write the 'docNames' and 'docLengths' hash maps to a file
            writeDocInfo();

            for (Map.Entry<String, PostingsList> entry : index.entrySet()) {
                // token
                String key = entry.getKey();
                // postings list
                String value = entry.getValue().toString();
                //write to dictionary
                Entry e = new Entry();
                e.key = key;
                e.ptr = free;
                writeEntry(e, free);
                //write to data file
                int byte_size =  writeData(value, free);
                //update free
                free += byte_size;
            }

        } catch ( IOException e ) {
            e.printStackTrace();
        }
        System.err.println( collisions + " collisions." );
    }


    // ==================================================================


    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String token ) {
        Entry res;
        long hash = hash_func(token);
        long actual_hash = token.hashCode();
        while (true){
            res = readEntry(hash);
            if (res == null) {
                break;
            }
            /**
             * This if condition was checking the word directly before.
             *
             * */
            if (res.hash == actual_hash) {
                break;
            }
            hash = (hash + 1) % TABLESIZE;
        }

        PostingsList postingsList = new PostingsList();
        if (res == null) {
            return null;
        }

        String[] postings = res.data.split("\\*");

        /**
         * Tried to optimize the performance but after trying different approaches the performance didn't change.
         */
        for (String posting : postings) {
            posting = posting.trim();
            String[] data = posting.split(" ");
            //int docID = new Integer(data[0]);
            int docID = Integer.parseInt(data[0]);
            // double score = Double.parseDouble(data[1]);
            for (int i = 2; i < data.length; i++) {
                //int a =  new Integer(data[i]);
                postingsList.add(docID, Integer.parseInt(data[i]));
            }
        }


        return postingsList;
    }


    /**
     *  Inserts this token in the main-memory hashtable.
     */
    public void insert( String token, int docID, int offset ) {
        if (index.containsKey(token)) {
            PostingsList postingsList = index.get(token);
            // postingsList.add(docID, offset);
            postingsList.add(docID, offset);
            index.put(token, postingsList);
        } else {
            PostingsList postingsList = new PostingsList();
            /*postingsList.add(docID, offset);*/
            postingsList.add(docID, offset);
            index.put(token, postingsList);
        }
    }


    /**
     *  Write index to file after indexing is done.
     */
    public void cleanup() {
        System.err.println( index.keySet().size() + " unique words" );
        System.err.print( "Writing index to disk..." );
        writeIndex();
        System.err.println( "done!" );
    }
}
