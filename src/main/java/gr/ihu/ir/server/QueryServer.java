package gr.ihu.ir.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import gr.ihu.ir.common.AppConfig;
import gr.ihu.ir.common.Json;
import gr.ihu.ir.common.SearchRequest;
import gr.ihu.ir.common.SearchResponse;
import gr.ihu.ir.common.TextAnalysis;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * App 3 — Query Server.
 *
 * 1. Ανοίγει το αντεστραμμένο ευρετήριο (DirectoryReader + IndexSearcher).
 * 2. Ακούει σε ένα TCP port (default 5555).
 * 3. SERVER LOOP (διαφάνεια 13): για κάθε σύνδεση
 *      διαβάζει ΕΝΑ request (μία γραμμή JSON) → το εκτελεί στο index μέσω IndexSearcher
 *      → γράφει την απάντηση (μία γραμμή JSON) στο ΙΔΙΟ socket → κλείνει τη σύνδεση.
 *    Το loop δεν τερματίζει ποτέ από μόνο του· βγαίνουμε με break ΜΟΝΟ όταν έρθει το ειδικό
 *    request {"command":"shutdown"}.
 *
 * Οι συνδέσεις εξυπηρετούνται μία-μία (σειριακά). Για ένα σύστημα εργασίας είναι αρκετό,
 * γιατί κάθε ερώτημα απαντιέται σε λίγα ms.
 *
 * Χρήση:  run-app3.bat
 */
public class QueryServer {

    /** Πόσο περιμένουμε έναν client να στείλει το request του πριν τον «κόψουμε». */
    private static final int CLIENT_READ_TIMEOUT_MS = 10_000;

    public static void main(String[] args) throws Exception {
        AppConfig cfg = AppConfig.load();
        int port = cfg.serverPort();
        InetAddress bindAddress = InetAddress.getByName(cfg.serverHost());
        QueryParser.Operator operator = "AND".equalsIgnoreCase(cfg.get("server.default.operator"))
                ? QueryParser.Operator.AND : QueryParser.Operator.OR;

        // ΙΔΙΟΣ analyzer με το indexing (βλ. TextAnalysis): οι λέξεις του ερωτήματος πρέπει να
        // γίνουν ακριβώς οι ίδιοι όροι (stems) που υπάρχουν στο λεξικό του index.
        Analyzer analyzer = TextAnalysis.newAnalyzer();

        try (Directory dir = FSDirectory.open(cfg.indexDir());
             DirectoryReader reader = DirectoryReader.open(dir);
             ServerSocket serverSocket = new ServerSocket(port, 50, bindAddress)) {

            IndexSearcher searcher = new IndexSearcher(reader);   // default βαθμολόγηση: BM25
            SearchService service = new SearchService(searcher, analyzer,
                    cfg.getInt("server.max.results"), operator);

            System.out.println("=== App 3: Query Server ===");
            System.out.println("Index   : " + cfg.indexDir() + " (" + reader.numDocs() + " έγγραφα)");
            System.out.println("Ακούω σε: " + bindAddress.getHostAddress() + ":" + port
                    + "   (default operator: " + operator + ")");
            System.out.println("Τερματισμός με: run-app4.bat --shutdown");

            // ===================== SERVER LOOP =====================
            while (true) {
                try (Socket client = serverSocket.accept()) {          // περιμένει σύνδεση
                    client.setSoTimeout(CLIENT_READ_TIMEOUT_MS);
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                    BufferedWriter out = new BufferedWriter(
                            new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8));

                    String line = in.readLine();                       // ΕΝΑ request = μία γραμμή
                    if (line == null) {
                        continue;                                      // ο client έκλεισε χωρίς να στείλει τίποτα
                    }

                    SearchRequest request = parseRequest(line);
                    if (request != null && request.isShutdown()) {
                        send(out, SearchResponse.status("shutdown"));
                        System.out.println("[server] Ελήφθη shutdown — τερματισμός.");
                        break;                                         // η ΜΟΝΗ έξοδος από το loop
                    }

                    send(out, handle(service, request, line));
                } catch (IOException e) {
                    // Πρόβλημα με ΕΝΑΝ client (π.χ. έκλεισε απότομα): το καταγράφουμε, ο server συνεχίζει.
                    System.out.println("[server] Σφάλμα σύνδεσης: " + e.getMessage());
                }
            }
            // =======================================================
        }
        System.out.println("[server] Ο server σταμάτησε.");
    }

    /** Εκτελεί ένα request και γράφει log: ερώτημα, πλήθος αποτελεσμάτων, χρόνος. */
    private static SearchResponse handle(SearchService service, SearchRequest request, String rawLine) {
        if (request == null) {
            System.out.println("[server] Άκυρο JSON: " + rawLine);
            return SearchResponse.error("Άκυρο JSON request. Παράδειγμα: {\"query\": \"machine learning\", \"maxResults\": 10}");
        }
        long start = System.nanoTime();
        SearchResponse response;
        try {
            response = service.search(request);
        } catch (Exception e) {
            // Οτιδήποτε απρόβλεπτο (π.χ. υπερβολικά πολλοί όροι σε wildcard) → error, όχι crash.
            response = SearchResponse.error("Σφάλμα κατά την αναζήτηση: " + e.getMessage());
        }
        long ms = (System.nanoTime() - start) / 1_000_000;

        if (response.error != null) {
            System.out.printf("[ερώτημα] '%s' → ΣΦΑΛΜΑ: %s (%d ms)%n", request.query, response.error, ms);
        } else {
            response.tookMs = ms;
            System.out.printf("[ερώτημα] '%s' → %d αποτελέσματα (%d ms)%n", request.query, response.totalHits, ms);
        }
        return response;
    }

    /** JSON → SearchRequest, ή null αν το JSON είναι άκυρο. */
    private static SearchRequest parseRequest(String line) {
        try {
            return Json.MAPPER.readValue(line, SearchRequest.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static void send(BufferedWriter out, SearchResponse response) throws IOException {
        out.write(Json.MAPPER.writeValueAsString(response));
        out.write('\n');
        out.flush();
    }
}
