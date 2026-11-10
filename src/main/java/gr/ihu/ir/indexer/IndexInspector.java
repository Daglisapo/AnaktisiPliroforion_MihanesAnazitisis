package gr.ihu.ir.indexer;

import gr.ihu.ir.common.AppConfig;
import gr.ihu.ir.common.Fields;
import gr.ihu.ir.common.TextAnalysis;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.MultiTerms;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Εργαλείο ελέγχου του αντεστραμμένου ευρετηρίου (ΔΕΝ είναι ένα από τα 5 apps).
 *
 * Για κάθε λέξη που δίνουμε δείχνει τη «γραμμή» του λεξικού (dictionary) του index:
 *   - df  (document frequency) = σε πόσα έγγραφα εμφανίζεται ο όρος (μήκος της postings list),
 *   - cf  (collection frequency) = πόσες φορές συνολικά εμφανίζεται σε όλη τη συλλογή.
 * Με --top N δείχνει τους N όρους με τη μεγαλύτερη df.
 *
 * Χρήση:  inspect-index.bat network retrieval learning --top 15
 */
public class IndexInspector {

    public static void main(String[] args) throws Exception {
        AppConfig cfg = AppConfig.load();
        Analyzer analyzer = TextAnalysis.newAnalyzer();
        int top = 0;
        List<String> words = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--top") && i + 1 < args.length) {
                top = Integer.parseInt(args[++i]);
            } else {
                words.add(args[i]);
            }
        }

        try (DirectoryReader reader = DirectoryReader.open(FSDirectory.open(cfg.indexDir()))) {
            System.out.println("Index: " + cfg.indexDir());
            System.out.println("Έγγραφα (numDocs): " + reader.numDocs());
            Terms terms = MultiTerms.getTerms(reader, Fields.CONTENT);
            if (terms != null) {
                System.out.println("Διαφορετικοί όροι στο πεδίο content: " + terms.size());
                System.out.println("Σύνολο tokens (sumTotalTermFreq): " + terms.getSumTotalTermFreq());
            }

            for (String word : words) {
                // Η λέξη περνάει από τον ΙΔΙΟ analyzer → παίρνουμε το stem που υπάρχει στο index.
                for (String stem : TextAnalysis.tokens(analyzer, word)) {
                    Term t = new Term(Fields.CONTENT, stem);
                    System.out.printf("  %-15s → όρος '%s': df=%d έγγραφα, cf=%d εμφανίσεις%n",
                            word, stem, reader.docFreq(t), reader.totalTermFreq(t));
                }
            }

            if (top > 0 && terms != null) {
                printTopTerms(terms, top);
            }
        }
    }

    /** Διατρέχει όλο το λεξικό και κρατά τους N όρους με τη μεγαλύτερη df (min-heap). */
    private static void printTopTerms(Terms terms, int n) throws Exception {
        PriorityQueue<TermStat> heap = new PriorityQueue<>((a, b) -> Integer.compare(a.df(), b.df()));
        TermsEnum it = terms.iterator();
        BytesRef term;
        while ((term = it.next()) != null) {
            heap.add(new TermStat(term.utf8ToString(), it.docFreq(), it.totalTermFreq()));
            if (heap.size() > n) {
                heap.poll();   // πετάμε τον όρο με τη μικρότερη df
            }
        }
        List<TermStat> list = new ArrayList<>(heap);
        list.sort((a, b) -> Integer.compare(b.df(), a.df()));
        System.out.println("Top " + n + " όροι κατά df:");
        for (TermStat s : list) {
            System.out.printf("  %-15s df=%-6d cf=%d%n", s.term(), s.df(), s.cf());
        }
    }

    private record TermStat(String term, int df, long cf) {
    }
}
