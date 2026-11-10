package gr.ihu.ir.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Διαβάζει το Repository (αρχεία repo_00000.jsonl, repo_00001.jsonl, ...) ΕΝΑ-ΕΝΑ έγγραφο.
 *
 * ΓΙΑΤΙ γραμμή-γραμμή: ένα αρχείο του repository είναι ~128 MB. Αν το φορτώναμε ολόκληρο
 * στη μνήμη θα χρειαζόμασταν πολλή RAM χωρίς λόγο. Με BufferedReader.readLine() κρατάμε
 * στη μνήμη μόνο ΕΝΑ άρθρο τη φορά (μία γραμμή = ένα άρθρο).
 *
 * Το χρησιμοποιούν ο Indexer (App 2) και το εργαλείο ελέγχου RepositoryInspector.
 */
public final class RepositoryReader {

    /** Τι κάνουμε με κάθε έγγραφο που διαβάζεται (π.χ. το δίνουμε στον IndexWriter). */
    @FunctionalInterface
    public interface DocumentHandler {
        void handle(RepoDocument doc) throws IOException;
    }

    private RepositoryReader() {
    }

    /** Τα αρχεία του repository, ταξινομημένα κατά όνομα (= κατά σειρά δημιουργίας). */
    public static List<Path> listFiles(Path repoDir) throws IOException {
        if (!Files.isDirectory(repoDir)) {
            throw new IOException("Δεν βρέθηκε ο φάκελος του repository: " + repoDir);
        }
        try (Stream<Path> s = Files.list(repoDir)) {
            return s.filter(p -> {
                        String n = p.getFileName().toString();
                        return n.startsWith("repo_") && n.endsWith(".jsonl");
                    })
                    .sorted()
                    .toList();
        }
    }

    /**
     * Διαβάζει διαδοχικά όλα τα έγγραφα όλων των αρχείων και καλεί τον handler για καθένα.
     * @return πόσα έγγραφα διαβάστηκαν
     */
    public static long readAll(Path repoDir, DocumentHandler handler) throws IOException {
        long count = 0;
        for (Path file : listFiles(repoDir)) {
            try (BufferedReader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }
                    RepoDocument doc = Json.MAPPER.readValue(line, RepoDocument.class);
                    handler.handle(doc);
                    count++;
                }
            }
        }
        return count;
    }
}
