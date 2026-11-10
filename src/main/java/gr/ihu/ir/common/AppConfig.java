package gr.ihu.ir.common;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Κεντρικές ρυθμίσεις και για τις 5 εφαρμογές (paths, ports, όρια).
 *
 * ΓΙΑΤΙ υπάρχει: τα paths των δεδομένων (PDF, repository, index) δεν πρέπει να είναι
 * γραμμένα «σκληρά» σε 5 διαφορετικά σημεία του κώδικα. Τα διαβάζουμε από ΕΝΑ αρχείο
 * {@code config.properties}.
 *
 * Σειρά προτεραιότητας (η επόμενη «νικάει» την προηγούμενη):
 *   1. προεπιλογές μέσα σε αυτή την κλάση,
 *   2. το αρχείο config.properties (στον τρέχοντα φάκελο ή όπου δείχνει το -Dconfig=...),
 *   3. System properties από τη γραμμή εντολών, π.χ. {@code java -Dserver.port=6000 ...}.
 */
public final class AppConfig {

    private final Properties props = new Properties();

    private AppConfig() {
        // 1. Προεπιλογές
        props.setProperty("pdf.dir", "C:/IR_data/arxiv_papers");
        props.setProperty("repo.dir", "C:/IR_data/repository");
        props.setProperty("index.dir", "C:/IR_data/index");
        props.setProperty("repo.max.file.mb", "128");
        props.setProperty("store.threads", String.valueOf(Math.max(1, Runtime.getRuntime().availableProcessors() - 1)));
        props.setProperty("server.host", "127.0.0.1");
        props.setProperty("server.port", "5555");
        props.setProperty("server.max.results", "100");
        props.setProperty("server.default.operator", "OR");
        props.setProperty("client.timeout.ms", "30000");
        props.setProperty("ui.port", "8080");
        props.setProperty("ui.results.per.page", "10");
    }

    /** Φορτώνει τις ρυθμίσεις με τη σειρά προτεραιότητας που περιγράφεται παραπάνω. */
    public static AppConfig load() {
        AppConfig cfg = new AppConfig();

        // 2. Αρχείο config.properties
        Path file = Path.of(System.getProperty("config", "config.properties"));
        if (Files.isRegularFile(file)) {
            try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                cfg.props.load(r);
            } catch (IOException e) {
                System.err.println("[config] Αδυναμία ανάγνωσης " + file + ": " + e.getMessage());
            }
        }

        // 3. System properties (-Dκλειδί=τιμή) υπερισχύουν
        for (String key : cfg.props.stringPropertyNames()) {
            String override = System.getProperty(key);
            if (override != null) {
                cfg.props.setProperty(key, override);
            }
        }
        return cfg;
    }

    public String get(String key) {
        return props.getProperty(key);
    }

    public int getInt(String key) {
        return Integer.parseInt(props.getProperty(key).trim());
    }

    public Path getPath(String key) {
        return Path.of(props.getProperty(key).trim());
    }

    // --- Βολικοί getters για τα πιο συχνά κλειδιά ---

    public Path pdfDir()     { return getPath("pdf.dir"); }
    public Path repoDir()    { return getPath("repo.dir"); }
    public Path indexDir()   { return getPath("index.dir"); }
    public String serverHost() { return get("server.host").trim(); }
    public int serverPort()  { return getInt("server.port"); }
}
