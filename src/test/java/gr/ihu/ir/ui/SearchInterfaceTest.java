package gr.ihu.ir.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Ασφάλεια της διεπαφής: path traversal στο /pdf και HTML escaping. */
class SearchInterfaceTest {

    @TempDir
    Path dir;

    @Test
    void servesOnlyPdfsInsideTheFolder() throws Exception {
        Files.writeString(dir.resolve("1234.5678v1.pdf"), "%PDF");
        SearchInterface ui = new SearchInterface(null, dir, 10);

        assertNotNull(ui.resolvePdf("1234.5678v1.pdf"));
        assertNull(ui.resolvePdf("..\\..\\Windows\\win.ini"));
        assertNull(ui.resolvePdf("../secret.pdf"));
        assertNull(ui.resolvePdf("C:\\x.pdf"));
        assertNull(ui.resolvePdf("missing.pdf"));
        assertNull(ui.resolvePdf("notes.txt"));
    }

    @Test
    void highlightEscapesHtml() {
        String html = ResultsPage.highlight("<script> neural networks </script>", List.of("network"));
        assertEquals("&lt;script&gt; neural <mark>networks</mark> &lt;/script&gt;", html);
    }
}
