package gr.ihu.ir.server;

import gr.ihu.ir.common.RepoDocument;
import gr.ihu.ir.common.SearchRequest;
import gr.ihu.ir.common.SearchResponse;
import gr.ihu.ir.common.TextAnalysis;
import gr.ihu.ir.indexer.TestIndexes;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Boolean ερωτήματα, phrase queries και χειρισμός λαθών πάνω σε ένα μικρό index στη μνήμη. */
class SearchServiceTest {

    private Directory dir;
    private DirectoryReader reader;
    private SearchService service;

    @BeforeEach
    void setUp() throws Exception {
        Analyzer analyzer = TextAnalysis.newAnalyzer();
        dir = TestIndexes.inMemory(analyzer, List.of(
                new RepoDocument("a", "a.pdf", "Deep neural networks for image recognition."),
                new RepoDocument("b", "b.pdf", "A network of neural pathways; graph theory of the brain."),
                new RepoDocument("c", "c.pdf", "Information retrieval evaluation with graphs and networking."),
                new RepoDocument("d", "d.pdf", "Retrieval models: BM25 and evaluation of search engines.")));
        reader = DirectoryReader.open(dir);
        service = new SearchService(new IndexSearcher(reader), analyzer, 100, QueryParser.Operator.OR);
    }

    @AfterEach
    void tearDown() throws Exception {
        reader.close();
        dir.close();
    }

    private SearchResponse search(String q) throws Exception {
        return service.search(SearchRequest.query(q, 10, 0));
    }

    @Test
    void phraseQueryUsesPositions() throws Exception {
        // "neural network" ως φράση: μόνο το a (στο b οι λέξεις είναι σε άλλη σειρά/θέση)
        SearchResponse r = search("\"neural network\"");
        assertEquals(1, r.totalHits);
        assertEquals("a.pdf", r.results.get(0).filename);
    }

    @Test
    void booleanAnd() throws Exception {
        SearchResponse r = search("retrieval AND evaluation");
        assertEquals(2, r.totalHits);
    }

    @Test
    void booleanOrNot() throws Exception {
        // graph OR network, αλλά ΟΧΙ neural → μόνο το c
        SearchResponse r = search("graph OR network NOT neural");
        assertEquals(1, r.totalHits);
        assertEquals("c.pdf", r.results.get(0).filename);
        assertNotNull(r.results.get(0).snippet);
    }

    @Test
    void syntaxErrorReturnsErrorNotException() throws Exception {
        SearchResponse r = search("neural AND (");
        assertNotNull(r.error);
        assertTrue(r.error.startsWith("Λάθος σύνταξη"));
    }

    @Test
    void paginationWithOffset() throws Exception {
        SearchResponse all = service.search(SearchRequest.query("network OR retrieval", 10, 0));
        SearchResponse page2 = service.search(SearchRequest.query("network OR retrieval", 2, 2));
        assertEquals(all.totalHits, page2.totalHits);
        assertEquals(all.results.get(2).filename, page2.results.get(0).filename);
        assertEquals(3, page2.results.get(0).rank);
    }
}
