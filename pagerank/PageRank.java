import java.util.*;
import java.io.*;
import java.io.FileWriter;
public class PageRank {


    /**  
     *   Maximal number of documents. We're assuming here that we
     *   don't have more docs than we can keep in main memory.
     */
    final static int MAX_NUMBER_OF_DOCS = 2000000;

    /**
     *   Mapping from document names to document numbers.
     */
    HashMap<String,Integer> docNumber = new HashMap<String,Integer>();

    /**
     *   Mapping from document numbers to document names
     */
    String[] docName = new String[MAX_NUMBER_OF_DOCS];

    /**  
     *   A memory-efficient representation of the transition matrix.
     *   The outlinks are represented as a HashMap, whose keys are 
     *   the numbers of the documents linked from.<p>
     *
     *   The value corresponding to key i is a HashMap whose keys are 
     *   all the numbers of documents j that i links to.<p>
     *
     *   If there are no outlinks from i, then the value corresponding 
     *   key i is null.
     */
    HashMap<Integer,HashMap<Integer,Boolean>> link = new HashMap<Integer,HashMap<Integer,Boolean>>();

    /**
     *   The number of outlinks from each node.
     */
    int[] out = new int[MAX_NUMBER_OF_DOCS];

    /**
     *   The probability that the surfer will be bored, stop
     *   following links, and take a random jump somewhere.
     */
    final static double BORED = 0.15;

    /**
     *   Convergence criterion: Transition probabilities do not 
     *   change more that EPSILON from one iteration to another.
     */
    final static double EPSILON = 0.0001;

       
    /* --------------------------------------------- */

	int doc_size;
    public PageRank( String filename ) {
	int noOfDocs = readDocs( filename );
	this.doc_size = noOfDocs;
	//iterate( noOfDocs, 1000 );
    // MC1(noOfDocs);
	// MC2(noOfDocs, 1);
		// MC4(noOfDocs, 1);
		//MC5(noOfDocs);
		//bestMC();
    }


    /* --------------------------------------------- */


    /**
     *   Reads the documents and fills the data structures. 
     *
     *   @return the number of documents read.
     */
    int readDocs( String filename ) {
	int fileIndex = 0;
	try {
	    System.err.print( "Reading file... " );
	    BufferedReader in = new BufferedReader( new FileReader( filename ));
	    String line;
	    while ((line = in.readLine()) != null && fileIndex<MAX_NUMBER_OF_DOCS ) {
		int index = line.indexOf( ";" );
		String title = line.substring( 0, index );
		Integer fromdoc = docNumber.get( title );
		//  Have we seen this document before?
		if ( fromdoc == null ) {	
		    // This is a previously unseen doc, so add it to the table.
		    fromdoc = fileIndex++;
		    docNumber.put( title, fromdoc );
		    docName[fromdoc] = title;
		}
		// Check all outlinks.
		StringTokenizer tok = new StringTokenizer( line.substring(index+1), "," );
		while ( tok.hasMoreTokens() && fileIndex<MAX_NUMBER_OF_DOCS ) {
		    String otherTitle = tok.nextToken();
		    Integer otherDoc = docNumber.get( otherTitle );
		    if ( otherDoc == null ) {
			// This is a previousy unseen doc, so add it to the table.
			otherDoc = fileIndex++;
			docNumber.put( otherTitle, otherDoc );
			docName[otherDoc] = otherTitle;
		    }
		    // Set the probability to 0 for now, to indicate that there is
		    // a link from fromdoc to otherDoc.
		    if ( link.get(fromdoc) == null ) {
			link.put(fromdoc, new HashMap<Integer,Boolean>());
		    }
		    if ( link.get(fromdoc).get(otherDoc) == null ) {
			link.get(fromdoc).put( otherDoc, true );
			out[fromdoc]++;
		    }
		}
	    }
	    if ( fileIndex >= MAX_NUMBER_OF_DOCS ) {
		System.err.print( "stopped reading since documents table is full. " );
	    }
	    else {
		System.err.print( "done. " );
	    }
	}
	catch ( FileNotFoundException e ) {
	    System.err.println( "File " + filename + " not found!" );
	}
	catch ( IOException e ) {
	    System.err.println( "Error reading file " + filename );
	}
	System.err.println( "Read " + fileIndex + " number of documents" );
	return fileIndex;
    }


    /* --------------------------------------------- */


    /*
     *   Chooses a probability vector a, and repeatedly computes
     *   aP, aP^2, aP^3... until aP^i = aP^(i+1).
     */
     ArrayList<PageRankDocument> iterate( int numberOfDocs, int maxIterations ) {
		double [] pr = new double[numberOfDocs];
		Arrays.fill(pr,(double) BORED/numberOfDocs);
		double diff = 0;
		int loops = 0;
		double[] newpr = new double[numberOfDocs];

		while (loops <= maxIterations) {
			newpr = new double[numberOfDocs];
			Arrays.fill(newpr, BORED/numberOfDocs);
			for (int i = 0; i < numberOfDocs; i++) {
				if (link.get(i)!=null) {
					for (Map.Entry<Integer, Boolean> entry : link.get(i).entrySet()) {
						if (entry.getValue()){
							newpr[entry.getKey()] += (pr[i] / out[i])*(1-BORED);
						}
					}
				}else{
					for (int j = 0; j < numberOfDocs; j++) {
						newpr[j] += (pr[i] / numberOfDocs)* (1-BORED);
					}
				}
			}
			diff = 0;
			for (int i = 0; i < numberOfDocs; i++) {
				diff += Math.abs(newpr[i] - pr[i]);
			}

			if (diff < EPSILON) {
				break;
			}
			pr = newpr;
			loops++;
		}



		System.out.println("Iterations: " + loops);
		ArrayList<PageRankDocument> res = new ArrayList<PageRankDocument>();

		for (int i = 0; i < numberOfDocs; i++) {
			res.add(new PageRankDocument(docName[i], newpr[i]));
		}
		Collections.sort(res);
		for (int i = 0; i < 30; i++) {
			System.out.printf("%s\n", res.get(i));
		}
		//writeToFile(res, "pagerank.txt");

		return res;
	}

	void writeToFile(ArrayList<PageRankDocument> res, String filename) {
		try {
			FileWriter myWriter = new FileWriter(filename);
			for (int i = 0; i < res.size(); i++) {
				myWriter.write(docNumber.get(res.get(i).name) + " " + res.get(i).rank + "\n");
			}
			myWriter.close();
		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
	}



	public class PageRankDocument implements Comparable<PageRankDocument> {
		String name;
		double rank;

		public PageRankDocument(String name, double rank) {
			this.name = name;
			this.rank = rank;
		}

		public int compareTo(PageRankDocument other) {
			if (this.rank < other.rank) {
				return 1;
			} else if (this.rank > other.rank) {
				return -1;
			} else {
				return 0;
			}
		}

		public String toString() {
			return name + " " + rank;
		}
	}



	ArrayList<PageRankDocument> MC1(int N){
		// How many times do we want to run the simulation
		Random random = new Random();
		int current;
		double[] count = new double[doc_size];;
		double stop = BORED * 100;
		int random_tmp;


		for (int i = 0; i < N; i++) {
			current = random.nextInt(doc_size);
			while(true){
				if (random.nextInt(100) < stop) {	// 0-99
					// bored
					break;
				}
				// Dangling node, jump to a random page (sink node)
				if (out[current] == 0) {
					current = random.nextInt(doc_size);
					continue;
				}else{

					random_tmp = random.nextInt(out[current]);
					Integer tmp = (Integer) link.get(current).keySet().toArray()[random_tmp];
					current = tmp.intValue();
				}
			}
			count[current]++;

		}
		ArrayList<PageRankDocument> res = new ArrayList<PageRankDocument>();
		for (int i = 0; i < doc_size; i++) {
			res.add(new PageRankDocument(docName[i], count[i]/N));
		}
		Collections.sort(res);
	/*	for (int i = 0; i < 30; i++) {
			System.out.printf("%s\n", res.get(i));
		}
		writeToFile(res, "pagerankMC1.txt");*/

		return res;
	}
	ArrayList<PageRankDocument> MC2(int N){
		// How many times do we want to run the simulation
		Random random = new Random();
		int m = N / this.doc_size;
		int current;
		double[] count = new double[this.doc_size];
		double stop = BORED * 100;
		int random_tmp;


		for (int i = 0; i < m ; i++) {
			for (int j = 0; j < this.doc_size; j++) {
				current = j;
				while(true){
					if (random.nextInt(100) < stop) {	// 0-99
					// bored
					break;
					}

					// Dangling node, jump to a random page (sink node)
					if (out[current] == 0) {
						current = random.nextInt(this.doc_size);
						continue;
					}else{
						random_tmp = random.nextInt(out[current]);
						Integer tmp = (Integer) link.get(current).keySet().toArray()[random_tmp];
						current = tmp.intValue();
					}

				}
				count[current]++;
			}
		}
		ArrayList<PageRankDocument> res = new ArrayList<PageRankDocument>();
		for (int i = 0; i < this.doc_size; i++) {
			res.add(new PageRankDocument(docName[i], count[i]/N));
		}
		Collections.sort(res);
		/*for (int i = 0; i < 30; i++) {
			System.out.printf("%s\n", res.get(i));
		}
		writeToFile(res, "pagerankMC2.txt");*/
		return res;
	}

	ArrayList<PageRankDocument> MC4( int N){
		Random random = new Random();
		int m = N / this.doc_size;
		int current;
		double[] count = new double[this.doc_size];;
		double stop = BORED * 100;
		int random_tmp;

		double total_walks = 0;


		for (int i = 0; i < m ; i++) {
			for (int j = 0; j < this.doc_size; j++) {
				current = j;
				while(true){
					if (random.nextInt(100) < stop) {	// 0-99
						// bored
						break;
					}
					total_walks++;
					count[current]++;
					// Dangling node, jump to a random page (sink node)
					if (out[current] == 0) {
						break;
					}else{

						random_tmp = random.nextInt(out[current]);
						Integer tmp = (Integer) link.get(current).keySet().toArray()[random_tmp];
						current = tmp.intValue();
					}


				}

			}
		}
		ArrayList<PageRankDocument> res = new ArrayList<PageRankDocument>();
		for (int i = 0; i < this.doc_size; i++) {
			res.add(new PageRankDocument(docName[i], count[i]/total_walks));
		}
		Collections.sort(res);
		for (int i = 0; i < 30; i++) {
			System.out.printf("%s\n", res.get(i));
		}
/*
		writeToFile(res, "pagerankMC4.txt");
*/

		return res;

	}
	 ArrayList<PageRankDocument> MC5(int N){
		Random random = new Random();
		int current;
		double[] count = new double[this.doc_size];;
		double stop = BORED * 100;
		int random_tmp;

		double total_walks = 0;


		for (int i = 0; i < N; i++) {
			current = random.nextInt(this.doc_size);
			while(true){
				if (random.nextInt(100) < stop) {	// 0-99
				// bored
				break;
				}
				total_walks++;
				count[current]++;

				// Dangling node, jump to a random page (sink node)
				if (out[current] == 0) {
					break;
				}else{

					random_tmp = random.nextInt(out[current]);
					Integer tmp = (Integer) link.get(current).keySet().toArray()[random_tmp];
					current = tmp.intValue();
				}


			}
		}
		ArrayList<PageRankDocument> res = new ArrayList<PageRankDocument>();
		for (int i = 0; i < this.doc_size; i++) {
			res.add(new PageRankDocument(docName[i], count[i]/total_walks));
		}
		Collections.sort(res);
		for (int i = 0; i < 30; i++) {
			System.out.printf("%s\n", res.get(i));
		}
		// writeToFile(res, "pagerankMC5.txt");

		return res;
	}


	void bestMC() throws IOException{
		int[] N = {this.doc_size, 5*this.doc_size, 10*this.doc_size, 20*this.doc_size, 30*this.doc_size};


		double[][] results = new double[4][N.length];
		for (int j = 0; j < N.length; j++) {
			results[0][j] = 0;
			results[1][j] = 0;
			results[2][j] = 0;
			results[3][j] = 0;

			ArrayList<PageRankDocument> real = iterate(this.doc_size, N[j]);
			ArrayList<PageRankDocument> res1 = MC1(N[j]);
			ArrayList<PageRankDocument> res2 = MC2(N[j]);
			ArrayList<PageRankDocument> res4 = MC4(N[j]);
			ArrayList<PageRankDocument> res5 = MC5(N[j]);
			for (int i = 0; i < 30; i++) {
				results[0][j] += Math.pow((real.get(i).rank - res1.get(i).rank), 2);
				results[1][j] += Math.pow((real.get(i).rank - res2.get(i).rank), 2);
				results[2][j] += Math.pow((real.get(i).rank - res4.get(i).rank), 2);
				results[3][j] += Math.pow((real.get(i).rank - res5.get(i).rank), 2);
			}

		}
		plotMultipleSeries(N, results);

	}



	void plotMultipleSeries(int[] N, double[][] results) throws IOException{
		File file = new File("pagerank_best.txt");
		FileWriter writer = new FileWriter(file);

		for (int j = 0; j < results[0].length; j++) {
			writer.write(N[j]+ " " +results[0][j] + " " + results[1][j] + " " + results[2][j] + " " + results[3][j] + "\n");
		}
		writer.close();

	}

    /* --------------------------------------------- */


    public static void main( String[] args )  throws IOException{
	if ( args.length != 1 ) {
	    System.err.println( "Please give the name of the link file" );
	}
	else {
	    PageRank ob = new PageRank( args[0] );
		ob.iterate(ob.doc_size, 1000);
		// ob.MC4(ob.doc_size*10);
		// ob.bestMC();
	}
    }
}