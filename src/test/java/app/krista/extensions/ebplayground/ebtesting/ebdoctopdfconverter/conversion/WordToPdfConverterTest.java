package app.krista.extensions.ebplayground.ebtesting.ebdoctopdfconverter.conversion;

import app.krista.extensions.ebplayground.ebtesting.ebdoctopdfconverter.validation.DocumentValidation.WordFormat;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WordToPdfConverterTest {

    @Test
    void convertsValidDocxToPdf() throws Exception {
        byte[] docx = buildDocx();

        byte[] pdfBytes = WordToPdfConverter.convert(docx, WordFormat.DOCX);

        assertValidPdf(pdfBytes);
        String text = extractText(pdfBytes);
        assertTrue(text.contains("Hello from a bold run"), "expected bold run text in output: " + text);
        assertTrue(text.contains("and a normal run."), "expected normal run text in output: " + text);
    }

    @Test
    void convertsPlainParagraphs_legacyDocRenderingEngine() throws Exception {
        // The legacy .doc path extracts plain paragraph text via HWPF/WordExtractor, then hands it to
        // this same rendering engine - exercised directly here since constructing a valid binary .doc
        // fixture from scratch isn't supported by POI (HWPFDocument has no no-arg "new empty doc" API,
        // unlike XWPFDocument). See WordToPdfConverter's class javadoc.
        List<String> paragraphs = List.of("Legacy doc test paragraph.", "", "Second paragraph of the legacy document.");

        byte[] pdfBytes = WordToPdfConverter.renderPlainParagraphsToPdf(paragraphs);

        assertValidPdf(pdfBytes);
        String text = extractText(pdfBytes);
        assertTrue(text.contains("Legacy doc test paragraph."), "expected first paragraph in output: " + text);
        assertTrue(text.contains("Second paragraph of the legacy document."), "expected second paragraph in output: " + text);
    }

    @Test
    void wrapsLongContentAcrossMultiplePages() throws Exception {
        List<String> paragraphs = new ArrayList<>();
        for (int i = 0; i < 120; i++) {
            paragraphs.add("This is repeated filler paragraph number " + i + " used to force a page break.");
        }

        byte[] pdfBytes = WordToPdfConverter.renderPlainParagraphsToPdf(paragraphs);

        try (PDDocument doc = PDDocument.load(pdfBytes)) {
            assertTrue(doc.getNumberOfPages() > 1, "expected multiple pages for long content");
        }
    }

    @Test
    void rejectsNullFormat() {
        assertThrows(IllegalArgumentException.class, () -> WordToPdfConverter.convert(new byte[]{1}, null));
    }

    @Test
    void handlesCorruptedOrUnreadableDocumentGracefully() {
        byte[] garbage = {0x01, 0x02, 0x03, 0x04, 0x05};

        DocumentConversionException ex = assertThrows(DocumentConversionException.class,
                () -> WordToPdfConverter.convert(garbage, WordFormat.DOCX));
        assertTrue(ex.getMessage().toLowerCase().contains("corrupted") || ex.getMessage().toLowerCase().contains("read")
                        || ex.getMessage().toLowerCase().contains("parsed"),
                "expected an actionable message, got: " + ex.getMessage());
    }

    @Test
    void selfTestPassesAgainstTheRealConversionPath() {
        assertDoesNotThrow(WordToPdfConverter::selfTest);
    }

    private static byte[] buildDocx() throws Exception {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            XWPFParagraph para = doc.createParagraph();
            para.setAlignment(ParagraphAlignment.LEFT);

            XWPFRun bold = para.createRun();
            bold.setBold(true);
            bold.setText("Hello from a bold run");

            XWPFRun normal = para.createRun();
            normal.setText(" and a normal run.");

            doc.write(bos);
            return bos.toByteArray();
        }
    }

    private static void assertValidPdf(byte[] pdfBytes) throws Exception {
        assertTrue(pdfBytes != null && pdfBytes.length > 4, "expected non-empty PDF bytes");
        assertEquals("%PDF", new String(pdfBytes, 0, 4, java.nio.charset.StandardCharsets.US_ASCII));
        try (PDDocument doc = PDDocument.load(pdfBytes)) {
            assertTrue(doc.getNumberOfPages() >= 1, "expected at least one page");
        }
    }

    private static String extractText(byte[] pdfBytes) throws Exception {
        try (PDDocument doc = PDDocument.load(pdfBytes)) {
            return new PDFTextStripper().getText(doc);
        }
    }
}
