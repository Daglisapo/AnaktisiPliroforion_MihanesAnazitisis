package gr.ihu.ir.store;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Μετατροπή ενός PDF σε plain text με το Apache PDFBox 3.x (βήμα 1 του Store Module).
 */
public final class PdfTextExtractor {

    private PdfTextExtractor() {
    }

    /**
     * Ανοίγει το PDF, εξάγει όλο το κείμενο και το «καθαρίζει».
     * Αν το PDF είναι χαλασμένο ή κρυπτογραφημένο, το PDFBox πετάει IOException
     * — τη χειρίζεται ο καλών (StoreModule), που το γράφει στο errors.log και συνεχίζει.
     */
    public static String extract(Path pdf) throws IOException {
        // try-with-resources: το PDDocument κλείνει πάντα (αλλιώς μένουν ανοιχτά αρχεία/μνήμη).
        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String raw = stripper.getText(document);
            return clean(raw);
        }
    }

    /**
     * Μικρός καθαρισμός του κειμένου ΠΡΙΝ μπει στο repository:
     *  - Windows αλλαγές γραμμής (\r\n) γίνονται \n,
     *  - αφαιρούνται «αόρατοι» χαρακτήρες ελέγχου (π.χ. \u0000) που βγάζουν μερικά PDF,
     *  - αφαιρούνται «μισά» surrogate pairs (χαλασμένοι Unicode χαρακτήρες) που αλλιώς
     *    θα έκαναν το UTF-8 αρχείο άκυρο,
     *  - πολλές κενές γραμμές στη σειρά γίνονται μία.
     * ΔΕΝ κάνουμε εδώ lowercase/stemming: το repository κρατά το ΑΥΘΕΝΤΙΚΟ κείμενο.
     * Η γλωσσική προεπεξεργασία γίνεται στο App 2 (Indexer) από τον EnglishAnalyzer.
     */
    static String clean(String raw) {
        String text = raw.replace("\r\n", "\n").replace('\r', '\n');
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isHighSurrogate(c)) {
                // Κρατάμε ζεύγος surrogate μόνο αν είναι πλήρες (high + low).
                if (i + 1 < text.length() && Character.isLowSurrogate(text.charAt(i + 1))) {
                    sb.append(c).append(text.charAt(i + 1));
                    i++;
                } else {
                    sb.append(' ');
                }
            } else if (Character.isLowSurrogate(c)) {
                sb.append(' ');                       // «ορφανό» low surrogate
            } else if (c == '\n' || c == '\t' || !Character.isISOControl(c)) {
                sb.append(c);
            } else {
                sb.append(' ');                       // άλλοι χαρακτήρες ελέγχου
            }
        }
        return sb.toString().replaceAll("\n{3,}", "\n\n").strip();
    }
}
