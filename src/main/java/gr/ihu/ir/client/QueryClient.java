package gr.ihu.ir.client;

import gr.ihu.ir.common.AppConfig;
import gr.ihu.ir.common.Json;
import gr.ihu.ir.common.SearchRequest;
import gr.ihu.ir.common.SearchResponse;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * App 4 — Query Client.
 *
 * Απλή Java εφαρμογή με TCP socket (διαφάνεια 14):
 *   1. στέλνει το ερώτημα (JSON) στον Query Server,
 *   2. λαμβάνει τα αποτελέσματα (JSON) και τα τυπώνει στο stdout.
 * Επειδή τυπώνει «καθαρό» JSON στο stdout, μπορεί να τον καλέσει ΟΠΟΙΑΔΗΠΟΤΕ άλλη
 * εφαρμογή (PHP, .NET, Python ή το δικό μας App 5) και να διαβάσει την απάντηση.
 *
 * Τρόποι εκτέλεσης — ΙΔΙΑ λειτουργικότητα, αλλάζει μόνο από πού έρχεται το ερώτημα:
 *   (α) με ορίσματα:      run-app4.bat "machine learning" 10       → τυπώνει JSON, τερματίζει
 *   (β) διαδραστικά:      run-app4.bat                             → prompt «Ερώτημα>»
 *   (γ) από άλλη εφαρμογή: run-app4.bat --stdin                    → ένα ερώτημα ανά γραμμή
 *                         από το stdin, μία γραμμή JSON ανά απάντηση στο stdout
 *   Τερματισμός server:   run-app4.bat --shutdown
 *
 * Επιλογές: --max N, --offset K, --host H, --port P, --pretty
 */
public class QueryClient {

    /** stdout σε UTF-8, ώστε όποια εφαρμογή μάς διαβάζει να παίρνει πάντα σωστούς χαρακτήρες. */
    private static final PrintStream OUT =
            new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);

    private final String host;
    private final int port;
    private final int timeoutMs;

    public QueryClient(String host, int port, int timeoutMs) {
        this.host = host;
        this.port = port;
        this.timeoutMs = timeoutMs;
    }

    public static void main(String[] args) throws Exception {
        AppConfig cfg = AppConfig.load();
        Options o = Options.parse(args, cfg);
        QueryClient client = new QueryClient(o.host, o.port, cfg.getInt("client.timeout.ms"));

        if (o.shutdown) {
            OUT.println(client.sendRaw(Json.MAPPER.writeValueAsString(SearchRequest.shutdown())));
        } else if (o.query != null) {
            // (α) ερώτημα από τα ορίσματα
            String response = client.search(o.query, o.max, o.offset);
            OUT.println(o.pretty ? pretty(response) : response);
            System.exit(isError(response) ? 2 : 0);
        } else if (o.stdin) {
            // (γ) κάθε γραμμή του stdin = ένα ερώτημα, κάθε γραμμή του stdout = μία απάντηση
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            String line;
            while ((line = in.readLine()) != null) {
                if (!line.isBlank()) {
                    OUT.println(client.search(line.strip(), o.max, o.offset));
                }
            }
        } else {
            interactive(client, o.max);
        }
    }

    /** Στέλνει ένα ερώτημα και επιστρέφει την απάντηση του server ως JSON κείμενο. */
    public String search(String query, int maxResults, int offset) throws IOException {
        SearchRequest request = SearchRequest.query(query, maxResults, offset);
        return sendRaw(Json.MAPPER.writeValueAsString(request));
    }

    /**
     * Το πρωτόκολλο: ανοίγουμε TCP σύνδεση, γράφουμε ΜΙΑ γραμμή JSON, διαβάζουμε ΜΙΑ γραμμή
     * JSON, κλείνουμε. Αν ο server δεν τρέχει, επιστρέφουμε JSON με "error" (όχι exception),
     * ώστε όποιος διαβάζει το stdout να το χειριστεί όπως κάθε άλλη απάντηση.
     */
    public String sendRaw(String requestJson) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            socket.setSoTimeout(timeoutMs);

            BufferedWriter out = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            out.write(requestJson);
            out.write('\n');
            out.flush();

            String response = in.readLine();
            return response != null ? response
                    : errorJson("Ο server έκλεισε τη σύνδεση χωρίς απάντηση");
        } catch (IOException e) {
            return errorJson("Δεν ήταν δυνατή η επικοινωνία με τον Query Server στο " + host + ":" + port
                    + " (" + e.getMessage() + "). Τρέχει το App 3;");
        }
    }

    /** (β) Διαδραστική λειτουργία για χειροκίνητη χρήση από την κονσόλα. */
    private static void interactive(QueryClient client, int max) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        OUT.println("=== App 4: Query Client (" + client.host + ":" + client.port + ") ===");
        OUT.println("Γράψε ερώτημα (π.χ. \"neural network\", retrieval AND evaluation).");
        OUT.println("Εντολές: :max N  |  :shutdown  |  :quit");
        while (true) {
            OUT.print("Ερώτημα> ");
            OUT.flush();
            String line = in.readLine();
            if (line == null || line.strip().equals(":quit")) {
                break;
            }
            line = line.strip();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith(":max ")) {
                max = Integer.parseInt(line.substring(5).strip());
                OUT.println("maxResults = " + max);
            } else if (line.equals(":shutdown")) {
                OUT.println(client.sendRaw(Json.MAPPER.writeValueAsString(SearchRequest.shutdown())));
                break;
            } else {
                OUT.println(pretty(client.search(line, max, 0)));
            }
        }
    }

    private static String errorJson(String message) {
        try {
            return Json.MAPPER.writeValueAsString(SearchResponse.error(message));
        } catch (IOException e) {
            return "{\"error\":\"client error\"}";
        }
    }

    private static boolean isError(String json) {
        try {
            return Json.MAPPER.readTree(json).has("error");
        } catch (IOException e) {
            return true;
        }
    }

    private static String pretty(String json) {
        try {
            return Json.MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(Json.MAPPER.readTree(json));
        } catch (IOException e) {
            return json;
        }
    }

    /** Τα ορίσματα της γραμμής εντολών. */
    private static final class Options {
        String host;
        int port;
        int max = 10;
        int offset = 0;
        boolean pretty;
        boolean stdin;
        boolean shutdown;
        String query;

        static Options parse(String[] args, AppConfig cfg) {
            Options o = new Options();
            o.host = cfg.serverHost();
            o.port = cfg.serverPort();
            List<String> positional = new ArrayList<>();
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--host" -> o.host = args[++i];
                    case "--port" -> o.port = Integer.parseInt(args[++i]);
                    case "--max" -> o.max = Integer.parseInt(args[++i]);
                    case "--offset" -> o.offset = Integer.parseInt(args[++i]);
                    case "--pretty" -> o.pretty = true;
                    case "--stdin" -> o.stdin = true;
                    case "--shutdown" -> o.shutdown = true;
                    default -> positional.add(args[i]);
                }
            }
            // Μορφή εκφώνησης: run-app4.bat "machine learning" 10
            // (αν το τελευταίο όρισμα είναι αριθμός, είναι το maxResults)
            if (positional.size() >= 2 && positional.get(positional.size() - 1).matches("\\d+")) {
                o.max = Integer.parseInt(positional.remove(positional.size() - 1));
            }
            if (!positional.isEmpty()) {
                o.query = String.join(" ", positional);
            }
            return o;
        }
    }
}
