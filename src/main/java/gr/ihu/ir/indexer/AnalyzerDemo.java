package gr.ihu.ir.indexer;

import gr.ihu.ir.common.TextAnalysis;
import org.apache.lucene.analysis.Analyzer;

/**
 * Επίδειξη της προεπεξεργασίας (ΔΕΝ είναι ένα από τα 5 apps): τυπώνει μια πρόταση πριν
 * και μετά τον EnglishAnalyzer — το αντίστοιχο του παραδείγματος του μαθήματος
 * «αυτοκίνητο / αυτοκίνητα / αυτοκινήτων → ίδια ρίζα».
 *
 * Χρήση:  analyze.bat "Neural Networks and networking: the network's performance"
 */
public class AnalyzerDemo {

    public static void main(String[] args) throws Exception {
        String text = args.length > 0 ? String.join(" ", args)
                : "Deep Neural Networks for Information Retrieval: networking, the network's evaluation.";
        Analyzer analyzer = TextAnalysis.newAnalyzer();
        System.out.println("Πριν : " + text);
        System.out.println("Μετά : " + TextAnalysis.tokens(analyzer, text));
    }
}
