package gr.ihu.ir.store;

import gr.ihu.ir.common.AppConfig;
import gr.ihu.ir.common.RepoDocument;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * App 1 — Store Module.
 *
 * Ρόλος (διαφάνεια 10): να φέρει τη συλλογή σε μορφή αποδοτική για επεξεργασία.
 * «Ένα αρχείο ανά έγγραφο» (10.000 μικρά PDF) δεν είναι πρακτικό, οπότε:
 *   1. διαβάζει κάθε PDF και το μετατρέπει σε plain text (PdfTextExtractor / PDFBox),
 *   2. γράφει τα κείμενα το ένα μετά το άλλο σε μεγάλα αρχεία έως 128 MB (RepositoryWriter).
 *
 * Παραλληλία: η εξαγωγή κειμένου από PDF είναι αργή (CPU), οπότε γίνεται ταυτόχρονα σε
 * πολλά threads (thread pool). Η ΕΓΓΡΑΦΗ όμως στο repository γίνεται ΣΕΙΡΙΑΚΑ από το main
 * thread, με τη σειρά των αρχείων: έτσι δεν χρειάζεται συγχρονισμός στο αρχείο εξόδου και
 * το repository βγαίνει πάντα ίδιο (ντετερμινιστικό).
 *
 * Χρήση:  run-app1.bat [--limit N] [--threads T]
 */
public class StoreModule {

    /** Μέγιστος χρόνος για ένα PDF· αν το PDFBox «κολλήσει», το αρχείο καταγράφεται ως αποτυχία. */
    private static final long PDF_TIMEOUT_SECONDS = 120;

    // Κρατάμε αναφορές στους loggers, αλλιώς η Java μπορεί να τους «ξεχάσει» (garbage collection)
    // και να χαθεί η ρύθμιση. Το PDFBox γράφει πολλά warnings (π.χ. για fonts) που δεν μας αφορούν.
    private static final Logger PDFBOX_LOG = Logger.getLogger("org.apache.pdfbox");
    private static final Logger FONTBOX_LOG = Logger.getLogger("org.apache.fontbox");

    public static void main(String[] args) throws Exception {
        PDFBOX_LOG.setLevel(Level.SEVERE);
        FONTBOX_LOG.setLevel(Level.SEVERE);

        AppConfig cfg = AppConfig.load();
        int limit = intOption(args, "--limit", Integer.MAX_VALUE);
        int threads = intOption(args, "--threads", cfg.getInt("store.threads"));
        long maxFileBytes = cfg.getInt("repo.max.file.mb") * 1024L * 1024L;
        Path pdfDir = cfg.pdfDir();
        Path repoDir = cfg.repoDir();

        List<Path> pdfs = listPdfs(pdfDir, limit);
        System.out.println("=== App 1: Store Module ===");
        System.out.println("PDF φάκελος : " + pdfDir + "  (" + pdfs.size() + " αρχεία προς επεξεργασία)");
        System.out.println("Repository  : " + repoDir + "  (όριο " + (maxFileBytes >> 20) + " MB/αρχείο)");
        System.out.println("Threads     : " + threads);

        Files.createDirectories(repoDir);
        deleteOldRepositoryFiles(repoDir);

        long start = System.nanoTime();
        int ok = 0;
        int failed = 0;

        Path errorLogFile = repoDir.resolve("errors.log");
        ExecutorService pool = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "pdf-extractor");
            t.setDaemon(true);   // ένα «κολλημένο» PDF δεν εμποδίζει τον τερματισμό του προγράμματος
            return t;
        });

        List<Path> repoFiles;
        try (RepositoryWriter writer = new RepositoryWriter(repoDir, maxFileBytes);
             PrintWriter errorLog = new PrintWriter(Files.newBufferedWriter(errorLogFile, StandardCharsets.UTF_8))) {

            errorLog.println("# Σφάλματα Store Module — " + LocalDateTime.now());

            // Επεξεργασία σε «παρτίδες»: στέλνουμε λίγα PDF στο pool και μετά γράφουμε τα
            // αποτελέσματά τους ΜΕ ΤΗ ΣΕΙΡΑ. Έτσι στη μνήμη υπάρχουν μόνο λίγα κείμενα τη φορά.
            int batchSize = threads * 2;
            for (int from = 0; from < pdfs.size(); from += batchSize) {
                List<Path> batch = pdfs.subList(from, Math.min(from + batchSize, pdfs.size()));

                List<Future<String>> futures = new ArrayList<>();
                for (Path pdf : batch) {
                    futures.add(pool.submit(() -> PdfTextExtractor.extract(pdf)));
                }

                for (int i = 0; i < batch.size(); i++) {
                    Path pdf = batch.get(i);
                    String fileName = pdf.getFileName().toString();
                    try {
                        String text = futures.get(i).get(PDF_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                        if (text.isBlank()) {
                            // π.χ. σκαναρισμένο PDF (μόνο εικόνες) — δεν έχει κείμενο για ευρετηρίαση
                            throw new IOException("Δεν βρέθηκε κείμενο (ίσως σκαναρισμένο PDF)");
                        }
                        writer.write(new RepoDocument(stripExtension(fileName), fileName, text));
                        ok++;
                    } catch (ExecutionException e) {
                        failed++;
                        logError(errorLog, fileName, e.getCause());
                    } catch (TimeoutException e) {
                        failed++;
                        futures.get(i).cancel(true);
                        logError(errorLog, fileName, new IOException("Timeout > " + PDF_TIMEOUT_SECONDS + "s"));
                    } catch (IOException e) {
                        failed++;
                        logError(errorLog, fileName, e);
                    }

                    int processed = ok + failed;
                    if (processed % 100 == 0) {
                        System.out.printf("  ... %d / %d  (OK: %d, σφάλματα: %d, %.0f s)%n",
                                processed, pdfs.size(), ok, failed, seconds(start));
                    }
                }
            }

            repoFiles = writer.createdFiles();
        } finally {
            pool.shutdownNow();
        }
        // Η σύνοψη τυπώνεται ΜΕΤΑ το κλείσιμο του writer, ώστε τα μεγέθη αρχείων να είναι τα τελικά.
        printSummary(ok, failed, repoFiles, errorLogFile, start);
    }

    /** Όλα τα *.pdf του φακέλου, ταξινομημένα κατά όνομα, το πολύ {@code limit}. */
    private static List<Path> listPdfs(Path pdfDir, int limit) throws IOException {
        if (!Files.isDirectory(pdfDir)) {
            throw new IOException("Δεν βρέθηκε ο φάκελος με τα PDF: " + pdfDir);
        }
        try (Stream<Path> s = Files.list(pdfDir)) {
            return s.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".pdf"))
                    .sorted()
                    .limit(limit)
                    .toList();
        }
    }

    /** Σβήνει repo_*.jsonl από προηγούμενο τρέξιμο, ώστε να μη μείνουν «παλιά» αρχεία. */
    private static void deleteOldRepositoryFiles(Path repoDir) throws IOException {
        try (Stream<Path> s = Files.list(repoDir)) {
            for (Path p : s.toList()) {
                String n = p.getFileName().toString();
                if (n.startsWith("repo_") && n.endsWith(".jsonl")) {
                    Files.delete(p);
                }
            }
        }
    }

    private static void logError(PrintWriter errorLog, String fileName, Throwable error) {
        String msg = error.getClass().getSimpleName() + ": " + error.getMessage();
        errorLog.println(fileName + "\t" + msg);
        errorLog.flush();
        System.out.println("  [σφάλμα] " + fileName + " -> " + msg);
    }

    private static void printSummary(int ok, int failed, List<Path> files, Path errorLog, long start)
            throws IOException {
        long totalBytes = 0;
        System.out.println();
        System.out.println("=== Σύνοψη ===");
        for (Path f : files) {
            long size = Files.size(f);
            totalBytes += size;
            System.out.printf("  %s  %,d bytes (%.1f MB)%n", f.getFileName(), size, size / 1048576.0);
        }
        System.out.println("Επιτυχίες       : " + ok);
        System.out.println("Αποτυχίες       : " + failed + (failed > 0 ? "  (βλ. " + errorLog + ")" : ""));
        System.out.println("Αρχεία repository: " + files.size());
        System.out.printf("Μέγεθος repository: %.1f MB%n", totalBytes / 1048576.0);
        System.out.printf("Συνολικός χρόνος : %.1f s%n", seconds(start));
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private static double seconds(long startNanos) {
        return (System.nanoTime() - startNanos) / 1e9;
    }

    /** Διαβάζει μια επιλογή της μορφής «--όνομα τιμή» από τα ορίσματα. */
    private static int intOption(String[] args, String name, int defaultValue) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(name)) {
                return Integer.parseInt(args[i + 1]);
            }
        }
        return defaultValue;
    }
}
