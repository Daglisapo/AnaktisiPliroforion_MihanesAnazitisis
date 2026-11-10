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
     * Ένας απλός «τίτλος» για εμφάνιση: η πρώτη γραμμή (από τις 40 πρώτες) με αρκετά γράμματα.
     * (Δεν είναι πάντα ο πραγματικός τίτλος του άρθρου, αλλά συνήθως είναι κοντά.)
     *
     * Τα PDF του arXiv έχουν στο περιθώριο την κάθετη «σφραγίδα» "arXiv:0705.1161v1 [cs.IR] 8 May 2007",
     * που το PDFBox βγάζει ως πολλές πολύ μικρές γραμμές ("8", "M", "ay", "2", ...). Αυτές
     * έχουν λίγα γράμματα, οπότε απορρίπτονται από τον έλεγχο letters >= 10.
     */
    static String guessTitle(String text) {
        return text.lines()
                .limit(40)
                .map(String::strip)
                .filter(t -> t.chars().filter(Character::isLetter).count() >= 10)
                .filter(t -> !t.toLowerCase().startsWith("arxiv:"))
                .findFirst()
                .map(t -> t.length() > 200 ? t.substring(0, 200) + "…" : t)
                .orElse("");
    }
}
