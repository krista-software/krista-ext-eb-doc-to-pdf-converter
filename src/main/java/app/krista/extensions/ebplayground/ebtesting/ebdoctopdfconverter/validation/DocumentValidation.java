package app.krista.extensions.ebplayground.ebtesting.ebdoctopdfconverter.validation;

import java.util.Locale;

/**
 * Fail-fast validation for the "Convert Document to PDF" request. Pure Java, no Krista runtime
 * dependency other than the plain {@code app.krista.model.base.File} model type, so it is fully
 * unit-testable without a DI container.
 */
public final class DocumentValidation {
    private DocumentValidation() {}

    public enum WordFormat { DOCX, DOC }

    private static final byte[] ZIP_MAGIC = {0x50, 0x4B, 0x03, 0x04};
    private static final byte[] OLE2_MAGIC = {
            (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1
    };

    /** Throws if the input file reference itself is missing. */
    public static void requireFile(app.krista.model.base.File file) {
        if (file == null) {
            throw new IllegalArgumentException(
                    "File is required. Please attach a Word document (.doc or .docx).");
        }
    }

    /**
     * Validates the file name extension and sniffs the content's magic bytes to confirm it genuinely
     * looks like the declared format, catching corrupted or mislabeled uploads before conversion is
     * attempted.
     *
     * @throws IllegalArgumentException with an actionable, user-facing message on any validation failure
     */
    public static WordFormat validateWordDocument(String fileName, byte[] content) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException(
                    "The uploaded file has no file name. Please attach a file named with a .doc or .docx extension.");
        }
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException(
                    "The uploaded file '" + fileName + "' is empty. Please attach a valid Word document.");
        }

        String lower = fileName.toLowerCase(Locale.ROOT);
        WordFormat declared;
        if (lower.endsWith(".docx")) {
            declared = WordFormat.DOCX;
        } else if (lower.endsWith(".doc")) {
            declared = WordFormat.DOC;
        } else {
            throw new IllegalArgumentException(
                    "Unsupported file type for '" + fileName + "'. Only Word documents (.doc or .docx) are supported.");
        }

        if (declared == WordFormat.DOCX && !startsWith(content, ZIP_MAGIC)) {
            throw new IllegalArgumentException(
                    "'" + fileName + "' does not look like a valid .docx file (expected a ZIP/OOXML package). "
                            + "The file may be corrupted or mislabeled.");
        }
        if (declared == WordFormat.DOC && !startsWith(content, OLE2_MAGIC)) {
            throw new IllegalArgumentException(
                    "'" + fileName + "' does not look like a valid legacy .doc file (expected an OLE2 compound document). "
                            + "The file may be corrupted or mislabeled.");
        }
        return declared;
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }
}
