package gr.ihu.ir.common;

/**
 * Ένα έγγραφο (άρθρο) όπως αποθηκεύεται στο Repository — μία γραμμή JSON:
 * <pre>
 * {"id":"2401.12345v3","source":"2401.12345v3.pdf","text":"Introduction\nThis paper..."}
 * </pre>
 * (ακριβώς η μορφή JSON της διαφάνειας 11 της εκφώνησης).
 *
 * Public πεδία + κενός constructor = ό,τι χρειάζεται η Jackson για να το γράψει/διαβάσει.
 */
public class RepoDocument {

    /** Αναγνωριστικό = όνομα αρχείου χωρίς ".pdf" (π.χ. 2401.12345v3). */
    public String id;

    /** Όνομα του αρχικού PDF (το χρειάζεται ο server για τα filename/path των αποτελεσμάτων). */
    public String source;

    /** Το plain text που εξήγαγε το PDFBox. */
    public String text;

    public RepoDocument() {
    }

    public RepoDocument(String id, String source, String text) {
        this.id = id;
        this.source = source;
        this.text = text;
    }
}
