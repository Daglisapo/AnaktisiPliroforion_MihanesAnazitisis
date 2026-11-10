package gr.ihu.ir.indexer;

import gr.ihu.ir.common.Fields;
import gr.ihu.ir.common.RepoDocument;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexOptions;

import java.nio.file.Path;

/**
 * Μετατρέπει ένα έγγραφο του Repository σε Lucene {@link Document}.
 *
 * Πεδία:
 *   docno    – StringField (ΔΕΝ γίνεται tokenize, ψάχνεται μόνο ακριβώς), stored
 *   filename – StringField, stored
 *   path     – StoredField (μόνο αποθήκευση, δεν χρειάζεται αναζήτηση)
 *   title    – StoredField (πρώτη γραμμή κειμένου, για να δείχνουμε κάτι ωραίο στα αποτελέσματα)
 *   content  – custom FieldType (βλ. CONTENT_TYPE), εδώ γίνεται η πραγματική αναζήτηση
 */
public final class LuceneDocumentFactory {

    /**
     * Ο τύπος του πεδίου content — οι δύο ρητές απαιτήσεις της εκφώνησης (διαφάνεια 12):
     *
     * 1. IndexOptions.DOCS_AND_FREQS_AND_POSITIONS:
     *    κάθε posting του αντεστραμμένου ευρετηρίου κρατά
     *      - το docID           (σε ποια έγγραφα εμφανίζεται ο όρος),
     *      - το term frequency  (πόσες φορές εμφανίζεται σε κάθε έγγραφο → χρειάζεται για BM25),
     *      - τις positions      (σε ποιες θέσεις → χρειάζονται για phrase queries "neural network").
     *
     * 2. stored = true:
     *    αποθηκεύεται και το αυθεντικό κείμενο. Είναι «σπατάλη» χώρου, αλλά επιτρέπει στη
     *    Lucene να επιστρέφει πραγματικά έγγραφα (κείμενο, snippet) και όχι σκέτα docIDs.
     */
    static final FieldType CONTENT_TYPE = new FieldType();

    static {
        CONTENT_TYPE.setTokenized(true);   // περνάει από τον analyzer (EnglishAnalyzer)
        CONTENT_TYPE.setStored(true);
        CONTENT_TYPE.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
        CONTENT_TYPE.freeze();             // από εδώ και πέρα δεν αλλάζει κατά λάθος
    }

    private LuceneDocumentFactory() {
    }

    public static Document create(RepoDocument repoDoc, Path pdfDir) {
        Document doc = new Document();
        doc.add(new StringField(Fields.DOCNO, repoDoc.id, Field.Store.YES));
        doc.add(new StringField(Fields.FILENAME, repoDoc.source, Field.Store.YES));
        doc.add(new StoredField(Fields.PATH, pdfDir.resolve(repoDoc.source).toString()));
        doc.add(new StoredField(Fields.TITLE, guessTitle(repoDoc.text)));
        doc.add(new Field(Fields.CONTENT, repoDoc.text, CONTENT_TYPE));
        return doc;
    }

    /**
     * Ένας απλός «τίτλος» για εμφάνιση: η πρώτη γραμμή του κειμένου με αρκετά γράμματα.
     * (Δεν είναι πάντα ο πραγματικός τίτλος του άρθρου, αλλά συνήθως είναι κοντά.)
     */
    static String guessTitle(String text) {
        for (String line : text.split("\n", 15)) {
            String t = line.strip();
            long letters = t.chars().filter(Character::isLetter).count();
            if (letters >= 10 && !t.toLowerCase().startsWith("arxiv:")) {
                return t.length() > 200 ? t.substring(0, 200) + "…" : t;
            }
        }
        return "";
    }
}
