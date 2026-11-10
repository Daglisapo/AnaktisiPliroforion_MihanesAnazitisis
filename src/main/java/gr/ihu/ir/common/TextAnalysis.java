package gr.ihu.ir.common;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Η προεπεξεργασία κειμένου του συστήματος, σε ΕΝΑ σημείο.
 *
 * Χρησιμοποιούμε τον {@link EnglishAnalyzer} της Lucene (τα άρθρα του arXiv είναι αγγλικά).
 * Η «αλυσίδα» του, με τη σειρά που εφαρμόζεται:
 *   1. StandardTokenizer       → σπάει το κείμενο σε λέξεις (tokens) με βάση τους κανόνες
 *                                Unicode· τα σημεία στίξης ΑΦΑΙΡΟΥΝΤΑΙ εδώ (punctuation removal).
 *   2. EnglishPossessiveFilter → αφαιρεί το κτητικό 's  (π.χ. "Google's" → "Google").
 *   3. LowerCaseFilter         → casefolding: "Network" → "network".
 *   4. StopFilter              → αφαιρεί stopwords ("the", "a", "of", "and", ...).
 *   5. PorterStemFilter        → stemming με τον αλγόριθμο Porter:
 *                                "networks", "networking", "network" → "network".
 *
 * ΓΙΑΤΙ μία κοινή μέθοδος: ο Indexer (App 2) και ο Query Server (App 3) ΠΡΕΠΕΙ να
 * χρησιμοποιούν ΑΚΡΙΒΩΣ τον ίδιο analyzer. Αν στο ευρετήριο είναι αποθηκευμένο το
 * "network" (stem) και το ερώτημα έμενε "Networks", δεν θα υπήρχε ταίριασμα.
 */
public final class TextAnalysis {

    private TextAnalysis() {
    }

    /** Ο analyzer για indexing ΚΑΙ για searching. */
    public static Analyzer newAnalyzer() {
        return new EnglishAnalyzer();
    }

    /** Βοηθητικό: επιστρέφει τα tokens που βγάζει ο analyzer για ένα κείμενο (για επίδειξη/έλεγχο). */
    public static List<String> tokens(Analyzer analyzer, String text) throws IOException {
        List<String> result = new ArrayList<>();
        try (TokenStream ts = analyzer.tokenStream(Fields.CONTENT, text)) {
            CharTermAttribute term = ts.addAttribute(CharTermAttribute.class);
            ts.reset();                       // υποχρεωτικό πριν το πρώτο incrementToken()
            while (ts.incrementToken()) {
                result.add(term.toString());
            }
            ts.end();
        }
        return result;
    }
}
