package gr.ihu.ir.store;

import gr.ihu.ir.common.RepoDocument;
import gr.ihu.ir.common.RepositoryReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ελέγχει τον κανόνα «όριο μεγέθους, αλλά ένα άρθρο δεν σπάει ποτέ σε δύο αρχεία». */
class RepositoryWriterTest {

    @TempDir
    Path dir;

    @Test
    void rotatesFilesWithoutSplittingDocuments() throws Exception {
        long limit = 1000;                       // μικρό όριο αντί για 128 MB, για το test
        String text = "x".repeat(300) + "\nline two \"quoted\"";
        try (RepositoryWriter w = new RepositoryWriter(dir, limit)) {
            for (int i = 0; i < 10; i++) {
                w.write(new RepoDocument("doc" + i, "doc" + i + ".pdf", text));
            }
        }

        List<Path> files = RepositoryReader.listFiles(dir);
        assertTrue(files.size() > 1, "έπρεπε να δημιουργηθούν πολλά αρχεία");
        for (Path f : files) {
            List<String> lines = Files.readAllLines(f);
            // κάθε αρχείο: το πολύ «όριο + ένα άρθρο»
            assertTrue(Files.size(f) < limit + lines.get(0).length() + 2);
        }

        // Όλα τα έγγραφα διαβάζονται πίσω ακέραια και με τη σωστή σειρά.
        List<RepoDocument> back = new ArrayList<>();
        long n = RepositoryReader.readAll(dir, back::add);
        assertEquals(10, n);
        assertEquals("doc0", back.get(0).id);
        assertEquals("doc9", back.get(9).id);
        assertEquals(text, back.get(5).text);
    }

    @Test
    void cleanRemovesControlCharacters() {
        String cleaned = PdfTextExtractor.clean("Hello\u0000World\r\n\n\n\n\nEnd\uD800");
        assertEquals("Hello World\n\nEnd", cleaned);
    }
}
