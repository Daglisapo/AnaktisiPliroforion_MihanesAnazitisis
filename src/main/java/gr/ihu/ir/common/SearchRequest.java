package gr.ihu.ir.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Request από τον Client (App 4) προς τον Query Server (App 3), μία γραμμή JSON.
 *
 * Κανονικό ερώτημα (διαφάνεια 15 της εκφώνησης):
 * <pre>{ "query": "machine learning", "maxResults": 10 }</pre>
 * Προαιρετικό πεδίο για pagination:
 * <pre>{ "query": "machine learning", "maxResults": 10, "offset": 20 }</pre>
 * Ειδικό αίτημα τερματισμού του server:
 * <pre>{ "command": "shutdown" }</pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)   // δεν γράφουμε πεδία που είναι null
public class SearchRequest {

    public String query;
    public Integer maxResults;
    public Integer offset;
    public String command;

    public SearchRequest() {
    }

    public static SearchRequest query(String query, int maxResults, int offset) {
        SearchRequest r = new SearchRequest();
        r.query = query;
        r.maxResults = maxResults;
        r.offset = offset > 0 ? offset : null;   // το offset γράφεται μόνο όταν χρειάζεται
        return r;
    }

    public static SearchRequest shutdown() {
        SearchRequest r = new SearchRequest();
        r.command = "shutdown";
        return r;
    }

    public boolean isShutdown() {
        return "shutdown".equalsIgnoreCase(command);
    }
}
