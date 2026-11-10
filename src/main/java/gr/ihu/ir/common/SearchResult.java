package gr.ihu.ir.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Ένα αποτέλεσμα της λίστας αποτελεσμάτων.
 * Υποχρεωτικά πεδία (εκφώνηση): filename, path, score.
 * Επιπλέον (προαιρετικά): rank, docno, title, snippet.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"rank", "filename", "path", "score", "docno", "title", "snippet"})
public class SearchResult {

    public Integer rank;
    public String filename;
    public String path;
    public double score;
    public String docno;
    public String title;
    public String snippet;

    public SearchResult() {
    }
}
