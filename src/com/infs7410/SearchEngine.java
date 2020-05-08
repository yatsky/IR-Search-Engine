package com.infs7410;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.Directory;

import java.io.IOException;
import java.nio.file.Paths;

import org.apache.lucene.store.FSDirectory;


public class SearchEngine {
    private static final String INDEX_DIR = "/home/thomas/INFS7410/qldWikiCore/data/index";
    public IndexSearcher indexSearcher;
    public Query normalQuery;
    public FuzzyQuery fuzzyQuery;
    public StandardAnalyzer analyzer = new StandardAnalyzer();
    private int maxNumResults;

    /**
     * Constructor
     * @param maxNumResults The maximum number of results to be returned after search.
     */
    SearchEngine(int maxNumResults){
        try{
            indexSearcher = createSearcher();
            this.maxNumResults = maxNumResults;
        } catch (IOException e){
            System.out.println(e.toString());
        }
    }

    public static void main(String[] args) throws Exception {

        SearchEngine se = new SearchEngine(10);
        //Search by title
        TopDocs topDocs = se.normalSearch("text", "University");
        System.out.println("Total Results: " + topDocs.totalHits);
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document doc = se.indexSearcher.doc(scoreDoc.doc);
            System.out.println((doc.get("baseUrl")));
//            System.out.println(String.format(doc.get("cat")));
        }

        TopDocs fuzzyDocs = se.fuzzySearch("title", "Queenslang");
        System.out.println("Fuzzy Search Total Results: " + fuzzyDocs.totalHits);
        for (ScoreDoc scoreDoc:fuzzyDocs.scoreDocs){
            Document doc = se.indexSearcher.doc(scoreDoc.doc);
            System.out.println(doc.get("title"));
        }

    }

    /**
     * Normal search using StandardAnalyzer
     * @param field The field to be searched against.
     * @param query The search query
     * @return hits, the top docs that are most similar to query
     * @throws Exception
     */
    public TopDocs normalSearch(String field, String query) throws Exception {
        QueryParser queryParser = new QueryParser(field, analyzer);
        normalQuery = queryParser.parse(query);
        TopDocs hits = indexSearcher.search(normalQuery, maxNumResults);
        return hits;
    }

    /**
     * Fuzzy search for any field and query
     * @param field The field to be searched in
     * @param query The query to be searched
     * @return hits, the top docs that are most similar to query
     * @throws Exception
     */
    public TopDocs fuzzySearch(String field, String query) throws Exception{
        Term term = new Term(field, query);
        fuzzyQuery = new FuzzyQuery(term); // maxEdits = 2, as a default
        TopDocs hits = indexSearcher.search(fuzzyQuery, maxNumResults);
        return hits;

    }


    /**
     * Highlight the query.
     * @param field_to_show, Decides which field to get text from, this is introduced to deal with text search.
     * @param searchQuery, the query to be searched.
     * @param analyzer, the analyzer
     * @param scoreDoc, score doc
     * @return An array of strings, of which some have been highlighted.
     * @throws IOException
     * @throws InvalidTokenOffsetsException
     */
    public String[] highlightQuery(String field_to_show, Query searchQuery, Analyzer analyzer,
                                   ScoreDoc scoreDoc)
    throws IOException, InvalidTokenOffsetsException{
        String[] frags;
        Formatter formatter = new SimpleHTMLFormatter();
        QueryScorer queryScorer= new QueryScorer(searchQuery);
        Highlighter highlighter = new Highlighter(formatter, queryScorer);
        Fragmenter fragmenter = new SimpleSpanFragmenter(queryScorer, 10);
        highlighter.setTextFragmenter(fragmenter);

        // process one doc
        // Crete token stream
        TokenStream stream = TokenSources.getAnyTokenStream(indexSearcher.getIndexReader(),scoreDoc.doc,
                field_to_show, analyzer);
        // get highlighted text fragments
        Document doc = indexSearcher.doc(scoreDoc.doc);
        // this will fail when field_to_show is title and actual field selected is text
        frags = highlighter.getBestFragments(stream, doc.get(field_to_show), 10);

        return frags;
    }

    /**
     * Create an Index Searcher
     * @return The created index searcher
     * @throws IOException
     */
    private static IndexSearcher createSearcher() throws IOException {
        Directory dir = FSDirectory.open(Paths.get(INDEX_DIR));
        IndexReader reader = DirectoryReader.open(dir);
        IndexSearcher indexSearcher = new IndexSearcher(reader);
        return indexSearcher;
    }

}
