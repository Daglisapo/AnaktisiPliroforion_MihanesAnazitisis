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
    void contentFieldStoresFreqsPositionsAndText() {
        assertEquals(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS, LuceneDocumentFactory.CONTENT_TYPE.indexOptions());
        assertTrue(LuceneDocumentFactory.CONTENT_TYPE.stored());
        assertTrue(LuceneDocumentFactory.CONTENT_TYPE.tokenized());
    }
}
