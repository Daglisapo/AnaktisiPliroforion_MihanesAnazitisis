package gr.ihu.ir.indexer;

import gr.ihu.ir.common.TextAnalysis;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexOptions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Η προεπεξεργασία: casefolding, στίξη, stopwords, stemming — και ο τύπος του πεδίου content. */
class AnalyzerTest {

    @Test
    void stemsVariantsToSameTerm() throws Exception {
        Analyzer a = TextAnalysis.newAnalyzer();
        assertEquals(List.of("network", "network", "network"),
                TextAnalysis.tokens(a, "Networks, networking; NETWORK!"));
    }

    @Test
    void removesStopwordsAndPunctuation() throws Exception {
        Analyzer a = TextAnalysis.newAnalyzer();
        assertEquals(List.of("evalu", "retriev", "system"),
                TextAnalysis.tokens(a, "The evaluation of the retrieval systems."));
    }

    @Test
    void titleSkipsArxivSideStamp() {
        // Η κάθετη σφραγίδα του arXiv βγαίνει ως πολλές μικρές γραμμές πριν από τον τίτλο.
        String text = "8\n M\nay\n 2\n00\n7\n] \n R\nI\n.s\nc\n[\n 1\nv\n1\n6\n1\n1\n.\n5\n0\n7\n0\n:\nv\ni\nX\nr\na\n"
                + "IDF Revisited: A Simple New Derivation\nAuthor Name\nAbstract ...";
        assertEquals("IDF Revisited: A Simple New Derivation", LuceneDocumentFactory.guessTitle(text));
    }

    @Test
    void contentFieldStoresFreqsPositionsAndText() {
        assertEquals(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS, LuceneDocumentFactory.CONTENT_TYPE.indexOptions());
        assertTrue(LuceneDocumentFactory.CONTENT_TYPE.stored());
        assertTrue(LuceneDocumentFactory.CONTENT_TYPE.tokenized());
    }
}
