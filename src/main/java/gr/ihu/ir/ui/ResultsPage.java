package gr.ihu.ir.ui;

import gr.ihu.ir.common.SearchResponse;
import gr.ihu.ir.common.SearchResult;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Φτιάχνει το HTML της σελίδας αναζήτησης (server-side, χωρίς JavaScript/frameworks).
 * Το «σκελετό» της σελίδας (CSS + φόρμα) βρίσκεται στο resources/ui/page.html και εδώ
 * απλώς συμπληρώνουμε τα {{...}} κενά.
 *
 * ΑΣΦΑΛΕΙΑ: ό,τι προέρχεται από τον χρήστη ή από τα PDF περνά από escape() πριν μπει στο
 * HTML, ώστε ένα κείμενο σαν "<script>" να εμφανίζεται ως κείμενο και να μην εκτελείται.
 */
final class ResultsPage {

    private static final String TEMPLATE = loadTemplate();

    private ResultsPage() {
    }

    /** Η αρχική σελίδα, χωρίς αποτελέσματα. */
    static String empty() {
        return TEMPLATE.replace("{{QUERY}}", "").replace("{{CONTENT}}", "");
    }

    /** Σελίδα με τα αποτελέσματα ενός ερωτήματος. */
    static String render(String query, SearchResponse resp, int page, int perPage, long elapsedMs) {
        StringBuilder html = new StringBuilder();

        if (resp.error != null) {
            html.append("<div class=\"msg error\">").append(escape(resp.error)).append("</div>");
        } else if (resp.totalHits == null || resp.totalHits == 0) {
            html.append("<div class=\"msg\">Δεν βρέθηκαν έγγραφα για <b>")
                .append(escape(query)).append("</b>.</div>");
        } else {
            int from = (page - 1) * perPage + 1;
            int to = from + resp.results.size() - 1;
            html.append("<div class=\"stats\">Αποτελέσματα ").append(from).append("–").append(to)
                .append(" από ").append(String.format(Locale.ROOT, "%,d", resp.totalHits))
                .append(" (").append(elapsedMs).append(" ms)</div>");

            List<String> words = queryWords(query);
            html.append("<ol class=\"results\" start=\"").append(from).append("\">");
            for (SearchResult r : resp.results) {
                appendResult(html, r, words);
            }
            html.append("</ol>");
            appendPager(html, query, page, perPage, resp.totalHits);
        }
        return TEMPLATE.replace("{{QUERY}}", escape(query)).replace("{{CONTENT}}", html.toString());
    }

    private static void appendResult(StringBuilder html, SearchResult r, List<String> words) {
        String pdfLink = "/pdf?file=" + url(r.filename);
        String title = (r.title == null || r.title.isBlank()) ? r.filename : r.title;
        html.append("<li class=\"result\">")
            .append("<a class=\"title\" href=\"").append(pdfLink).append("\" target=\"_blank\">")
            .append(escape(title)).append("</a>")
            .append("<div class=\"meta\">").append(escape(r.filename))
            .append(" &middot; score ").append(String.format(Locale.ROOT, "%.3f", r.score))
            .append("</div>");
        if (r.snippet != null) {
            html.append("<div class=\"snippet\">").append(highlight(r.snippet, words)).append("</div>");
        }
        html.append("</li>");
    }

    private static void appendPager(StringBuilder html, String query, int page, int perPage, long total) {
        long pages = (total + perPage - 1) / perPage;
        if (pages <= 1) {
            return;
        }
        html.append("<div class=\"pager\">");
        if (page > 1) {
            html.append("<a href=\"/?q=").append(url(query)).append("&page=").append(page - 1)
                .append("\">&laquo; Προηγούμενη</a>");
        }
        html.append("<span>Σελίδα ").append(page).append(" από ").append(pages).append("</span>");
        if (page < pages) {
            html.append("<a href=\"/?q=").append(url(query)).append("&page=").append(page + 1)
                .append("\">Επόμενη &raquo;</a>");
        }
        html.append("</div>");
    }

    /**
     * Τονίζει (με &lt;mark&gt;) τις λέξεις του snippet που ξεκινούν με μια λέξη του ερωτήματος
     * (π.χ. το "network" τονίζει και το "networks"). Απλή, οπτική μόνο, βοήθεια.
     */
    static String highlight(String snippet, List<String> words) {
        if (words.isEmpty()) {
            return escape(snippet);
        }
        List<String> quoted = new ArrayList<>();
        for (String w : words) {
            quoted.add(Pattern.quote(w));
        }
        Pattern p = Pattern.compile("(?iu)\\b(?:" + String.join("|", quoted) + ")\\w*");
        Matcher m = p.matcher(snippet);
        StringBuilder sb = new StringBuilder();
        int last = 0;
        while (m.find()) {
            sb.append(escape(snippet.substring(last, m.start())))
              .append("<mark>").append(escape(m.group())).append("</mark>");
            last = m.end();
        }
        sb.append(escape(snippet.substring(last)));
        return sb.toString();
    }

    /** Οι «πραγματικές» λέξεις του ερωτήματος (χωρίς AND/OR/NOT και σύμβολα). */
    static List<String> queryWords(String query) {
        List<String> words = new ArrayList<>();
        for (String w : query.split("[^\\p{L}\\p{N}]+")) {
            if (w.length() >= 3 && !w.equals("AND") && !w.equals("OR") && !w.equals("NOT")) {
                words.add(w.toLowerCase(Locale.ROOT));
            }
        }
        return words;
    }

    static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;")
                .replace("{", "&#123;");   // ώστε κείμενο σαν "{{CONTENT}}" να μη μπερδέψει το template
    }

    private static String url(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String loadTemplate() {
        try (InputStream in = ResultsPage.class.getResourceAsStream("/ui/page.html")) {
            if (in == null) {
                throw new IllegalStateException("Λείπει το resource /ui/page.html");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
