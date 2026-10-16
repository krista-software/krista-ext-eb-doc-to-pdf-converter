package app.krista.extensions.ebplayground.ebtesting.ebdoctopdfconverter.catalog;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.ExtensionResponse.Error.ExceptionType;
import app.krista.extension.executor.Invoker;
import app.krista.extension.impl.anno.Attribute;
import app.krista.extension.impl.anno.CatalogRequest;
import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Field;
import app.krista.extensions.ebplayground.ebtesting.ebdoctopdfconverter.catalog.extresp.ExtensionResponseFactory;
import app.krista.extensions.ebplayground.ebtesting.ebdoctopdfconverter.catalog.extresp.TelemetryHelper;
import app.krista.extensions.ebplayground.ebtesting.ebdoctopdfconverter.conversion.DocumentConversionException;
import app.krista.extensions.ebplayground.ebtesting.ebdoctopdfconverter.conversion.WordToPdfConverter;
import app.krista.extensions.ebplayground.ebtesting.ebdoctopdfconverter.integration.KristaMediaClient;
import app.krista.extensions.ebplayground.ebtesting.ebdoctopdfconverter.validation.DocumentValidation;
import app.krista.extensions.ebplayground.ebtesting.ebdoctopdfconverter.validation.DocumentValidation.WordFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

@Domain(id = "catEntryDomain_738dcd68-fbd8-4207-a7b2-bac849a4362b",
        name = "EB Testing",
        ecosystemId = "catEntryEcosystem_e1557d10-6587-409e-aa1c-b1bbb4c42b17",
        ecosystemName = "EB Playground",
        ecosystemVersion = "7a6784f1-14fb-44cf-bf9d-60afe56e0cbb")
public class DocumentConversionArea {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentConversionArea.class);
    private static final String PREFIX = "ebdoctopdfconverter";

    private final KristaMediaClient mediaClient;
    private final TelemetryHelper telemetry;
    private final String invokerId;

    @Inject
    public DocumentConversionArea(KristaMediaClient mediaClient, TelemetryHelper telemetry, Invoker invoker) {
        this.mediaClient = mediaClient;
        this.telemetry = telemetry;
        this.invokerId = invoker.getInvokerId();
    }

    @CatalogRequest(
            id = "localDomainRequest_7f3b8a2e-1c4d-4e6a-9b2f-3a7d5c9e1f04",
            name = "Convert Document to PDF",
            description = "Converts an uploaded Word document (.doc or .docx) to PDF.",
            area = "Document Conversion",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.File(name = "PDF File", required = false, attributes = {}, options = {})
    public ExtensionResponse convertDocumentToPdf(
            @Field.File(name = "File", required = true, attributes = {@Attribute(name = "visualWidth", value = "M")},
                    options = {}) app.krista.model.base.File file) {
        return audited("ConvertDocumentToPdf", () -> {
            DocumentValidation.requireFile(file);
            java.io.File localSource = mediaClient.toJavaFile(file);
            try {
                byte[] sourceBytes = Files.readAllBytes(localSource.toPath());
                WordFormat format = DocumentValidation.validateWordDocument(file.getFileName(), sourceBytes);
                byte[] pdfBytes = WordToPdfConverter.convert(sourceBytes, format);

                java.io.File localPdf = writeTempPdf(pdfBytes, file.getFileName());
                try {
                    app.krista.model.base.File kristaPdf = mediaClient.toKristaFile(localPdf);
                    Map<String, Object> out = new LinkedHashMap<>();
                    out.put("PDF File", kristaPdf);
                    return ExtensionResponseFactory.ok(out);
                } finally {
                    localPdf.delete();
                }
            } finally {
                localSource.delete();
            }
        });
    }

    // ---- the uniform try/catch -> classify -> ExtensionResponse + telemetry + timing wrapper ----
    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get() throws Exception;
    }

    private ExtensionResponse audited(String name, CheckedSupplier<ExtensionResponse> action) {
        long t0 = telemetry.start(PREFIX + "." + name);
        LOG.info("Catalog '{}' started for invoker {}", name, invokerId);
        try {
            ExtensionResponse r = action.get();
            LOG.info("Catalog '{}' ok in {}ms for invoker {}", name, System.currentTimeMillis() - t0, invokerId);
            telemetry.recordSuccess(PREFIX + "." + name, t0, Map.of());
            return r;
        } catch (IllegalArgumentException | DocumentConversionException e) {       // INPUT
            LOG.warn("Catalog '{}' INPUT for invoker {}: {}", name, invokerId, e.getMessage());
            telemetry.recordError(PREFIX + "." + name, t0, e, Map.of());
            return ExtensionResponseFactory.failure(e.getMessage(), ExceptionType.INPUT_ERROR);
        } catch (Exception e) {                                                    // SYSTEM
            LOG.error("Catalog '{}' SYSTEM for invoker {}: {}", name, invokerId, e.getMessage(), e);
            telemetry.recordError(PREFIX + "." + name, t0, e, Map.of());
            return ExtensionResponseFactory.failure(e.getMessage(), ExceptionType.SYSTEM_ERROR);
        }
    }

    private static java.io.File writeTempPdf(byte[] pdfBytes, String sourceFileName) throws IOException {
        String base = sourceFileName == null ? "document" : sourceFileName.replaceAll("(?i)\\.docx?$", "");
        java.io.File out = java.io.File.createTempFile(sanitizePrefix(base) + "-", ".pdf");
        try (OutputStream os = new java.io.FileOutputStream(out)) {
            os.write(pdfBytes);
        }
        return out;
    }

    private static String sanitizePrefix(String name) {
        String safe = name.replaceAll("[^A-Za-z0-9._-]", "_");
        return safe.length() >= 3 ? safe : safe + "pdf";
    }
}
