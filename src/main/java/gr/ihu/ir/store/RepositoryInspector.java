package gr.ihu.ir.store;

import gr.ihu.ir.common.AppConfig;
import gr.ihu.ir.common.RepoDocument;
import gr.ihu.ir.common.RepositoryReader;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Μικρό εργαλείο ελέγχου (ΔΕΝ είναι ένα από τα 5 apps): διαβάζει ΠΙΣΩ το Repository και
 * δείχνει ότι τα δεδομένα αποθηκεύτηκαν σωστά — αρχεία, μεγέθη, πλήθος εγγράφων και
 * ένα δείγμα κειμένου.
 *
 * Χρήση:  inspect-repository.bat [id-εγγράφου]
 */
public class RepositoryInspector {

    public static void main(String[] args) throws Exception {
        AppConfig cfg = AppConfig.load();
        Path repoDir = cfg.repoDir();
        String wantedId = args.length > 0 ? args[0] : null;

        System.out.println("Repository: " + repoDir);
        for (Path f : RepositoryReader.listFiles(repoDir)) {
            System.out.printf("  %s  %.2f MB%n", f.getFileName(), Files.size(f) / 1048576.0);
        }

        RepoDocument[] sample = new RepoDocument[1];
        long[] chars = new long[1];
        long count = RepositoryReader.readAll(repoDir, doc -> {
            chars[0] += doc.text.length();
            boolean match = wantedId == null ? sample[0] == null : doc.id.equals(wantedId);
            if (match) {
                sample[0] = doc;
            }
        });

        System.out.println("Έγγραφα στο repository: " + count);
        if (count > 0) {
            System.out.println("Μέσο μήκος κειμένου  : " + (chars[0] / count) + " χαρακτήρες");
        }
        if (sample[0] != null) {
            String text = sample[0].text;
            System.out.println();
            System.out.println("--- Δείγμα: id=" + sample[0].id + ", source=" + sample[0].source
                    + ", " + text.length() + " χαρακτήρες ---");
            System.out.println(text.substring(0, Math.min(600, text.length())));
            System.out.println("---");
        }
    }
}
