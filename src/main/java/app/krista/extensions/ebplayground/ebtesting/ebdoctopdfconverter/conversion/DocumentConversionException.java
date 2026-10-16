package app.krista.extensions.ebplayground.ebtesting.ebdoctopdfconverter.conversion;

/**
 * The uploaded Word document could not be converted - it may be corrupted, password-protected, or use
 * a document-format variant the converter doesn't support. Carries a friendly, user-facing message;
 * the original cause is preserved for logging only.
 */
public class DocumentConversionException extends RuntimeException {
    public DocumentConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
