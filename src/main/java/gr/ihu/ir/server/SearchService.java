package gr.ihu.ir.server;

import gr.ihu.ir.common.Fields;
import gr.ihu.ir.common.SearchRequest;
import gr.ihu.ir.common.SearchResponse;
import gr.ihu.ir.common.SearchResult;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryVisitor;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Η «καρδιά» του Query Server: παίρνει ένα {@link SearchRequest}, το εκτελεί στο
 * αντεστραμμένο ευρετήριο και επιστρέφει {@link SearchResponse}.
 * Δεν ξέρει τίποτα για sockets — έτσι δοκιμάζεται εύκολα και μένει μικρή.
 */
public class SearchService {

    private final IndexSearcher searcher;
    private final Analyzer analyzer;
    private final int maxResultsLimit;
    private final QueryParser.Operator defaultOperator;

    public SearchService(IndexSearcher searcher, Analyzer analyzer, int maxResultsLimit,
                         QueryParser.Operator defaultOperator) {
        this.searcher = searcher;
        this.analyzer = analyzer;
        this.maxResultsLimit = maxResultsLimit;
        this.defaultOperator = defaultOperator;
    }

    public SearchResponse search(SearchRequest request) throws IOException {
        if (request.query == null || request.query.isBlank()) {
            return SearchResponse.error("Κενό ερώτημα: στείλε {\"query\": \"...\", \"maxResults\": 10}");
        }
        int maxResults = clamp(request.maxResults == null ? 10 : request.maxResults, 1, maxResultsLimit);
        int offset = Math.max(0, request.offset == null ? 0 : request.offset);

        // 1. Ερώτημα χρήστη → Lucene Query.
        //    Ο QueryParser περνά τις λέξεις από τον ΙΔΙΟ EnglishAnalyzer με το indexing
        //    ("Networks" → "network") και καταλαβαίνει AND, OR, NOT, "φράσεις", (παρενθέσεις).
        //    Δημιουργείται νέος parser ανά ερώτημα γιατί ο QueryParser δεν είναι thread-safe.
        Query query;
        try {
            QueryParser parser = new QueryParser(Fields.CONTENT, analyzer);
            parser.setDefaultOperator(defaultOperator);
            query = parser.parse(request.query);
        } catch (ParseException e) {
            return SearchResponse.error("Λάθος σύνταξη ερωτήματος: " + firstLine(e.getMessage()));
        }

        // 2. Αναζήτηση: η Lucene βαθμολογεί με BM25 και επιστρέφει τα top-k.
        //    Για pagination ζητάμε offset+maxResults και κρατάμε μόνο τη ζητούμενη «σελίδα».
        TopDocs top = searcher.search(query, offset + maxResults);
        long totalHits = searcher.count(query);   // ακριβές πλήθος όλων των εγγράφων που ταιριάζουν

        // 3. Οι όροι του ερωτήματος (μετά το stemming) για τα snippets. Οι όροι μετά από NOT
        //    δεν συλλέγονται (ο termCollector αγνοεί τα MUST_NOT clauses).
        Set<Term> terms = new HashSet<>();
        query.visit(QueryVisitor.termCollector(terms));
        Set<String> termTexts = new HashSet<>();
        for (Term t : terms) {
            termTexts.add(t.text());
        }

        // 4. Από docIDs → πραγματικά έγγραφα (μέσω των stored fields).
        SearchResponse response = SearchResponse.hits(totalHits, offset);
        StoredFields storedFields = searcher.storedFields();
        ScoreDoc[] hits = top.scoreDocs;
        for (int i = offset; i < hits.length; i++) {
            Document doc = storedFields.document(hits[i].doc);
            SearchResult r = new SearchResult();
            r.rank = i + 1;
            r.filename = doc.get(Fields.FILENAME);
            r.path = doc.get(Fields.PATH);
            r.score = Math.round(hits[i].score * 10000.0) / 10000.0;
            r.docno = doc.get(Fields.DOCNO);
            r.title = doc.get(Fields.TITLE);
            r.snippet = SnippetBuilder.build(analyzer, doc.get(Fields.CONTENT), termTexts);
            response.results.add(r);
        }
        return response;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String firstLine(String s) {
        if (s == null) {
            return "";
        }
        int nl = s.indexOf('\n');
        return (nl > 0 ? s.substring(0, nl) : s).strip();
    }
}
