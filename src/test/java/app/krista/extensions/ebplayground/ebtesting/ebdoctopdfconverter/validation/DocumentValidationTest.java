package app.krista.extensions.ebplayground.ebtesting.ebdoctopdfconverter.validation;

import app.krista.extensions.ebplayground.ebtesting.ebdoctopdfconverter.validation.DocumentValidation.WordFormat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentValidationTest {

    private static final byte[] ZIP_BYTES = {0x50, 0x4B, 0x03, 0x04, 0x00, 0x00};
    private static final byte[] OLE2_BYTES =
            {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1, 0x00};
    private static final byte[] GARBAGE_BYTES = {0x01, 0x02, 0x03, 0x04};

    @Test
    void requireFile_nullFile_throws() {
        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> DocumentValidation.requireFile(null));
        assertEquals(true, ex.getMessage().contains("File is required"));
    }

    @Test
    void validatesDocxMagicBytes() {
        WordFormat format = DocumentValidation.validateWordDocument("report.docx", ZIP_BYTES);
        assertEquals(WordFormat.DOCX, format);
    }

    @Test
    void validatesLegacyDocMagicBytes() {
        WordFormat format = DocumentValidation.validateWordDocument("report.doc", OLE2_BYTES);
        assertEquals(WordFormat.DOC, format);
    }

    @Test
    void isCaseInsensitiveOnExtension() {
        assertEquals(WordFormat.DOCX, DocumentValidation.validateWordDocument("Report.DOCX", ZIP_BYTES));
    }

    @Test
    void rejectsUnsupportedFileType() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> DocumentValidation.validateWordDocument("report.pdf", ZIP_BYTES));
        assertEquals(true, ex.getMessage().contains("Unsupported file type"));
    }

    @Test
    void rejectsMissingExtension() {
        assertThrows(IllegalArgumentException.class,
                () -> DocumentValidation.validateWordDocument("report", ZIP_BYTES));
    }

    @Test
    void rejectsBlankFileName() {
        assertThrows(IllegalArgumentException.class,
                () -> DocumentValidation.validateWordDocument("  ", ZIP_BYTES));
    }

    @Test
    void rejectsNullContent() {
        assertThrows(IllegalArgumentException.class,
                () -> DocumentValidation.validateWordDocument("report.docx", null));
    }

    @Test
    void rejectsEmptyContent() {
        assertThrows(IllegalArgumentException.class,
                () -> DocumentValidation.validateWordDocument("report.docx", new byte[0]));
    }

    @Test
    void rejectsMismatchedMagicBytes_docxExtensionWithOle2Content() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> DocumentValidation.validateWordDocument("report.docx", OLE2_BYTES));
        assertEquals(true, ex.getMessage().contains("does not look like a valid .docx file"));
    }

    @Test
    void rejectsMismatchedMagicBytes_docExtensionWithGarbageContent() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> DocumentValidation.validateWordDocument("report.doc", GARBAGE_BYTES));
        assertEquals(true, ex.getMessage().contains("does not look like a valid legacy .doc file"));
    }
}
