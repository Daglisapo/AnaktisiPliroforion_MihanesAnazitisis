package gr.ihu.ir.store;

import gr.ihu.ir.common.Json;
import gr.ihu.ir.common.RepoDocument;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Γράφει τα έγγραφα ΔΙΑΔΟΧΙΚΑ σε μεγάλα αρχεία JSON Lines (βήμα 2 του Store Module):
 * repo_00000.jsonl, repo_00001.jsonl, ...  Το σύνολο αυτών των αρχείων = το Repository.
 *
 * Μορφή JSON Lines: ένα JSON object ανά γραμμή. Η Jackson κάνει escape τις αλλαγές
 * γραμμής του κειμένου (\n), άρα ένα έγγραφο = ακριβώς μία γραμμή.
 *
 * ΚΑΝΟΝΑΣ ΜΕΓΕΘΟΥΣ (όριο 128 MB ανά αρχείο):
 *   Ένα άρθρο ΔΕΝ σπάει ΠΟΤΕ σε δύο αρχεία. Γράφουμε ΟΛΟΚΛΗΡΟ το άρθρο στο τρέχον αρχείο·
 *   αν ΜΕΤΑ από αυτό το αρχείο έφτασε ή ξεπέρασε τα 128 MB, το κλείνουμε και τα επόμενα
 *   άρθρα πάνε σε νέο αρχείο. Γι' αυτό ένα αρχείο μπορεί να βγει λίγα KB πάνω από τα
 *   128 MB (όσο το τελευταίο άρθρο) — αυτό είναι αποδεκτό.
 *
 * Η κλάση ΔΕΝ είναι thread-safe: καλείται μόνο από ένα thread (σειριακή εγγραφή).
 */
public class RepositoryWriter implements Closeable {

    private final Path repoDir;
    private final long maxFileBytes;

    private OutputStream out;          // το τρέχον ανοιχτό αρχείο (null = κανένα)
    private long currentFileBytes;     // πόσα bytes έχουμε γράψει στο τρέχον αρχείο
    private int nextFileNumber = 0;    // ο αριθμός του επόμενου αρχείου που θα ανοίξει
    private final List<Path> createdFiles = new ArrayList<>();

    public RepositoryWriter(Path repoDir, long maxFileBytes) throws IOException {
        this.repoDir = repoDir;
        this.maxFileBytes = maxFileBytes;
        Files.createDirectories(repoDir);
    }

    /** Γράφει ένα ολόκληρο έγγραφο ως μία γραμμή JSON. */
    public void write(RepoDocument doc) throws IOException {
        if (out == null) {
            openNextFile();
        }
        byte[] line = (Json.MAPPER.writeValueAsString(doc) + "\n").getBytes(StandardCharsets.UTF_8);
        out.write(line);
        currentFileBytes += line.length;

        // Ο έλεγχος γίνεται ΜΕΤΑ την εγγραφή → το άρθρο δεν κόβεται ποτέ στη μέση.
        if (currentFileBytes >= maxFileBytes) {
            closeCurrentFile();
        }
    }

    private void openNextFile() throws IOException {
        Path file = repoDir.resolve(String.format("repo_%05d.jsonl", nextFileNumber++));
        out = new BufferedOutputStream(Files.newOutputStream(file), 1 << 20); // buffer 1 MB
        currentFileBytes = 0;
        createdFiles.add(file);
    }

    private void closeCurrentFile() throws IOException {
        if (out != null) {
            out.close();
            out = null;
        }
    }

    public List<Path> createdFiles() {
        return createdFiles;
    }

    @Override
    public void close() throws IOException {
        closeCurrentFile();
    }
}
