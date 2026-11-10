package gr.ihu.ir.common;

/**
 * Τα ονόματα των πεδίων του Lucene Document.
 * Ορίζονται σε ΕΝΑ σημείο γιατί τα χρησιμοποιούν και ο Indexer (App 2) και ο Server (App 3)
 * — αν διέφεραν έστω και κατά ένα γράμμα, η αναζήτηση δεν θα έβρισκε τίποτα.
 */
public final class Fields {

    public static final String DOCNO = "docno";       // π.χ. 2401.12345v3
    public static final String FILENAME = "filename"; // π.χ. 2401.12345v3.pdf
    public static final String PATH = "path";         // πλήρης διαδρομή του PDF
    public static final String TITLE = "title";       // πρώτη γραμμή κειμένου (για εμφάνιση)
    public static final String CONTENT = "content";   // όλο το κείμενο (το πεδίο αναζήτησης)

    private Fields() {
    }
}
