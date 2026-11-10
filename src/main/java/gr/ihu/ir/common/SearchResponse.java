package gr.ihu.ir.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * Απάντηση του Query Server (App 3), μία γραμμή JSON. Ίδια πεδία με τη διαφάνεια 15:
 * <pre>
 * { "totalHits": 2,
 *   "results": [ { "filename": "document1.pdf", "path": "document1.pdf", "score": 4.82 }, ... ] }
 * </pre>
 * Σε περίπτωση λάθους η απάντηση είναι μόνο: <pre>{ "error": "..." }</pre>
 * (τα null πεδία δεν γράφονται χάρη στο NON_NULL).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchResponse {

    /** Συνολικό πλήθος εγγράφων που ταιριάζουν (όχι μόνο όσα επιστρέφονται). */
    public Long totalHits;

    /** Τα αποτελέσματα της τρέχουσας «σελίδας», ταξινομημένα κατά φθίνον score. */
    public List<SearchResult> results;

    // --- Προαιρετικά/επιπλέον πεδία ---
    public Integer offset;
    public Long tookMs;
    public String error;
    public String status;

    public SearchResponse() {
    }

    public static SearchResponse error(String message) {
        SearchResponse r = new SearchResponse();
        r.error = message;
        return r;
    }

    public static SearchResponse status(String status) {
        SearchResponse r = new SearchResponse();
        r.status = status;
        return r;
    }

    public static SearchResponse hits(long totalHits, int offset) {
        SearchResponse r = new SearchResponse();
        r.totalHits = totalHits;
        r.offset = offset;
        r.results = new ArrayList<>();
        return r;
    }
}
