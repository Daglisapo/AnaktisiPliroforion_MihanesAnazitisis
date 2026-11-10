package gr.ihu.ir.indexer;

import gr.ihu.ir.common.RepoDocument;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Βοηθητικό για tests: φτιάχνει index στη μνήμη με τον ΙΔΙΟ τρόπο που το φτιάχνει ο Indexer. */
public final class TestIndexes {

    private TestIndexes() {
    }

    public static Directory inMemory(Analyzer analyzer, List<RepoDocument> docs) throws IOException {
        Directory dir = new ByteBuffersDirectory();
        try (IndexWriter w = new IndexWriter(dir, new IndexWriterConfig(analyzer))) {
            for (RepoDocument d : docs) {
                w.addDocument(LuceneDocumentFactory.create(d, Path.of("pdfs")));
            }
        }
        return dir;
    }
}
