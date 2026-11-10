package gr.ihu.ir.server;

import gr.ihu.ir.common.Fields;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

import java.io.IOException;
import java.util.Set;

/**
 * Φτιάχνει ένα μικρό απόσπασμα (snippet, ~200 χαρακτήρες) του κειμένου γύρω από την
 * πρώτη εμφάνιση ενός όρου του ερωτήματος — όπως τα snippets των μηχανών αναζήτησης.
 *
 * Πώς βρίσκουμε τη θέση: περνάμε το κείμενο από τον ΙΔΙΟ analyzer. Κάθε token έχει
 * το stem του (π.χ. "network") ΚΑΙ τα offsets του στο αυθεντικό κείμενο (π.χ. 120–128 για
 * τη λέξη "Networks"). Μόλις βρούμε token που ανήκει στους όρους του ερωτήματος, κόβουμε
 * το κείμενο γύρω από αυτό το offset.
 *
 * (Αυτό είναι δυνατό μόνο επειδή αποθηκεύσαμε το κείμενο στο index — stored = true.)
 */
final class SnippetBuilder {

    private static final int BEFORE = 70;    // χαρακτήρες πριν από τον όρο
    private static final int LENGTH = 200;   // συνολικό μήκος

    private SnippetBuilder() {
    }

    static String build(Analyzer analyzer, String text, Set<String> queryTerms) throws IOException {
        int hit = findFirstMatch(analyzer, text, queryTerms);
        int start = Math.max(0, hit - BEFORE);
        int end = Math.min(text.length(), start + LENGTH);

        // Μη κόβουμε λέξεις στη μέση: μετακινούμε τα άκρα σε κενό.
        if (start > 0) {
            int space = text.indexOf(' ', start);
            if (space > 0 && space < hit) {
                start = space + 1;
            }
        }
        if (end < text.length()) {
            int space = text.lastIndexOf(' ', end);
            if (space > start) {
                end = space;
            }
        }

        String snippet = text.substring(start, end).replaceAll("\\s+", " ").strip();
        return (start > 0 ? "… " : "") + snippet + (end < text.length() ? " …" : "");
    }

    /** Το offset (θέση χαρακτήρα) του πρώτου token που είναι όρος του ερωτήματος, ή 0. */
    private static int findFirstMatch(Analyzer analyzer, String text, Set<String> queryTerms)
            throws IOException {
        if (queryTerms.isEmpty()) {
            return 0;
        }
        try (TokenStream ts = analyzer.tokenStream(Fields.CONTENT, text)) {
            CharTermAttribute term = ts.addAttribute(CharTermAttribute.class);
            OffsetAttribute offset = ts.addAttribute(OffsetAttribute.class);
            ts.reset();
            int found = 0;
            while (ts.incrementToken()) {
                if (queryTerms.contains(term.toString())) {
                    found = offset.startOffset();
                    break;
                }
            }
            // Ο κανόνας της Lucene: end() και close() πρέπει να κληθούν ακόμη κι αν σταματήσουμε νωρίς.
            ts.end();
            return found;
        }
    }
}
