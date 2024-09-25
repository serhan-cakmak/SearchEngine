/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

/**
 *  Searches an index for results of a query.
 */
public class Searcher {

    /** The index to be searched by this Searcher. */
    Index index;

    /** The k-gram index to be searched by this Searcher */
    KGramIndex kgIndex;
    
    /** Constructor */
    public Searcher( Index index, KGramIndex kgIndex ) {
        this.index = index;
        this.kgIndex = kgIndex;
        initPageRankings();

    }
    HashMap<Integer, Double> pageRankings = new HashMap<Integer, Double>();

    HashMap<Integer, Double> doc_lengths = new HashMap<Integer, Double>();
    void initPageRankings(){
        try {
            String filename = "PageRank_new.txt";
            java.io.BufferedReader in = new java.io.BufferedReader( new java.io.FileReader( filename ));
            String line;
            while ((line = in.readLine()) != null) {
                String[] tmp = line.trim().split(" ");
                pageRankings.put(Integer.parseInt(tmp[0]), Double.parseDouble(tmp[1]));
            }
        } catch ( IOException e ) {
            e.printStackTrace();
        }

    }

        /**
     *  Searches the index for postings matching the query.
     *  @return A postings list representing the result of the query.
     */
    public PostingsList search( Query query, QueryType queryType, RankingType rankingType, NormalizationType normType ) {
        if (query.queryterm.size() == 0) {
            return null;
        }
        /*HashMap<String, Integer> query_vec = new HashMap<String, Integer>();*/
        ArrayList<PostingsList> allPostings = new ArrayList<PostingsList>();
        ArrayList<Double> weights = new ArrayList<Double>();

        for (Query.QueryTerm queryTerm : query.queryterm) {
            allPostings.add(index.getPostings(queryTerm.term));
            weights.add(queryTerm.weight);
        }
        if (queryType == QueryType.RANKED_QUERY) {
            return ranked_search(allPostings, rankingType, pageRankings, normType, weights);
        }

        if (allPostings.size() <= 1) {
            return allPostings.get(0);
        }
        /**
         * I am checking the intersection between pairs of postings lists.
         * I modified the right term (p1, p2) (p2) to keep the track of the result of the intersection.
         * */
        if (queryType == QueryType.INTERSECTION_QUERY) {
            for (int i = 0; i < allPostings.size() - 1; i++) {
                allPostings.set(i + 1, intersect(allPostings.get(i), allPostings.get(i + 1)));
            }
            return allPostings.get(allPostings.size() - 1);

        } else if (queryType == QueryType.PHRASE_QUERY) {
            for (int i = 0; i < allPostings.size() - 1; i++) {
                allPostings.set(i + 1, positional_intersection(allPostings.get(i), allPostings.get(i + 1), 1));
            }
            return allPostings.get(allPostings.size() - 1);
        }

        return null;
    }

    public PostingsList ranked_search(ArrayList<PostingsList> allPostings, RankingType rankingType, HashMap<Integer, Double> pageRankings, NormalizationType normType
            , ArrayList<Double> weights) {
        PostingsList answer = new PostingsList();
        if (allPostings.size() == 0) {
            return answer;
        }
        double score = 0;

        for (int i = 0; i < allPostings.size(); i++) {
            for (int j = 0; j < allPostings.get(i).size(); j++) {
                PostingsEntry postingsEntry = allPostings.get(i).get(j);

                if (RankingType.TF_IDF == rankingType) {
                    if (normType == NormalizationType.NUMBER_OF_WORDS){
                        score = calculate_score(postingsEntry, allPostings.get(i).size(), index.docLengths.get(postingsEntry.docID));
                    } else if (normType == NormalizationType.EUCLIDEAN) {
                        score = calculate_score(postingsEntry, allPostings.get(i).size(), doc_lengths.get(postingsEntry.docID)) ;
                    }
                    score *= weights.get(i);
                    answer.add_scored(postingsEntry.docID, score,0);
                } else if (RankingType.PAGERANK == rankingType) {
                    if (postingsEntry.score ==0){
                        score = pageRankings.get(postingsEntry.docID);
                        answer.add_scored(postingsEntry.docID, score,0);
                    }
                }else if (RankingType.COMBINATION == rankingType) {
                    if (normType == NormalizationType.NUMBER_OF_WORDS){
                        score = calculate_score(postingsEntry, allPostings.get(i).size(), index.docLengths.get(postingsEntry.docID));
                    } else if (normType == NormalizationType.EUCLIDEAN) {
                        score = calculate_score(postingsEntry, allPostings.get(i).size(), doc_lengths.get(postingsEntry.docID)) ;
                    }
                    score += 100 * pageRankings.get(postingsEntry.docID);
                    score *= weights.get(i);
                    answer.add_scored(postingsEntry.docID, score,0);
                }
            }
        }
        answer.sort_list();
        return answer;

    }
    public double calculate_score(PostingsEntry postingsEntry, double df, double len_d) {
        double tf = postingsEntry.offsets.size();
        double idf = Math.log(index.docNames.size() / df);
        return (tf * idf) / len_d; // tf_idf
    }



    /**
     * Because we added the documents and offsets in a sorted manner, we can use this algorithm.
     */
    public PostingsList intersect(PostingsList p1, PostingsList p2){
        PostingsList answer = new PostingsList();
        int i = 0, j = 0;
        if (p1 == null || p2 == null) {
            return answer;
        }
        while (i != p1.size()  && j != p2.size()) {
            if (p1.get(i).docID == p2.get(j).docID) {
                answer.add(p1.get(i).docID, 0);
                i++;
                j++;
            } else if (p1.get(i).docID < p2.get(j).docID) {
                i++;
            } else {
                j++;
            }
        }
        return answer;
    }


    /**
     * Actually I would have used skip pointers to increase the speed of the algorithm.
     * But I think this approach is also fast enough.
     *
     * And also I added input k to care of also the more general cases. K is the maximum distance between the offsets.
     *
     * But in the final version I just looked for only the ordered paired of the offsets.
     * Ex: 0 [1, 10] | 0 [2, 9]
     * It will only get the 0 [1,2] case
     */

    public PostingsList positional_intersection(PostingsList p1, PostingsList p2, int k) {
        PostingsList answer = new PostingsList();
        if (p1 == null || p2 == null) {
            return answer;
        }
        int i = 0, j = 0;
        int offset1 , offset2;
        while (i != p1.size()  && j != p2.size()) {
            if (p1.get(i).docID == p2.get(j).docID) {
                offset1 = 0;
                offset2 = 0;
                while (offset1 != p1.get(i).offsets.size() && offset2 != p2.get(j).offsets.size()) {
                    int diff = p2.get(j).offsets.get(offset2) - p1.get(i).offsets.get(offset1) ;
                    if (diff <= k && diff >= 0) {
                        //offset 2 because we need the second one
                        answer.add(p1.get(i).docID, p2.get(j).offsets.get(offset2));
                        offset1++;
                        offset2++;
                    }
                    /**
                     * I would actually use the if statement below to get better result benefiting the spatial locality.
                     * */
                   /* if (Math.abs(p1.get(i).offsets.get(offset1) - p2.get(j).offsets.get(offset2)) <= k) {
                        //offset 2 because we need the second one
                        answer.add(p1.get(i).docID, p2.get(j).offsets.get(offset2));
                        offset1++;
                        offset2++;
                    } */else if (p1.get(i).offsets.get(offset1) < p2.get(j).offsets.get(offset2)) {
                        offset1++;
                    } else {
                        offset2++;
                    }
                }
                i++;
                j++;
            } else if (p1.get(i).docID < p2.get(j).docID) {
                i++;
            } else {
                j++;
            }
        }

        return answer;
    }
}