package gr.ihu.ir.ui;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import gr.ihu.ir.common.AppConfig;
import gr.ihu.ir.common.SearchResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * App 5 — Search Interface.
 *
 * Μια «τυπική διεπαφή αναζήτησης» (διαφάνεια 3): πλαίσιο κειμένου + κουμπί «Αναζήτηση».
 * Υλοποιείται ως μικρός web server με το ενσωματωμένο {@code com.sun.net.httpserver.HttpServer}
 * του JDK (χωρίς frameworks). Ο χρήστης ανοίγει http://localhost:8080 στον browser.
 *
 * Ροή ενός ερωτήματος:
 *   browser ──GET /?q=...──► App 5 ──(εκτελεί)──► App 4 Client ──TCP/JSON──► App 3 Server
 *   browser ◄──HTML──────── App 5 ◄──JSON (stdout)── App 4 ◄────────────────── App 3
 *
 * Endpoints:
 *   /            σελίδα αναζήτησης (με ?q=...&page=N εμφανίζει αποτελέσματα)
 *   /pdf?file=X  ανοίγει το αρχικό PDF (ΜΟΝΟ από τον φάκελο arxiv_papers)
 *
 * Χρήση:  run-app5.bat   και μετά  http://localhost:8080
 */
public class SearchInterface {

    private final ClientRunner client;
    private final Path pdfDir;
    private final int perPage;

    SearchInterface(ClientRunner client, Path pdfDir, int perPage) {
        this.client = client;
        this.pdfDir = pdfDir.toAbsolutePath().normalize();
        this.perPage = perPage;
    }

    public static void main(String[] args) throws IOException {
        AppConfig cfg = AppConfig.load();
        int port = cfg.getInt("ui.port");
        SearchInterface ui = new SearchInterface(
                new ClientRunner(cfg.serverHost(), cfg.serverPort()),
                cfg.pdfDir(), cfg.getInt("ui.results.per.page"));

        // Ακούμε μόνο στο localhost: η διεπαφή είναι για τον τοπικό υπολογιστή.
        HttpServer http = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
        http.createContext("/", ui::handleSearch);
        http.createContext("/pdf", ui::handlePdf);
        http.setExecutor(Executors.newFixedThreadPool(4));
        http.start();

        System.out.println("=== App 5: Search Interface ===");
        System.out.println("Άνοιξε στον browser: http://localhost:" + port);
        System.out.println("(Query Server: " + cfg.serverHost() + ":" + cfg.serverPort() + ", Ctrl+C για τερματισμό)");
    }

    /** GET / και GET /?q=...&page=N */
    private void handleSearch(HttpExchange ex) throws IOException {
        try (ex) {
            if (!ex.getRequestURI().getPath().equals("/")) {
                sendText(ex, 404, "text/plain", "404 Not Found");
                return;
            }
            Map<String, String> params = queryParams(ex.getRequestURI().getRawQuery());
            String q = params.getOrDefault("q", "").strip();
            if (q.isEmpty()) {
                sendText(ex, 200, "text/html", ResultsPage.empty());
                return;
            }
            int page = Math.max(1, parseIntOr(params.get("page"), 1));

            long start = System.nanoTime();
            SearchResponse resp = client.search(q, perPage, (page - 1) * perPage);
            long ms = (System.nanoTime() - start) / 1_000_000;
            System.out.printf("[ui] '%s' σελ.%d → %s (%d ms)%n", q, page,
                    resp.error != null ? "σφάλμα" : resp.totalHits + " αποτελέσματα", ms);

            sendText(ex, 200, "text/html", ResultsPage.render(q, resp, page, perPage, ms));
        }
    }

    /**
     * GET /pdf?file=2401.12345v3.pdf — σερβίρει το αρχικό PDF.
     * Έλεγχος path traversal: δεν επιτρέπουμε π.χ. file=..\..\Windows\win.ini.
     * Δεχόμαστε μόνο απλό όνομα αρχείου .pdf που, μετά το normalize(), βρίσκεται ΜΕΣΑ στο pdfDir.
     */
    private void handlePdf(HttpExchange ex) throws IOException {
        try (ex) {
            String name = queryParams(ex.getRequestURI().getRawQuery()).getOrDefault("file", "");
            Path file = resolvePdf(name);
            if (file == null) {
                sendText(ex, 404, "text/plain", "PDF not found");
                return;
            }
            ex.getResponseHeaders().set("Content-Type", "application/pdf");
            ex.sendResponseHeaders(200, Files.size(file));
            try (OutputStream out = ex.getResponseBody()) {
                Files.copy(file, out);
            }
        }
    }

    /** Επιστρέφει το ασφαλές path του PDF ή null αν το όνομα δεν είναι αποδεκτό. */
    Path resolvePdf(String name) {
        if (!name.matches("[A-Za-z0-9._-]+\\.pdf")) {
            return null;                                   // όχι «/», «\», «:» κ.λπ.
        }
        Path file = pdfDir.resolve(name).normalize();
        if (!file.startsWith(pdfDir) || !Files.isRegularFile(file)) {
            return null;
        }
        return file;
    }

    private static void sendText(HttpExchange ex, int status, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    /** "q=neural+network&page=2" → {q: "neural network", page: "2"} */
    private static Map<String, String> queryParams(String rawQuery) {
        Map<String, String> map = new HashMap<>();
        if (rawQuery == null) {
            return map;
        }
        for (String pair : rawQuery.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                map.put(URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
                        URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
            }
        }
        return map;
    }

    private static int parseIntOr(String s, int fallback) {
        try {
            return s == null ? fallback : Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
