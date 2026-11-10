package gr.ihu.ir.ui;

import gr.ihu.ir.client.QueryClient;
import gr.ihu.ir.common.Json;
import gr.ihu.ir.common.SearchResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Εκτελεί το App 4 (Query Client) ως ΞΕΧΩΡΙΣΤΗ διεργασία και διαβάζει το stdout του.
 *
 * ΓΙΑΤΙ περνάμε από τον client και δεν μιλάμε απευθείας στον server:
 * το σχήμα της εκφώνησης (διαφάνεια 3) είναι Search Interface → Query Client → Query Server.
 * Ο client είναι το «συμβόλαιο» με τον server: η διεπαφή δεν χρειάζεται να ξέρει τίποτα για
 * sockets ή για το πρωτόκολλο — απλώς τρέχει ένα πρόγραμμα και διαβάζει JSON από το stdout,
 * ακριβώς όπως θα έκανε ένα PHP ή .NET script (διαφάνεια 14).
 *
 * Το ερώτημα στέλνεται στο stdin του client (λειτουργία --stdin) και όχι ως όρισμα, γιατί στα
 * Windows τα εισαγωγικά μέσα σε ορίσματα (π.χ. "neural network") χαλάνε εύκολα.
 */
final class ClientRunner {

    private static final long TIMEOUT_SECONDS = 30;

    private final String host;
    private final int port;

    ClientRunner(String host, int port) {
        this.host = host;
        this.port = port;
    }

    SearchResponse search(String query, int maxResults, int offset) {
        try {
            // Ίδια Java και ίδιο classpath με την τρέχουσα διεργασία → βρίσκει την κλάση QueryClient.
            String java = ProcessHandle.current().info().command().orElse("java");
            List<String> command = List.of(
                    java, "-cp", System.getProperty("java.class.path"),
                    QueryClient.class.getName(),
                    "--stdin", "--host", host, "--port", String.valueOf(port),
                    "--max", String.valueOf(maxResults), "--offset", String.valueOf(offset));

            Process process = new ProcessBuilder(command)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();

            // Γράφουμε το ερώτημα (μία γραμμή) και κλείνουμε το stdin → ο client τερματίζει.
            try (OutputStream stdin = process.getOutputStream()) {
                stdin.write((query.replace('\n', ' ') + "\n").getBytes(StandardCharsets.UTF_8));
            }
            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();

            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return SearchResponse.error("Ο Query Client δεν απάντησε εγκαίρως.");
            }
            if (stdout.isEmpty()) {
                return SearchResponse.error("Ο Query Client δεν επέστρεψε απάντηση (exit code "
                        + process.exitValue() + ").");
            }
            return Json.MAPPER.readValue(stdout, SearchResponse.class);
        } catch (IOException e) {
            return SearchResponse.error("Αποτυχία εκτέλεσης του Query Client: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return SearchResponse.error("Η αναζήτηση διακόπηκε.");
        }
    }
}
