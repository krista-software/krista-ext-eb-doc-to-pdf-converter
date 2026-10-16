package app.krista.extensions.ebplayground.ebtesting.ebdoctopdfconverter.conversion;

import app.krista.extensions.ebplayground.ebtesting.ebdoctopdfconverter.validation.DocumentValidation.WordFormat;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts a Word document (.docx via OOXML/XWPF, legacy .doc via HWPF) to PDF entirely in-process,
 * using Apache POI to read the source document and PDFBox to render the output - no external API.
 *
 * <p>Fidelity is intentionally modest rather than a pixel-perfect re-layout of Word's own renderer:
 * paragraph text, bold/italic emphasis, and left/center/right alignment carry over for .docx; tables
 * render as plain delimited rows. Legacy .doc text is rendered as plain paragraphs without per-run
 * styling (HWPF's run-level style API is considerably more involved than XWPF's, and style fidelity for
 * the increasingly rare legacy binary format was judged not worth that complexity here).</p>
 */
public final class WordToPdfConverter {
    private WordToPdfConverter() {}

    private static final float MARGIN = 50f;
    private static final float PAGE_WIDTH = PDRectangle.LETTER.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.LETTER.getHeight();
    private static final float MAX_LINE_WIDTH = PAGE_WIDTH - 2 * MARGIN;
    private static final float DEFAULT_FONT_SIZE = 11f;
    private static final float LINE_HEIGHT_FACTOR = 1.25f;
    private static final float PARAGRAPH_GAP_FACTOR = 0.35f;

    /**
     * Converts the given Word document bytes to PDF bytes.
     *
     * @throws DocumentConversionException if the document is encrypted, corrupted, or otherwise can't
     *         be parsed - the message is safe to show to the end user.
     */
    public static byte[] convert(byte[] content, WordFormat format) {
        if (format == null) {
            throw new IllegalArgumentException("Document format is required.");
        }
        try {
            return format == WordFormat.DOCX ? renderDocxToPdf(content) : renderDocToPdf(content);
        } catch (EncryptedDocumentException e) {
            throw new DocumentConversionException(
                    "This document is password-protected and can't be converted. Remove the password and try again.", e);
        } catch (IOException e) {
            throw new DocumentConversionException(
                    "The document couldn't be read - it may be corrupted or not a genuine Word file.", e);
        } catch (RuntimeException e) {
            // POI throws a variety of unchecked parse exceptions for malformed/unsupported document
            // internals (e.g. a mislabeled file that passed the extension/magic-byte check but whose
            // internal structure doesn't actually match).
            throw new DocumentConversionException(
                    "The document couldn't be parsed - it may be corrupted or use an unsupported Word format variant.", e);
        }
    }

    /** Exercises the real conversion path end-to-end against a tiny in-memory document. Used by the
     * Setup area's Test Connection request to prove the bundled libraries initialize correctly. */
    public static void selfTest() {
        try {
            byte[] docx;
            try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                XWPFParagraph p = doc.createParagraph();
                XWPFRun r = p.createRun();
                r.setText("Krista Doc to PDF self test");
                doc.write(bos);
                docx = bos.toByteArray();
            }
            byte[] pdf = convert(docx, WordFormat.DOCX);
            if (pdf == null || pdf.length < 4 || pdf[0] != '%' || pdf[1] != 'P' || pdf[2] != 'D' || pdf[3] != 'F') {
                throw new IllegalStateException("Self-test conversion did not produce a valid PDF.");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Document conversion self-test failed: " + e.getMessage(), e);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // .docx (XWPF)
    // ---------------------------------------------------------------------------------------------

    private static byte[] renderDocxToPdf(byte[] content) throws IOException {
        try (PDDocument pdf = new PDDocument();
             ByteArrayInputStream bis = new ByteArrayInputStream(content);
             XWPFDocument doc = new XWPFDocument(bis)) {
            PdfCanvas canvas = new PdfCanvas(pdf);
            for (IBodyElement element : doc.getBodyElements()) {
                if (element instanceof XWPFParagraph) {
                    renderParagraph(canvas, (XWPFParagraph) element);
                } else if (element instanceof XWPFTable) {
                    renderTable(canvas, (XWPFTable) element);
                }
            }
            canvas.close();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            pdf.save(out);
            return out.toByteArray();
        }
    }

    private static void renderParagraph(PdfCanvas canvas, XWPFParagraph para) throws IOException {
        float lineHeight = DEFAULT_FONT_SIZE * LINE_HEIGHT_FACTOR;
        List<Token> tokens = tokenize(para);
        if (tokens.isEmpty()) {
            canvas.ensureRoom(lineHeight);
            canvas.y -= lineHeight;
            return;
        }
        ParagraphAlignment align = para.getAlignment();
        for (List<Token> line : wrapTokens(tokens)) {
            canvas.ensureRoom(lineHeight);
            float x = startX(align, lineWidth(line));
            for (Token t : line) {
                canvas.drawTextAt(t.text, t.font, t.size, x);
                x += t.width + spaceWidth(t.font, t.size);
            }
            canvas.y -= lineHeight;
        }
        canvas.ensureRoom(lineHeight * PARAGRAPH_GAP_FACTOR);
        canvas.y -= lineHeight * PARAGRAPH_GAP_FACTOR;
    }

    private static List<Token> tokenize(XWPFParagraph para) throws IOException {
        List<Token> tokens = new ArrayList<>();
        for (XWPFRun run : para.getRuns()) {
            String text = sanitize(run.text());
            if (text.isBlank()) continue;
            PDFont font = fontFor(run.isBold(), run.isItalic());
            int sz = run.getFontSize();
            float size = sz > 0 ? sz : DEFAULT_FONT_SIZE;
            for (String word : text.split("\\s+")) {
                if (word.isBlank()) continue;
                tokens.add(new Token(word, font, size, font.getStringWidth(word) / 1000f * size));
            }
        }
        return tokens;
    }

    // ---------------------------------------------------------------------------------------------
    // Table rendering (both docx) - plain delimited rows, regular style
    // ---------------------------------------------------------------------------------------------

    private static void renderTable(PdfCanvas canvas, XWPFTable table) throws IOException {
        float lineHeight = DEFAULT_FONT_SIZE * LINE_HEIGHT_FACTOR;
        for (XWPFTableRow row : table.getRows()) {
            StringBuilder sb = new StringBuilder();
            for (XWPFTableCell cell : row.getTableCells()) {
                if (sb.length() > 0) sb.append("  |  ");
                String cellText = cell.getText();
                sb.append(cellText == null ? "" : cellText.trim());
            }
            renderPlainLine(canvas, sanitize(sb.toString()), lineHeight);
        }
        canvas.ensureRoom(lineHeight * PARAGRAPH_GAP_FACTOR);
        canvas.y -= lineHeight * PARAGRAPH_GAP_FACTOR;
    }

    // ---------------------------------------------------------------------------------------------
    // Legacy .doc (HWPF) - plain paragraphs, no per-run styling (see class javadoc)
    // ---------------------------------------------------------------------------------------------

    private static byte[] renderDocToPdf(byte[] content) throws IOException {
        List<String> paragraphs;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(content);
             HWPFDocument hwpf = new HWPFDocument(bis)) {
            WordExtractor extractor = new WordExtractor(hwpf);
            try {
                paragraphs = List.of(extractor.getParagraphText());
            } finally {
                extractor.close();
            }
        }
        return renderPlainParagraphsToPdf(paragraphs);
    }

    /**
     * Renders a sequence of already-extracted plain-text paragraphs to PDF. Deliberately independent of
     * HWPF/XWPF so it can be unit-tested directly without needing a binary document fixture - this is
     * the actual layout engine used by the legacy .doc path.
     */
    public static byte[] renderPlainParagraphsToPdf(List<String> paragraphs) throws IOException {
        float lineHeight = DEFAULT_FONT_SIZE * LINE_HEIGHT_FACTOR;
        try (PDDocument pdf = new PDDocument()) {
            PdfCanvas canvas = new PdfCanvas(pdf);
            for (String raw : paragraphs) {
                String text = sanitize(raw == null ? "" : raw.trim());
                renderPlainLine(canvas, text, lineHeight);
            }
            canvas.close();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            pdf.save(out);
            return out.toByteArray();
        }
    }

    private static void renderPlainLine(PdfCanvas canvas, String text, float lineHeight) throws IOException {
        if (text.isEmpty()) {
            canvas.ensureRoom(lineHeight);
            canvas.y -= lineHeight;
            return;
        }
        List<Token> tokens = new ArrayList<>();
        for (String word : text.split("\\s+")) {
            if (word.isBlank()) continue;
            float width = PDType1Font.HELVETICA.getStringWidth(word) / 1000f * DEFAULT_FONT_SIZE;
            tokens.add(new Token(word, PDType1Font.HELVETICA, DEFAULT_FONT_SIZE, width));
        }
        for (List<Token> line : wrapTokens(tokens)) {
            canvas.ensureRoom(lineHeight);
            float x = MARGIN;
            for (Token t : line) {
                canvas.drawTextAt(t.text, t.font, t.size, x);
                x += t.width + spaceWidth(t.font, t.size);
            }
            canvas.y -= lineHeight;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Shared line-wrapping / text helpers
    // ---------------------------------------------------------------------------------------------

    static List<List<Token>> wrapTokens(List<Token> tokens) throws IOException {
        List<List<Token>> lines = new ArrayList<>();
        List<Token> current = new ArrayList<>();
        float currentWidth = 0f;
        for (Token t : tokens) {
            float extra = current.isEmpty() ? t.width : spaceWidth(t.font, t.size) + t.width;
            if (!current.isEmpty() && currentWidth + extra > MAX_LINE_WIDTH) {
                lines.add(current);
                current = new ArrayList<>();
                currentWidth = 0f;
                extra = t.width;
            }
            current.add(t);
            currentWidth += extra;
        }
        if (!current.isEmpty()) lines.add(current);
        return lines;
    }

    static float lineWidth(List<Token> line) throws IOException {
        float width = 0f;
        for (int i = 0; i < line.size(); i++) {
            Token t = line.get(i);
            width += t.width;
            if (i < line.size() - 1) width += spaceWidth(t.font, t.size);
        }
        return width;
    }

    private static float startX(ParagraphAlignment align, float lineWidth) {
        if (align == ParagraphAlignment.CENTER) return MARGIN + (MAX_LINE_WIDTH - lineWidth) / 2f;
        if (align == ParagraphAlignment.RIGHT || align == ParagraphAlignment.END) return MARGIN + (MAX_LINE_WIDTH - lineWidth);
        return MARGIN;
    }

    private static float spaceWidth(PDFont font, float size) throws IOException {
        return font.getStringWidth(" ") / 1000f * size;
    }

    private static PDFont fontFor(boolean bold, boolean italic) {
        if (bold && italic) return PDType1Font.HELVETICA_BOLD_OBLIQUE;
        if (bold) return PDType1Font.HELVETICA_BOLD;
        if (italic) return PDType1Font.HELVETICA_OBLIQUE;
        return PDType1Font.HELVETICA;
    }

    /** Replaces characters Helvetica/WinAnsiEncoding can't render (smart quotes, em-dashes, ...). */
    static String sanitize(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '‘': case '’': c = '\''; break;
                case '“': case '”': c = '"'; break;
                case '–': case '—': c = '-'; break;
                case ' ': c = ' '; break;
                case '…': sb.append("..."); continue;
                default: break;
            }
            sb.append(c <= 255 ? c : '?');
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------------------------------------
    // Internal rendering state
    // ---------------------------------------------------------------------------------------------

    static final class Token {
        final String text;
        final PDFont font;
        final float size;
        final float width;

        Token(String text, PDFont font, float size, float width) {
            this.text = text;
            this.font = font;
            this.size = size;
            this.width = width;
        }
    }

    /** Owns the current page/content-stream and starts a new page automatically when space runs out. */
    private static final class PdfCanvas implements AutoCloseable {
        private final PDDocument pdf;
        private PDPageContentStream stream;
        float y;

        PdfCanvas(PDDocument pdf) throws IOException {
            this.pdf = pdf;
            newPage();
        }

        void ensureRoom(float needed) throws IOException {
            if (y - needed < MARGIN) newPage();
        }

        void drawTextAt(String text, PDFont font, float size, float x) throws IOException {
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtOffset(x, y);
            stream.showText(text);
            stream.endText();
        }

        private void newPage() throws IOException {
            if (stream != null) stream.close();
            PDPage page = new PDPage(PDRectangle.LETTER);
            pdf.addPage(page);
            stream = new PDPageContentStream(pdf, page);
            y = PAGE_HEIGHT - MARGIN;
        }

        @Override
        public void close() throws IOException {
            if (stream != null) stream.close();
        }
    }
}
