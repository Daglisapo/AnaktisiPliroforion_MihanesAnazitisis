package gr.ihu.ir.indexer;

import gr.ihu.ir.common.AppConfig;
import gr.ihu.ir.common.RepositoryReader;
import gr.ihu.ir.common.TextAnalysis;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * App 2 — Text Preprocessor & Lucene Indexer.
 *
 * Ροή (διαφάνεια 12):
 *   Repository ──(ένα-ένα έγγραφο)──► Lucene Document ──► IndexWriter ──► Inverted Index
 *
 * Η προεπεξεργασία (casefolding, αφαίρεση στίξης, stopwords, stemming) γίνεται από τον
 * EnglishAnalyzer, που δίνεται στον IndexWriter: ο IndexWriter τον εφαρμόζει αυτόματα στο
 * tokenized πεδίο "content" κάθε εγγράφου (βλ. {@link TextAnalysis}).
 *
 * Χρήση:  run-app2.bat
 */
public class Indexer {

    public static void main(String[] args) throws Exception {
        AppConfig cfg = AppConfig.load();
        Path repoDir = cfg.repoDir();
        Path indexDir = cfg.indexDir();
        Path pdfDir = cfg.pdfDir();

        System.out.println("=== App 2: Text Preprocessor & Lucene Indexer ===");
        System.out.println("Repository: " + repoDir);
        System.out.println("Index     : " + indexDir);

        long start = System.nanoTime();
        Analyzer analyzer = TextAnalysis.newAnalyzer();

        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        // CREATE: φτιάχνουμε το ευρετήριο από την αρχή (σβήνει ό,τι υπήρχε) — αλλιώς ένα
        // δεύτερο τρέξιμο θα έβαζε κάθε έγγραφο ΔΥΟ φορές.
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        // Μεγαλύτερος buffer στη RAM = λιγότερα «segments» στον δίσκο = γρηγορότερο indexing.
        iwc.setRAMBufferSizeMB(256);

        long count;
        try (Directory dir = FSDirectory.open(indexDir);
             IndexWriter writer = new IndexWriter(dir, iwc)) {

            long[] counter = {0};
            count = RepositoryReader.readAll(repoDir, repoDoc -> {
                writer.addDocument(LuceneDocumentFactory.create(repoDoc, pdfDir));
                counter[0]++;
                if (counter[0] % 500 == 0) {
                    System.out.printf("  ... %d έγγραφα (%.0f s)%n", counter[0], seconds(start));
                }
            });

            System.out.println("Commit στο δίσκο...");
            writer.commit();   // από εδώ και πέρα το ευρετήριο είναι μόνιμο και ορατό στους readers
        }                      // close(): κλείνει τα αρχεία του index

        System.out.println();
        System.out.println("=== Σύνοψη ===");
        System.out.println("Έγγραφα στο index : " + count);
        System.out.printf("Μέγεθος index     : %.1f MB%n", folderSize(indexDir) / 1048576.0);
        System.out.printf("Συνολικός χρόνος  : %.1f s%n", seconds(start));
    }

    private static long folderSize(Path dir) throws IOException {
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(Files::isRegularFile).mapToLong(p -> p.toFile().length()).sum();
        }
    }

    private static double seconds(long startNanos) {
        return (System.nanoTime() - startNanos) / 1e9;
    }
}
